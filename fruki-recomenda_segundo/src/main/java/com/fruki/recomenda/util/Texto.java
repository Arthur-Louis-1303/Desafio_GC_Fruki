package com.fruki.recomenda.util;

import java.text.Normalizer;
import java.util.Collection;
import java.util.Locale;

public final class Texto {
    private Texto() {}

    /** Remove acentos e coloca em maiúsculas: "Água" -> "AGUA". */
    public static String normaliza(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toUpperCase(Locale.ROOT);
    }

    /** true se o texto contém qualquer uma das palavras (ignorando acento e caixa). */
    public static boolean contemAlgum(String texto, Collection<String> palavras) {
        if (texto == null || palavras == null) return false;
        String t = normaliza(texto);
        return palavras.stream().anyMatch(p -> t.contains(normaliza(p)));
    }
}
