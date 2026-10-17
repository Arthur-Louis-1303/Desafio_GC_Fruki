package com.fruki.recomenda.domain;

import jakarta.persistence.*;

/** Aba dim_hierarquia_comercial: liga o cliente (via idHierarquia) ao vendedor responsável. */
@Entity
@Table(name = "dim_hierarquia_comercial", indexes = {
        @Index(columnList = "id_hierarquia"),
        @Index(columnList = "cod_representante")})
public class HierarquiaComercial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "id_hierarquia")
    public Long idHierarquia;
    public Long versao;
    @Column(name = "cod_representante")
    public Long codRepresentante;
    public Long codSupervisor;
    public Long codGerente;
    public String estado;
    public String cidade;
    public String representante;
    public String supervisor;
}
