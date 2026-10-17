package com.fruki.recomenda.eventos;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/** Calendário de datas comemorativas. As datas móveis (Páscoa, Carnaval...) são calculadas por ano. */
@Service
public class EventoService {

    private static final List<String> REFRI = List.of("REFRI");
    private static final List<String> FESTA = List.of("ENERG", "REFRI", "AGUA", "ISOT");
    private static final List<String> LEVE = List.of("CHA", "SUCO", "AGUA");

    /** Eventos dos próximos {@code dias} dias a partir de {@code hoje}, filtrados por UF (null = todas). */
    public List<Evento> proximos(LocalDate hoje, String uf, int dias) {
        LocalDate limite = hoje.plusDays(dias);
        List<Evento> todos = new ArrayList<>();
        todos.addAll(doAno(hoje.getYear()));
        todos.addAll(doAno(hoje.getYear() + 1));
        return todos.stream()
                .filter(e -> !e.data().isBefore(hoje) && !e.data().isAfter(limite))
                .filter(e -> e.vale(uf))
                .sorted(Comparator.comparing(Evento::data))
                .toList();
    }

    List<Evento> doAno(int ano) {
        LocalDate pascoa = pascoa(ano);
        LocalDate diaDasMaes = LocalDate.of(ano, 5, 1).with(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.SUNDAY));
        LocalDate diaDosPais = LocalDate.of(ano, 8, 1).with(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.SUNDAY));
        LocalDate blackFriday = LocalDate.of(ano, 11, 1)
                .with(TemporalAdjusters.dayOfWeekInMonth(4, DayOfWeek.THURSDAY)).plusDays(1);
        return List.of(
                new Evento("Ano Novo", LocalDate.of(ano, 1, 1), List.of(), FESTA),
                new Evento("Festa de Iemanjá", LocalDate.of(ano, 2, 2), List.of("RS", "BA", "SP", "RJ"), List.of("AGUA")),
                new Evento("Carnaval", pascoa.minusDays(47), List.of(), FESTA),
                new Evento("Páscoa", pascoa, List.of(), LEVE),
                new Evento("Dia das Mães", diaDasMaes, List.of(), List.of("REFRI", "CHA")),
                new Evento("Dia dos Pais", diaDosPais, List.of(), List.of("REFRI", "ENERG")),
                new Evento("Semana Farroupilha", LocalDate.of(ano, 9, 20), List.of("RS"), REFRI),
                new Evento("Dia das Crianças", LocalDate.of(ano, 10, 12), List.of(), REFRI),
                new Evento("Finados", LocalDate.of(ano, 11, 2), List.of(), List.of()),
                new Evento("Black Friday", blackFriday, List.of(), REFRI),
                new Evento("Natal", LocalDate.of(ano, 12, 25), List.of(), List.of("REFRI", "SUCO")),
                new Evento("Réveillon", LocalDate.of(ano, 12, 31), List.of(), FESTA));
    }

    /** Algoritmo de Meeus/Jones/Butcher (calendário gregoriano). */
    static LocalDate pascoa(int ano) {
        int a = ano % 19, b = ano / 100, c = ano % 100;
        int d = b / 4, e = b % 4, f = (b + 8) / 25, g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4, k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int mes = (h + l - 7 * m + 114) / 31;
        int dia = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(ano, mes, dia);
    }
}
