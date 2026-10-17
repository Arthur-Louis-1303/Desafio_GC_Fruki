package com.fruki.recomenda.repository;

import com.fruki.recomenda.domain.Cliente;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    /** Clientes de um vendedor: cliente.idHierarquia -> hierarquia.idHierarquia -> codRepresentante. */
    @Query("select c from Cliente c where c.idHierarquia in "
            + "(select h.idHierarquia from HierarquiaComercial h where h.codRepresentante = :cod)")
    List<Cliente> findByRepresentante(@Param("cod") Long cod);

    @Query("select count(c) from Cliente c where c.codigoSegmento = :seg and c.situacaoCliente = 'Ativo'")
    long contaAtivosPorSegmento(@Param("seg") Long segmento);

    long countByEstadoLike(String estado);

    @Query("select count(c) from Cliente c where not exists "
            + "(select 1 from TabelaPreco t where t.idTabelaPreco = c.idTabelaPreco)")
    long contaSemTabelaPreco();

    @Query("select count(c) from Cliente c where not exists "
            + "(select 1 from HierarquiaComercial h where h.idHierarquia = c.idHierarquia)")
    long contaSemHierarquia();
}
