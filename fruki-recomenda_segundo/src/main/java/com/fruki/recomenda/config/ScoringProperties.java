package com.fruki.recomenda.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Pesos do scoring. Ficam no application.yml para ajustar sem recompilar. */
@ConfigurationProperties(prefix = "fruki.scoring")
public record ScoringProperties(
        double pesoAfinidade,
        double pontosFamiliaConhecida,
        double pontosCalor,
        double pontosFrio,
        double pontosEvento,
        double pontosAbcA,
        double pontosAbcB,
        double limiteCalor,
        double limiteFrio,
        int horizonteEventosDias,
        int topN,
        boolean bloquearCreditoSuspenso,
        List<String> familiasCalor,
        List<String> familiasFrio) {}
