package com.fruki.recomenda.service;

import com.fruki.recomenda.dto.Dtos.FamiliaStat;
import com.fruki.recomenda.dto.Dtos.ProdutoStat;
import com.fruki.recomenda.repository.ClienteRepository;
import com.fruki.recomenda.repository.TabelaPrecoRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quadrante 2. Como a base não traz volume vendido, a métrica é a PRESENÇA no mix:
 * % dos clientes que têm itens da família (ou o produto) na tabela de preço.
 * Quando houver uma tabela de pedidos, troque as queries por soma de quantidade/valor.
 */
@Service
@Transactional(readOnly = true)
public class VendasService {

    private final TabelaPrecoRepository tabelas;
    private final ClienteRepository clientes;

    public VendasService(TabelaPrecoRepository tabelas, ClienteRepository clientes) {
        this.tabelas = tabelas;
        this.clientes = clientes;
    }

    public List<FamiliaStat> porFamilia(String uf) {
        String filtro = filtro(uf);
        double total = Math.max(clientes.countByEstadoLike(filtro), 1);
        return tabelas.presencaPorFamilia(filtro).stream()
                .map(r -> new FamiliaStat(
                        r[0] == null ? "Sem família" : (String) r[0],
                        ((Number) r[1]).longValue(),
                        pct(((Number) r[1]).longValue(), total),
                        ((Number) r[2]).longValue(),
                        r[3] == null ? 0 : ((Number) r[3]).doubleValue()))
                .sorted(Comparator.comparingLong(FamiliaStat::clientes).reversed())
                .toList();
    }

    public List<ProdutoStat> porProduto(String familia, String uf) {
        String filtro = filtro(uf);
        double total = Math.max(clientes.countByEstadoLike(filtro), 1);
        return tabelas.presencaPorProduto(familia, filtro).stream()
                .map(r -> new ProdutoStat(
                        ((Number) r[0]).longValue(),
                        r[1] == null ? "Item " + r[0] : (String) r[1],
                        ((Number) r[2]).longValue(),
                        pct(((Number) r[2]).longValue(), total),
                        r[3] == null ? 0 : ((Number) r[3]).doubleValue()))
                .sorted(Comparator.comparingLong(ProdutoStat::clientes).reversed())
                .toList();
    }

    private static String filtro(String uf) {
        return (uf == null || uf.isBlank()) ? "%" : uf.toUpperCase();
    }

    private static double pct(long parte, double total) {
        return Math.round(parte / total * 1000) / 10.0;
    }
}
