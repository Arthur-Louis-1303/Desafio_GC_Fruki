package com.fruki.recomenda.dto;

import java.time.LocalDate;
import java.util.List;

/** Objetos devolvidos pela API (um por quadrante do painel). */
public final class Dtos {
    private Dtos() {}

    // Quadrante 1: clima
    public record PrevisaoDia(LocalDate data, double tempMax, int chuvaPct) {}
    public record ResumoClima(String uf, double mediaMaxima, int chuvaMediaPct, String fonte, List<PrevisaoDia> dias) {}

    // Quadrante 2: estatísticas por família (presença no mix dos clientes)
    public record FamiliaStat(String familia, long clientes, double percentualClientes, long itens, double precoMedio) {}
    public record ProdutoStat(Long idItem, String nome, long clientes, double percentualClientes, double precoMedio) {}

    // Quadrante 3: eventos
    public record EventoDto(String nome, LocalDate data, long diasRestantes, String abrangencia) {}

    // Quadrante 4: clientes e recomendações
    public record Recomendacao(Long idItem, String nome, String familia, String classeAbc, int score,
                               Double precoReferencia, List<String> motivos) {}
    public record RecomendacaoCliente(Long idCliente, Long codigoCliente, String cidade, String estado,
                                      Long segmento, boolean elegivel, List<String> alertas, int itensNoMix,
                                      double ticketPotencial, List<Recomendacao> recomendacoes) {}
    public record Vendedor(Long codigo, String nome, String estado, long clientes) {}

    // Administração
    public record Integridade(long clientes, long itens, long linhasTabelaPreco, long hierarquias,
                              long clientesSemTabelaPreco, long linhasTabelaSemItem, long clientesSemHierarquia) {}
}
