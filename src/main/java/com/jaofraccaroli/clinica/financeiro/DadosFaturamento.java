package com.jaofraccaroli.clinica.financeiro;

import java.math.BigDecimal;

/**
 * Únicos dados de um paciente que a área financeira pode receber:
 * o código do paciente e quem paga. Nada de prontuário, anotações ou motivo do atendimento.
 */
public record DadosFaturamento(
        String codigoPaciente,
        String nomePagador,
        String cpfPagador,
        BigDecimal valorSessao) {
}
