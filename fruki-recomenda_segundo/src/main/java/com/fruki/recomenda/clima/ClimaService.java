package com.fruki.recomenda.clima;

import com.fasterxml.jackson.databind.JsonNode;
import com.fruki.recomenda.dto.Dtos.PrevisaoDia;
import com.fruki.recomenda.dto.Dtos.ResumoClima;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

/**
 * Previsão dos próximos 7 dias por UF (usa a capital como referência) via Open-Meteo (gratuita, sem chave).
 * Uma única chamada em lote traz as 27 UFs; o resultado fica em cache por 3 horas.
 * Se a API falhar, cai para uma previsão simulada e marca fonte="simulado".
 */
@Service
public class ClimaService {

    private static final Logger log = LoggerFactory.getLogger(ClimaService.class);
    private static final Duration VALIDADE = Duration.ofHours(3);

    /** UF -> {latitude, longitude} da capital. */
    static final Map<String, double[]> COORDS = new LinkedHashMap<>();
    static {
        COORDS.put("AC", new double[]{-9.97, -67.81});  COORDS.put("AL", new double[]{-9.67, -35.74});
        COORDS.put("AP", new double[]{0.03, -51.07});   COORDS.put("AM", new double[]{-3.12, -60.02});
        COORDS.put("BA", new double[]{-12.97, -38.50}); COORDS.put("CE", new double[]{-3.73, -38.54});
        COORDS.put("DF", new double[]{-15.78, -47.93}); COORDS.put("ES", new double[]{-20.32, -40.34});
        COORDS.put("GO", new double[]{-16.68, -49.25}); COORDS.put("MA", new double[]{-2.53, -44.30});
        COORDS.put("MT", new double[]{-15.60, -56.10}); COORDS.put("MS", new double[]{-20.47, -54.62});
        COORDS.put("MG", new double[]{-19.92, -43.94}); COORDS.put("PA", new double[]{-1.46, -48.50});
        COORDS.put("PB", new double[]{-7.12, -34.86});  COORDS.put("PR", new double[]{-25.43, -49.27});
        COORDS.put("PE", new double[]{-8.05, -34.88});  COORDS.put("PI", new double[]{-5.09, -42.80});
        COORDS.put("RJ", new double[]{-22.91, -43.17}); COORDS.put("RN", new double[]{-5.79, -35.21});
        COORDS.put("RS", new double[]{-30.03, -51.23}); COORDS.put("RO", new double[]{-8.76, -63.90});
        COORDS.put("RR", new double[]{2.82, -60.67});   COORDS.put("SC", new double[]{-27.59, -48.55});
        COORDS.put("SP", new double[]{-23.55, -46.63}); COORDS.put("SE", new double[]{-10.91, -37.07});
        COORDS.put("TO", new double[]{-10.18, -48.33});
    }

    private static final Set<String> NORTE = Set.of("AC", "AP", "AM", "PA", "RO", "RR", "TO");
    private static final Set<String> SUL = Set.of("PR", "SC", "RS");
    private static final Set<String> SUDESTE = Set.of("MG", "SP", "RJ", "ES");

    private record Snapshot(Instant em, Map<String, List<PrevisaoDia>> dados, String fonte) {}

    private final RestClient http = RestClient.create();
    private volatile Snapshot snapshot;

    public List<ResumoClima> brasil() {
        Snapshot s = obter();
        return COORDS.keySet().stream().map(uf -> resumo(uf, s)).toList();
    }

    public ResumoClima uf(String uf) {
        String u = uf == null ? "" : uf.toUpperCase();
        if (!COORDS.containsKey(u)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "UF inválida: " + uf);
        return resumo(u, obter());
    }

    /** Média das temperaturas máximas da semana; NaN se a UF for desconhecida. */
    public double mediaMaxima(String uf) {
        if (uf == null || !COORDS.containsKey(uf.toUpperCase())) return Double.NaN;
        return uf(uf).mediaMaxima();
    }

    private ResumoClima resumo(String uf, Snapshot s) {
        List<PrevisaoDia> dias = s.dados().get(uf);
        double media = dias.stream().mapToDouble(PrevisaoDia::tempMax).average().orElse(Double.NaN);
        int chuva = (int) Math.round(dias.stream().mapToInt(PrevisaoDia::chuvaPct).average().orElse(0));
        return new ResumoClima(uf, Math.round(media * 10) / 10.0, chuva, s.fonte(), dias);
    }

    private synchronized Snapshot obter() {
        Snapshot s = snapshot;
        if (s != null && Duration.between(s.em(), Instant.now()).compareTo(VALIDADE) < 0) return s;
        try {
            snapshot = buscar();
        } catch (Exception e) {
            log.warn("Falha ao consultar Open-Meteo ({}). Usando previsão simulada.", e.getMessage());
            if (snapshot == null) snapshot = simulado();
        }
        return snapshot;
    }

    private Snapshot buscar() {
        StringJoiner lat = new StringJoiner(","), lon = new StringJoiner(",");
        COORDS.values().forEach(c -> { lat.add(String.valueOf(c[0])); lon.add(String.valueOf(c[1])); });
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                + "&daily=temperature_2m_max,precipitation_probability_max"
                + "&timezone=America%2FSao_Paulo&forecast_days=8";
        JsonNode raiz = http.get().uri(URI.create(url)).retrieve().body(JsonNode.class);
        if (raiz == null || !raiz.isArray() || raiz.size() != COORDS.size())
            throw new IllegalStateException("resposta inesperada da API de clima");

        Map<String, List<PrevisaoDia>> dados = new LinkedHashMap<>();
        int idx = 0;
        for (String uf : COORDS.keySet()) {
            JsonNode daily = raiz.get(idx++).get("daily");
            JsonNode datas = daily.get("time"), tmax = daily.get("temperature_2m_max"),
                    chuva = daily.get("precipitation_probability_max");
            List<PrevisaoDia> dias = new ArrayList<>();
            for (int d = 1; d < datas.size(); d++) {          // d=0 é hoje; a semana começa amanhã
                dias.add(new PrevisaoDia(LocalDate.parse(datas.get(d).asText()),
                        tmax.get(d).asDouble(), chuva.get(d).asInt(0)));
            }
            dados.put(uf, dias);
        }
        return new Snapshot(Instant.now(), dados, "open-meteo");
    }

    /** Previsão determinística (mesma entrada, mesma saída) para demo offline. */
    private Snapshot simulado() {
        Map<String, List<PrevisaoDia>> dados = new LinkedHashMap<>();
        LocalDate amanha = LocalDate.now().plusDays(1);
        for (String uf : COORDS.keySet()) {
            double base = NORTE.contains(uf) ? 32 : SUL.contains(uf) ? 23 : SUDESTE.contains(uf) ? 28 : 30;
            int h = uf.chars().sum();
            List<PrevisaoDia> dias = new ArrayList<>();
            for (int d = 0; d < 7; d++) {
                double t = Math.round((base + 3 * Math.sin((d + h) * 0.9)) * 10) / 10.0;
                dias.add(new PrevisaoDia(amanha.plusDays(d), t, (h * 7 + d * 13) % 55));
            }
            dados.put(uf, dias);
        }
        return new Snapshot(Instant.now(), dados, "simulado");
    }
}
