package com.fruki.recomenda.domain;

import jakarta.persistence.*;

/**
 * Aba dim_tabela_preco. Cada linha = um item na tabela de preço de um cliente.
 * Vários registros compartilham o mesmo idTabelaPreco, por isso a chave é um surrogate.
 */
@Entity
@Table(name = "dim_tabela_preco", indexes = {
        @Index(columnList = "id_tabela_preco"),
        @Index(columnList = "id_item")})
public class TabelaPreco {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "id_tabela_preco")
    public Long idTabelaPreco;
    @Column(name = "id_item")
    public Long idItem;
    public Double precoVenda;
    public Double precoMinimo;
    public String tabelaAtiva;
}
