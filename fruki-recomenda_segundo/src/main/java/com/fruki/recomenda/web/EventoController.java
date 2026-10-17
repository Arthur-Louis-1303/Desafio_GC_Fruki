package com.fruki.recomenda.web;

import com.fruki.recomenda.dto.Dtos.EventoDto;
import com.fruki.recomenda.eventos.EventoService;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {
    private final EventoService eventos;

    public EventoController(EventoService eventos) { this.eventos = eventos; }

    @GetMapping
    public List<EventoDto> listar(@RequestParam(required = false) String uf,
                                  @RequestParam(defaultValue = "270") int dias) {
        LocalDate hoje = LocalDate.now();
        return eventos.proximos(hoje, uf == null ? null : uf.toUpperCase(), dias).stream()
                .map(e -> new EventoDto(e.nome(), e.data(), ChronoUnit.DAYS.between(hoje, e.data()),
                        e.ufs().isEmpty() ? "Nacional" : String.join(", ", e.ufs())))
                .toList();
    }
}
