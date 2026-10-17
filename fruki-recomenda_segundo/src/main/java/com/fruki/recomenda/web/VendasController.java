package com.fruki.recomenda.web;

import com.fruki.recomenda.dto.Dtos.FamiliaStat;
import com.fruki.recomenda.dto.Dtos.ProdutoStat;
import com.fruki.recomenda.service.VendasService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vendas")
public class VendasController {
    private final VendasService vendas;

    public VendasController(VendasService vendas) { this.vendas = vendas; }

    /** Gráfico de barras por família. */
    @GetMapping("/familias")
    public List<FamiliaStat> familias(@RequestParam(required = false) String uf) { return vendas.porFamilia(uf); }

    /** Detalhe ao clicar em uma barra. */
    @GetMapping("/produtos")
    public List<ProdutoStat> produtos(@RequestParam String familia, @RequestParam(required = false) String uf) {
        return vendas.porProduto(familia, uf);
    }
}
