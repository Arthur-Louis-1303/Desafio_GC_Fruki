package com.fruki.recomenda.web;

import com.fruki.recomenda.dto.Dtos.Integridade;
import com.fruki.recomenda.dto.Dtos.RecomendacaoCliente;
import com.fruki.recomenda.dto.Dtos.Vendedor;
import com.fruki.recomenda.service.RecomendacaoService;
import com.fruki.recomenda.service.VendedorService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class VendedorController {
    private final VendedorService vendedores;
    private final RecomendacaoService recomendacao;

    public VendedorController(VendedorService vendedores, RecomendacaoService recomendacao) {
        this.vendedores = vendedores;
        this.recomendacao = recomendacao;
    }

    @GetMapping("/vendedores")
    public List<Vendedor> vendedores() { return vendedores.listar(); }

    /** Lista de clientes do vendedor, já com os produtos sugeridos (top 3 por padrão). */
    @GetMapping("/vendedores/{codigo}/clientes")
    public List<RecomendacaoCliente> carteira(@PathVariable Long codigo, @RequestParam(required = false) Integer top) {
        return vendedores.carteira(codigo, top);
    }

    /** Recomendação completa de um cliente (usada também pelo Pede Fruki no WhatsApp). */
    @GetMapping("/clientes/{id}/recomendacoes")
    public RecomendacaoCliente recomendacoes(@PathVariable Long id, @RequestParam(required = false) Integer top) {
        return recomendacao.recomendar(id, top);
    }

    @GetMapping("/admin/integridade")
    public Integridade integridade() { return vendedores.integridade(); }
}
