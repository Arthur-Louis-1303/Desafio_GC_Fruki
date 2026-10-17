package com.fruki.recomenda.web;

import com.fruki.recomenda.clima.ClimaService;
import com.fruki.recomenda.dto.Dtos.ResumoClima;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clima")
public class ClimaController {
    private final ClimaService clima;

    public ClimaController(ClimaService clima) { this.clima = clima; }

    /** Mapa do Brasil: resumo da semana das 27 UFs. */
    @GetMapping
    public List<ResumoClima> brasil() { return clima.brasil(); }

    @GetMapping("/{uf}")
    public ResumoClima uf(@PathVariable String uf) { return clima.uf(uf); }
}
