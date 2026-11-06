package com.jaofraccaroli.clinica.financeiro;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Planilha das cobranças: formato do Excel em português e nenhum dado clínico. */
class ExportadorCsvCobrancasTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 6);

    private final ExportadorCsvCobrancas exportador = new ExportadorCsvCobrancas();

    @Test
    void planilhaVaziaTemSoOCabecalho() {
        String csv = exportador.gerar(List.of(), HOJE);

        assertThat(csv).isEqualTo(ExportadorCsvCobrancas.BOM + ExportadorCsvCobrancas.CABECALHO + "\r\n");
    }

    @Test
    void comecaComAMarcaUtf8ParaOExcelLerOsAcentos() {
        assertThat(exportador.gerar(List.of(), HOJE)).startsWith("﻿Código do paciente;");
    }

    @Test
    void cabecalhoTemSoColunasFinanceiras() {
        String cabecalho = ExportadorCsvCobrancas.CABECALHO.toLowerCase();

        assertThat(cabecalho.split(";")).containsExactly(
                "código do paciente", "pagador", "cpf do pagador", "valor",
                "vencimento", "situação", "data do pagamento");
        assertThat(cabecalho).doesNotContain("anotaç", "prontuário", "sess", "nome do paciente");
    }

    @Test
    void linhaDeCobrancaPendente() {
        Cobranca c = cobranca("PAC-1A2B3C4D", "Ana Exemplo", "111.444.777-35", "150.00", HOJE.plusDays(5));

        assertThat(linhas(exportador.gerar(List.of(c), HOJE))[1])
                .isEqualTo("PAC-1A2B3C4D;Ana Exemplo;111.444.777-35;150,00;11/11/2026;Pendente;");
    }

    @Test
    void linhaDeCobrancaAtrasada() {
        Cobranca c = cobranca("PAC-1", "Ana", null, "99.9", HOJE.minusDays(1));

        assertThat(linhas(exportador.gerar(List.of(c), HOJE))[1])
                .isEqualTo("PAC-1;Ana;;99,90;05/11/2026;Atrasada;");
    }

    @Test
    void linhaDeCobrancaPagaMostraADataDoPagamento() {
        Cobranca c = cobranca("PAC-1", "Ana", null, "1234.5", HOJE.minusDays(10));
        c.setStatus(StatusCobranca.PAGA);
        c.setDataPagamento(HOJE.minusDays(2));

        assertThat(linhas(exportador.gerar(List.of(c), HOJE))[1])
                .isEqualTo("PAC-1;Ana;;1234,50;27/10/2026;Paga;04/11/2026");
    }

    @Test
    void umaLinhaPorCobrancaNaMesmaOrdem() {
        String csv = exportador.gerar(List.of(
                cobranca("PAC-A", "A", null, "10", HOJE),
                cobranca("PAC-B", "B", null, "20", HOJE)), HOJE);

        String[] linhas = linhas(csv);
        assertThat(linhas).hasSize(3);
        assertThat(linhas[1]).startsWith("PAC-A;");
        assertThat(linhas[2]).startsWith("PAC-B;");
        assertThat(csv).endsWith("\r\n");
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @CsvSource(delimiter = '|', quoteCharacter = '`', value = {
            "Ana Exemplo          | Ana Exemplo",
            "Silva; Souza         | `\"Silva; Souza\"`",
            "Maria \"Mãe\"         | `\"Maria \"\"Mãe\"\"\"`",
            "=SOMA(A1:A9)         | '=SOMA(A1:A9)",
            "+5511999             | '+5511999",
            "-10                  | '-10",
            "@cmd                 | '@cmd",
            "a=b                  | a=b"
    })
    void protegeCadaCelula(String texto, String esperado) {
        assertThat(ExportadorCsvCobrancas.celula(texto)).isEqualTo(esperado);
    }

    @Test
    void celulaComQuebraDeLinhaFicaEntreAspas() {
        assertThat(ExportadorCsvCobrancas.celula("linha1\nlinha2")).isEqualTo("\"linha1\nlinha2\"");
        assertThat(ExportadorCsvCobrancas.celula("linha1\rlinha2")).isEqualTo("\"linha1\rlinha2\"");
    }

    @Test
    void caracteresDeControleNoInicioTambemSaoNeutralizados() {
        assertThat(ExportadorCsvCobrancas.celula("\tcmd")).isEqualTo("'\tcmd");
        assertThat(ExportadorCsvCobrancas.celula("\rcmd")).isEqualTo("\"'\rcmd\"");
    }

    @Test
    void celulaVaziaOuNulaFicaEmBranco() {
        assertThat(ExportadorCsvCobrancas.celula(null)).isEmpty();
        assertThat(ExportadorCsvCobrancas.celula("")).isEmpty();
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @CsvSource({"0.01, '0,01'", "150, '150,00'", "150.005, '150,01'", "150.004, '150,00'", "1000000, '1000000,00'"})
    void valorComDuasCasasEVirgula(BigDecimal valor, String esperado) {
        assertThat(ExportadorCsvCobrancas.valor(valor)).isEqualTo(esperado);
    }

    @Test
    void dataNoFormatoBrasileiro() {
        assertThat(ExportadorCsvCobrancas.data(LocalDate.of(2026, 1, 2))).isEqualTo("02/01/2026");
        assertThat(ExportadorCsvCobrancas.data(null)).isEmpty();
    }

    private static String[] linhas(String csv) {
        return csv.split("\r\n");
    }

    private static Cobranca cobranca(String codigo, String pagador, String cpf, String valor, LocalDate vencimento) {
        Cobranca c = new Cobranca();
        c.setCodigoPaciente(codigo);
        c.setNomePagador(pagador);
        c.setCpfPagador(cpf);
        c.setValor(new BigDecimal(valor));
        c.setVencimento(vencimento);
        return c;
    }
}
