package com.fruki.recomenda.domain;

import jakarta.persistence.*;

/** Aba dim_item (portfólio). */
@Entity
@Table(name = "dim_item")
public class Item {
    @Id
    public Long idItem;
    public Long codItem;
    public String nome;              // coluna ITEM
    public String codDescFamilia;    // ex.: "0114 - ENERGETICO LATA" (a coluna FAMILIA da amostra está anonimizada)
    public String marca;
    public String sabor;
    public String classificacaoAbc;  // A / B / C
    public String obsoleto;
}
