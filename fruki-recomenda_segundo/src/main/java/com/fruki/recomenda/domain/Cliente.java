package com.fruki.recomenda.domain;

import jakarta.persistence.*;

/** Aba dim_cliente. Sem FK rígida: os IDs do recorte de amostra não fecham entre as abas. */
@Entity
@Table(name = "dim_cliente", indexes = {
        @Index(columnList = "id_tabela_preco"),
        @Index(columnList = "id_hierarquia"),
        @Index(columnList = "codigo_segmento")})
public class Cliente {
    @Id
    public Long idCliente;
    public Long codigoCliente;
    @Column(name = "id_hierarquia")
    public Long idHierarquia;
    @Column(name = "codigo_segmento")
    public Long codigoSegmento;      // perfil de mercado
    public Long codigoSubsegmento;
    @Column(name = "id_tabela_preco")
    public Long idTabelaPreco;       // liga o cliente aos itens que ele compra
    public String estado;
    public String cidade;
    public String bairro;
    public String situacaoCliente;   // Ativo / Inativo
    public String situacaoCredito;   // ex.: Suspenso
}
