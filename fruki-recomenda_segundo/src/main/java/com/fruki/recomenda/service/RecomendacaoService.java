package com.fruki.recomenda.service;

import com.fruki.recomenda.clima.ClimaService;
import com.fruki.recomenda.config.ScoringProperties;
import com.fruki.recomenda.domain.Cliente;
import com.fruki.recomenda.domain.Item;
import com.fruki.recomenda.dto.Dtos.Recomendacao;
import com.fruki.recomenda.dto.Dtos.RecomendacaoCliente;
import com.fruki.recomenda.eventos.Evento;
import com.fruki.recomenda.eventos.EventoService;
import com.fruki.recomenda.repository.ClienteRepository;
import com.fruki.recomenda.repository.ItemRepository;
import com.fruki.recomenda.repository.TabelaPrecoRepository;
import com.fruki.recomenda.util.Texto;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Motor de regras e pontuação (0 a 100) para cada item do portfólio que o cliente ainda NÃO compra.
 *
 *  1. Perfil de mercado  : % dos clientes ativos do mesmo segmento que têm o item no mix     (até pesoAfinidade)
 *  2. Histórico          : o item é da mesma família de itens que o cliente já compra       (+pontosFamiliaConhecida)
 *  3. Clima              : calor previsto + família de bebida gelada / frio + chá, energético (+pontosCalor/Frio)
 *  4. Eventos            : feriado/data próxima que impulsiona a família do item           (+pontosEvento, decai com os dias)
 *  5. Relevância         : classificação ABC do item                                         (+10 A, +5 B)
 */
@Service
public class RecomendacaoService {

    private record PeerStat(long clientes, double precoMedio) {}

    private final ClienteRepository clientes;
    private final ItemRepository itens;
    private final TabelaPrecoRepository tabelas;
    private final ClimaService clima;
    private final EventoService eventos;
    private final ScoringProperties cfg;

    public RecomendacaoService(ClienteRepository clientes, ItemRepository itens, TabelaPrecoRepository tabelas,
                               ClimaService clima, EventoService eventos, ScoringProperties cfg) {
        this.clientes = clientes;
        this.itens = itens;
        this.tabelas = tabelas;
        this.clima = clima;
        this.eventos = eventos;
        this.cfg = cfg;
    }

    @Transactional(readOnly = true)
    public RecomendacaoCliente recomendar(Long idCliente, Integer top) {
        Cliente c = clientes.findById(idCliente).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado: " + idCliente));
        return recomendarLote(List.of(c), top).get(0);
    }

    /** Processa vários clientes reaproveitando catálogo e estatísticas de segmento (evita N consultas repetidas). */
    @Transactional(readOnly = true)
    public List<RecomendacaoCliente> recomendarLote(List<Cliente> lista, Integer top) {
        int n = (top == null || top <= 0) ? cfg.topN() : top;
        Map<Long, Item> catalogo = itens.findAll().stream().collect(Collectors.toMap(i -> i.idItem, i -> i));
        Map<Long, Map<Long, PeerStat>> cacheSegmento = new HashMap<>();
        Map<Long, Long> cacheTotalSegmento = new HashMap<>();
        LocalDate hoje = LocalDate.now();

        List<RecomendacaoCliente> saida = new ArrayList<>();
        for (Cliente c : lista) {
            Map<Long, PeerStat> pares = Map.of();
            long totalAtivos = 0;
            if (c.codigoSegmento != null) {
                pares = cacheSegmento.computeIfAbsent(c.codigoSegmento, this::carregaSegmento);
                totalAtivos = cacheTotalSegmento.computeIfAbsent(c.codigoSegmento, clientes::contaAtivosPorSegmento);
            }
            saida.add(montar(c, catalogo, pares, totalAtivos, hoje, n));
        }
        return saida;
    }

    private Map<Long, PeerStat> carregaSegmento(Long segmento) {
        Map<Long, PeerStat> mapa = new HashMap<>();
        for (Object[] r : tabelas.estatisticaPorSegmento(segmento)) {
            double preco = r[2] == null ? 0 : ((Number) r[2]).doubleValue();
            mapa.put(((Number) r[0]).longValue(), new PeerStat(((Number) r[1]).longValue(), preco));
        }
        return mapa;
    }

