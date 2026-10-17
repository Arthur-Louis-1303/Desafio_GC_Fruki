package com.fruki.recomenda;

import static org.junit.jupiter.api.Assertions.*;

import com.fruki.recomenda.eventos.Evento;
import com.fruki.recomenda.eventos.EventoService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventoServiceTest {

    private final EventoService service = new EventoService();

    @Test
    void calculaDatasMoveis() {
        List<Evento> e2027 = service.proximos(LocalDate.of(2027, 1, 1), null, 365);
        assertEquals(LocalDate.of(2027, 3, 28), data(e2027, "Páscoa"));
        assertEquals(LocalDate.of(2027, 2, 9), data(e2027, "Carnaval"));
        assertEquals(LocalDate.of(2027, 5, 9), data(e2027, "Dia das Mães"));
        assertEquals(LocalDate.of(2026, 11, 27),
                data(service.proximos(LocalDate.of(2026, 10, 14), null, 60), "Black Friday"));
    }

    @Test
    void eventoRegionalSoApareceNaUfCorreta() {
        LocalDate hoje = LocalDate.of(2026, 9, 1);
        assertTrue(service.proximos(hoje, "RS", 30).stream().anyMatch(e -> e.nome().equals("Semana Farroupilha")));
        assertTrue(service.proximos(hoje, "AM", 30).stream().noneMatch(e -> e.nome().equals("Semana Farroupilha")));
    }

    private static LocalDate data(List<Evento> lista, String nome) {
        return lista.stream().filter(e -> e.nome().equals(nome)).findFirst().orElseThrow().data();
    }
}
