package com.fruki.recomenda.importacao;

import com.fruki.recomenda.domain.*;
import com.fruki.recomenda.repository.*;
import com.fruki.recomenda.util.Texto;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Lê as 4 abas do Excel e grava no banco ao subir a aplicação.
 * As colunas são encontradas pelo NOME do cabeçalho (normalizado), não pela posição,
 * então reordenar colunas na planilha não quebra a importação.
 */
@Component
public class PlanilhaImporter implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlanilhaImporter.class);
    private static final DataFormatter FMT = new DataFormatter(new Locale("pt", "BR"));

    private final Resource arquivo;
    private final ClienteRepository clientes;
    private final ItemRepository itens;
    private final TabelaPrecoRepository tabelas;
    private final HierarquiaRepository hierarquias;

    public PlanilhaImporter(@Value("${fruki.import.arquivo}") Resource arquivo, ClienteRepository clientes,
                            ItemRepository itens, TabelaPrecoRepository tabelas, HierarquiaRepository hierarquias) {
        this.arquivo = arquivo;
        this.clientes = clientes;
        this.itens = itens;
        this.tabelas = tabelas;
        this.hierarquias = hierarquias;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!arquivo.exists()) {
            log.warn("Planilha não encontrada ({}). Nada foi importado.", arquivo);
            return;
        }
        try (InputStream in = arquivo.getInputStream(); Workbook wb = WorkbookFactory.create(in)) {
            clientes.saveAll(ler(wb, "dim_cliente", l -> {
                Long id = l.num("ID_CLIENTE");
                if (id == null) return null;
                Cliente c = new Cliente();
                c.idCliente = id;
                c.codigoCliente = l.num("CODIGO_CLIENTE");
                c.idHierarquia = l.num("ID_HIERARQUIA");
                c.codigoSegmento = l.num("CODIGO_SEGMENTO");
                c.codigoSubsegmento = l.num("CODIGO_SUBSEGUIMENTO");
                c.idTabelaPreco = l.num("ID_TABELA_PRECO");
                c.estado = l.str("ESTADO");
                c.cidade = l.str("CIDADE");
                c.bairro = l.str("BAIRRO");
                c.situacaoCliente = l.str("SITUACAO_CLIENTE");
                c.situacaoCredito = l.str("SITUACAO_CREDITO");
                return c;
            }));

            itens.saveAll(ler(wb, "dim_item", l -> {
                Long id = l.num("ID_ITEM");
                if (id == null) return null;
                Item i = new Item();
                i.idItem = id;
                i.codItem = l.num("COD_ITEM");
                i.nome = l.str("ITEM");
                i.codDescFamilia = l.str("COD_DESC_FAMILIA");
                i.marca = l.str("MARCA");
                i.sabor = l.str("SABOR");
                String abc = l.str("CLASSIFICAO_ABC");               // grafia da planilha
                i.classificacaoAbc = abc != null ? abc : l.str("CLASSIFICACAO_ABC");
                i.obsoleto = l.str("OBSOLETO");
                return i;
            }));

            tabelas.saveAll(ler(wb, "dim_tabela_preco", l -> {
                Long idTabela = l.num("ID_TABELA_PRECO");
                Long idItem = l.num("ID_ITEM");
                if (idTabela == null || idItem == null) return null;
                TabelaPreco t = new TabelaPreco();
                t.idTabelaPreco = idTabela;
                t.idItem = idItem;
                t.precoVenda = l.dec("PRECO_VENDA");
                t.precoMinimo = l.dec("PRECO_MINIMO");
                t.tabelaAtiva = l.str("TABELA_ATIVA");
                return t;
            }));

            hierarquias.saveAll(ler(wb, "dim_hierarquia_comercial", l -> {
                Long id = l.num("ID_HIERARQUIA");
                if (id == null) return null;
                HierarquiaComercial h = new HierarquiaComercial();
                h.idHierarquia = id;
                h.versao = l.num("VERSION");
                h.codRepresentante = l.num("COD_REPRESENTANTE");
                h.codSupervisor = l.num("COD_SUPERVISOR");
                h.codGerente = l.num("COD_GERENTE");
                h.estado = l.str("ESTADO");
                h.cidade = l.str("CIDADE");
                h.representante = l.str("REPRESENTANTE");
                h.supervisor = l.str("SUPERVISOR");
                return h;
            }));
        }
        log.info("Importação concluída: {} clientes, {} itens, {} linhas de preço, {} hierarquias.",
                clientes.count(), itens.count(), tabelas.count(), hierarquias.count());
    }

    private <T> List<T> ler(Workbook wb, String aba, Function<Linha, T> mapeador) {
        Sheet sheet = wb.getSheet(aba);
        if (sheet == null) {
            log.warn("Aba '{}' não encontrada.", aba);
            return List.of();
        }
        int inicio = sheet.getFirstRowNum();
        Row cab = sheet.getRow(inicio);
        Map<String, Integer> colunas = new HashMap<>();
        for (Cell c : cab) {
            colunas.put(normalizaCabecalho(FMT.formatCellValue(c)), c.getColumnIndex());
        }
        List<T> saida = new ArrayList<>();
        for (int i = inicio + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            T obj = mapeador.apply(new Linha(row, colunas));
            if (obj != null) saida.add(obj);
        }
        log.info("Aba {}: {} linhas lidas.", aba, saida.size());
        return saida;
    }

    /** "ID CLIENTE" -> "ID_CLIENTE"; "CLASSIFICAÇÃO ABC" -> "CLASSIFICACAO_ABC". */
    static String normalizaCabecalho(String s) {
        return Texto.normaliza(s).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    /** Uma linha da planilha com leitura tolerante a células vazias e tipos misturados. */
    private record Linha(Row row, Map<String, Integer> colunas) {
        String str(String col) {
            Integer i = colunas.get(col);
            if (i == null) return null;
            Cell c = row.getCell(i);
            if (c == null) return null;
            String s = FMT.formatCellValue(c).trim();
            return s.isEmpty() ? null : s;
        }

        Long num(String col) {
            Double d = dec(col);
            return d == null ? null : Math.round(d);
        }

        Double dec(String col) {
            Integer i = colunas.get(col);
            if (i == null) return null;
            Cell c = row.getCell(i);
            if (c == null) return null;
            if (c.getCellType() == CellType.NUMERIC) return c.getNumericCellValue();
            String s = str(col);
            if (s == null) return null;
            try {
                return Double.valueOf(s.replace(".", "").replace(",", "."));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
