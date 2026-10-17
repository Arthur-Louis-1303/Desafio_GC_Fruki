package com.fruki.recomenda.repository;

import com.fruki.recomenda.domain.HierarquiaComercial;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface HierarquiaRepository extends JpaRepository<HierarquiaComercial, Long> {

    /** [codRepresentante, nome, estado] */
    @Query("select h.codRepresentante, max(h.representante), max(h.estado) from HierarquiaComercial h "
            + "where h.codRepresentante is not null group by h.codRepresentante")
    List<Object[]> listarRepresentantes();
}
