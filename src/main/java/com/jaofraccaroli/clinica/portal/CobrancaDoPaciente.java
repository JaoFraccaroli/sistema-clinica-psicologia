package com.jaofraccaroli.clinica.portal;

import java.math.BigDecimal;
import java.time.LocalDate;

/** O que o paciente vê de uma cobrança. */
public record CobrancaDoPaciente(BigDecimal valor, LocalDate vencimento, String situacao,
                                 boolean atrasada, LocalDate dataPagamento) {
}