    private RecomendacaoCliente montar(Cliente c, Map<Long, Item> catalogo, Map<Long, PeerStat> pares,
                                       long totalAtivosSegmento, LocalDate hoje, int n) {
        Set<Long> mix = c.idTabelaPreco == null ? Set.of() : new HashSet<>(tabelas.itensDaTabela(c.idTabelaPreco));
        List<String> alertas = new ArrayList<>();
        boolean ativo = "Ativo".equalsIgnoreCase(c.situacaoCliente);
        boolean suspenso = c.situacaoCredito != null && c.situacaoCredito.equalsIgnoreCase("Suspenso");
        if (!ativo) alertas.add("Cliente inativo");
        if (suspenso) alertas.add("Crédito suspenso");
        boolean elegivel = ativo && !(suspenso && cfg.bloquearCreditoSuspenso());
        if (mix.isEmpty()) alertas.add("Cliente sem itens na tabela de preço");

        if (!elegivel) {
            return new RecomendacaoCliente(c.idCliente, c.codigoCliente, c.cidade, c.estado, c.codigoSegmento,
                    false, alertas, mix.size(), 0, List.of());
        }

        // Os pares excluem o próprio cliente (ele só conta nos itens que já tem, que são descartados abaixo).
        long totalPares = Math.max(totalAtivosSegmento - 1, 0);
        Set<String> familiasMix = mix.stream().map(catalogo::get).filter(Objects::nonNull)
                .map(i -> i.codDescFamilia).filter(Objects::nonNull).collect(Collectors.toSet());
        double temp = clima.mediaMaxima(c.estado);
        List<Evento> evs = eventos.proximos(hoje, c.estado, cfg.horizonteEventosDias());

        List<Recomendacao> recs = new ArrayList<>();
        for (Item it : catalogo.values()) {
            if (mix.contains(it.idItem) || obsoleto(it)) continue;

            double s = 0;
            List<String> motivos = new ArrayList<>();
            String familia = it.codDescFamilia;

            PeerStat ps = pares.get(it.idItem);
            if (ps != null && totalPares > 0) {
                double afinidade = Math.min(1.0, (double) ps.clientes() / totalPares);
                if (afinidade > 0) {
                    s += cfg.pesoAfinidade() * afinidade;
                    motivos.add(Math.round(afinidade * 100) + "% dos clientes com o mesmo perfil compram");
                }
            }
            if (familia != null && familiasMix.contains(familia)) {
                s += cfg.pontosFamiliaConhecida();
                motivos.add("complementa a linha que o cliente já compra");
            }
            if (!Double.isNaN(temp)) {
                if (temp >= cfg.limiteCalor() && Texto.contemAlgum(familia, cfg.familiasCalor())) {
                    s += cfg.pontosCalor();
                    motivos.add("calor previsto na semana (" + Math.round(temp) + "°C)");
                } else if (temp <= cfg.limiteFrio() && Texto.contemAlgum(familia, cfg.familiasFrio())) {
                    s += cfg.pontosFrio();
                    motivos.add("frio previsto na semana (" + Math.round(temp) + "°C)");
                }
            }
            for (Evento e : evs) {
                if (Texto.contemAlgum(familia, e.familias())) {
                    long dias = ChronoUnit.DAYS.between(hoje, e.data());
                    s += cfg.pontosEvento() * (1 - 0.5 * dias / cfg.horizonteEventosDias());
                    motivos.add(e.nome() + " em " + dias + " dias");
                    break;                                   // só o evento mais próximo pontua
                }
            }
            if ("A".equalsIgnoreCase(it.classificacaoAbc)) { s += cfg.pontosAbcA(); motivos.add("item classe A"); }
            else if ("B".equalsIgnoreCase(it.classificacaoAbc)) { s += cfg.pontosAbcB(); }

            if (s <= 0) continue;
            int score = (int) Math.min(100, Math.round(s));
            String nome = it.nome != null ? it.nome : "Item " + it.codItem;
            recs.add(new Recomendacao(it.idItem, nome, familia, it.classificacaoAbc, score,
                    ps == null ? null : ps.precoMedio(), motivos));
        }

        recs.sort(Comparator.comparingInt(Recomendacao::score).reversed().thenComparing(Recomendacao::idItem));
        List<Recomendacao> topN = recs.stream().limit(n).toList();
        double ticket = topN.stream().filter(r -> r.precoReferencia() != null)
                .mapToDouble(Recomendacao::precoReferencia).sum();
        return new RecomendacaoCliente(c.idCliente, c.codigoCliente, c.cidade, c.estado, c.codigoSegmento,
                true, alertas, mix.size(), Math.round(ticket * 100) / 100.0, topN);
    }

    private static boolean obsoleto(Item i) {
        String o = Texto.normaliza(i.obsoleto);
        return o.equals("S") || o.equals("SIM") || o.equals("OBSOLETO") || o.equals("1") || o.equals("TRUE");
    }
}
