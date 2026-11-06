package com.jaofraccaroli.clinica.financeiro;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Valores de fronteira da situação da cobrança. A fronteira é o dia do vencimento:
 * um dia antes, no dia e um dia depois.
 */
class SituacaoCobrancaTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 6);

    @ParameterizedTest(name = "vencimento {0} dia(s) em relação a hoje → {1}, {2} dia(s) de atraso")
    @CsvSource({
            "-30, ATRASADA, 30",
            "-2,  ATRASADA, 2",
            "-1,  ATRASADA, 1",   // fronteira: venceu ontem
            "0,   PENDENTE, 0",   // fronteira: vence hoje
            "1,   PENDENTE, 0",   // fronteira: vence amanhã
            "30,  PENDENTE, 0",
    })
    void situacaoDeCobrancaNaoPagaDependeDoVencimento(int diasAteOVencimento, SituacaoCobranca esperada, long atraso) {
        Cobranca cobranca = cobrancaVencendoEm(HOJE.plusDays(diasAteOVencimento));

        assertThat(cobranca.situacaoEm(HOJE)).isEqualTo(esperada);
        assertThat(cobranca.diasEmAtraso(HOJE)).isEqualTo(atraso);
    }

    @ParameterizedTest(name = "paga, vencimento {0} dia(s) em relação a hoje → PAGA, sem atraso")
    @ValueSource(ints = { -30, -1, 0, 1, 30 })
    void cobrancaPagaNuncaFicaAtrasada(int diasAteOVencimento) {
        Cobranca cobranca = cobrancaVencendoEm(HOJE.plusDays(diasAteOVencimento));
        cobranca.setStatus(StatusCobranca.PAGA);

        assertThat(cobranca.situacaoEm(HOJE)).isEqualTo(SituacaoCobranca.PAGA);
        assertThat(cobranca.diasEmAtraso(HOJE)).isZero();
    }

    @ParameterizedTest
    @CsvSource({ "PENDENTE, Pendente", "ATRASADA, Atrasada", "PAGA, Paga" })
    void cadaSituacaoTemUmaDescricaoParaATela(SituacaoCobranca situacao, String descricao) {
        assertThat(situacao.getDescricao()).isEqualTo(descricao);
    }

    private Cobranca cobrancaVencendoEm(LocalDate vencimento) {
        Cobranca cobranca = new Cobranca();
        cobranca.setCodigoPaciente("PAC-TESTE");
        cobranca.setNomePagador("Pagador");
        cobranca.setValor(new BigDecimal("150.00"));
        cobranca.setVencimento(vencimento);
        return cobranca;
    }
}
