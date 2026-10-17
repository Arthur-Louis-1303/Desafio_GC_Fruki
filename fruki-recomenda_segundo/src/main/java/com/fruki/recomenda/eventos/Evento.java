package com.fruki.recomenda.eventos;

import java.time.LocalDate;
import java.util.List;

/**
 * @param ufs      UFs onde o evento vale (vazio = nacional)
 * @param familias palavras que identificam as famílias de produto impulsionadas pelo evento
 */
public record Evento(String nome, LocalDate data, List<String> ufs, List<String> familias) {
    public boolean vale(String uf) {
        return ufs.isEmpty() || uf == null || ufs.contains(uf);
    }
}
