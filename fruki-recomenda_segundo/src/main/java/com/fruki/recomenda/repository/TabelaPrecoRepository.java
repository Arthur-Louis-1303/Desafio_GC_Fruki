package com.fruki.recomenda.repository;

import com.fruki.recomenda.domain.TabelaPreco;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TabelaPrecoRepository extends JpaRepository<TabelaPreco, Long> {

    /** Itens que o cliente já tem no mix (via id da tabela de preço do cliente). */
    @Query("select t.idItem from TabelaPreco t where t.idTabelaPreco = :idTabela")
    List<Long> itensDaTabela(@Param("idTabela") Long idTabela);

    /**
     * Perfil de mercado: para cada item, quantos clientes ATIVOS do segmento o têm no mix.
     * Retorna [idItem, qtdClientes, precoMedio].
     */
    @Query("select tp.idItem, count(distinct c.idCliente), avg(tp.precoVenda) "
            + "from Cliente c join TabelaPreco tp on tp.idTabelaPreco = c.idTabelaPreco "
            + "where c.codigoSegmento = :seg and c.situacaoCliente = 'Ativo' group by tp.idItem")
    List<Object[]> estatisticaPorSegmento(@Param("seg") Long segmento);

    /** Presença por família: [familia, clientes, itensDistintos, precoMedio]. uf = "%" para todos. */
    @Query("select i.codDescFamilia, count(distinct c.idCliente), count(distinct i.idItem), avg(tp.precoVenda) "
            + "from Cliente c join TabelaPreco tp on tp.idTabelaPreco = c.idTabelaPreco "
            + "join Item i on i.idItem = tp.idItem "
            + "where c.estado like :uf group by i.codDescFamilia")
    List<Object[]> presencaPorFamilia(@Param("uf") String uf);

    /** Presença por produto dentro de uma família: [idItem, nome, clientes, precoMedio]. */
    @Query("select i.idItem, i.nome, count(distinct c.idCliente), avg(tp.precoVenda) "
            + "from Cliente c join TabelaPreco tp on tp.idTabelaPreco = c.idTabelaPreco "
            + "join Item i on i.idItem = tp.idItem "
            + "where c.estado like :uf and i.codDescFamilia = :familia group by i.idItem, i.nome")
    List<Object[]> presencaPorProduto(@Param("familia") String familia, @Param("uf") String uf);

    @Query("select count(t) from TabelaPreco t where not exists (select 1 from Item i where i.idItem = t.idItem)")
    long contaSemItem();
}
