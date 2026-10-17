# Fruki Recomenda (back-end)

Spring Boot 3 / Java 17. Importa a planilha `DESAFIO_GC_CLAUDE.xlsx` para um banco H2 em memória
e expõe a API usada pelo painel do vendedor.

## Rodar
    mvn spring-boot:run          # http://localhost:8080
    mvn test

Para usar a base completa: troque `fruki.import.arquivo` no application.yml (ou aponte para `file:/caminho/base.xlsx`).

## Endpoints
| Quadrante            | Endpoint                                          |
|----------------------|---------------------------------------------------|
| Clima (mapa)         | GET /api/clima  ·  GET /api/clima/{uf}            |
| Vendas por família   | GET /api/vendas/familias?uf=RS                    |
| Produtos da família  | GET /api/vendas/produtos?familia=...&uf=RS        |
| Eventos              | GET /api/eventos?uf=RS&dias=270                   |
| Vendedores           | GET /api/vendedores                               |
| Clientes + sugestões | GET /api/vendedores/{cod}/clientes?top=3          |
| Recomendação cliente | GET /api/clientes/{id}/recomendacoes?top=10       |
| Qualidade dos joins  | GET /api/admin/integridade                        |

## Como as abas se conectam
    dim_cliente.ID_HIERARQUIA   -> dim_hierarquia_comercial.ID_HIERARQUIA  (vendedor = COD_REPRESENTANTE)
    dim_cliente.ID_TABELA_PRECO -> dim_tabela_preco.ID_TABELA_PRECO         (itens que o cliente compra)
    dim_tabela_preco.ID_ITEM    -> dim_item.ID_ITEM                         (família, ABC, nome)

Ajuste os pesos do scoring em `fruki.scoring.*` no application.yml.

## Front-end
O painel está em `src/main/resources/static/index.html` e é servido pelo próprio Spring Boot em
http://localhost:8080/ quando a aplicação está rodando. Hoje ele usa dados simulados (clima, vendas,
clientes e eventos fixos no JavaScript). Para ligá-lo à API, troque cada conjunto de dados simulado
por um `fetch` ao endpoint da tabela acima.
