package com.fruki.recomenda.service;

import com.fruki.recomenda.domain.Cliente;
import com.fruki.recomenda.dto.Dtos.Integridade;
import com.fruki.recomenda.dto.Dtos.RecomendacaoCliente;
import com.fruki.recomenda.dto.Dtos.Vendedor;
import com.fruki.recomenda.repository.*;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Quadrante 4: carteira do vendedor com as sugestões de cada cliente. */
@Service
@Transactional(readOnly = true)
public class VendedorService {

    private final HierarquiaRepository hierarquias;
    private final ClienteRepository clientes;
    private final ItemRepository itens;
    private final TabelaPrecoRepository tabelas;
    private final RecomendacaoService recomendacao;

    public VendedorService(HierarquiaRepository hierarquias, ClienteRepository clientes, ItemRepository itens,
                           TabelaPrecoRepository tabelas, RecomendacaoService recomendacao) {
        this.hierarquias = hierarquias;
        this.clientes = clientes;
        this.itens = itens;
        this.tabelas = tabelas;
        this.recomendacao = recomendacao;
    }

    public List<Vendedor> listar() {
        return hierarquias.listarRepresentantes().stream()
                .map(r -> {
                    Long cod = ((Number) r[0]).longValue();
                    return new Vendedor(cod, (String) r[1], (String) r[2], clientes.findByRepresentante(cod).size());
                })
                .toList();
    }

    public List<RecomendacaoCliente> carteira(Long codRepresentante, Integer top) {
        List<Cliente> lista = clientes.findByRepresentante(codRepresentante);
        return recomendacao.recomendarLote(lista, top == null ? 3 : top);
    }

    /** Mostra o quanto os IDs das abas se conectam (útil para validar o recorte de dados). */
    public Integridade integridade() {
        return new Integridade(clientes.count(), itens.count(), tabelas.count(), hierarquias.count(),
                clientes.contaSemTabelaPreco(), tabelas.contaSemItem(), clientes.contaSemHierarquia());
    }
}
