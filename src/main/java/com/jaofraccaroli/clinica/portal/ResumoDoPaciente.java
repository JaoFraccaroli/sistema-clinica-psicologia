package com.jaofraccaroli.clinica.portal;

import java.math.BigDecimal;
import java.util.List;

/** Tudo o que aparece na área do paciente. */
public record ResumoDoPaciente(
        String codigo,
        String nome,
        List<SessaoDoPaciente> proximasSessoes,
        List<SessaoDoPaciente> sessoesAnteriores,
        List<CobrancaDoPaciente> cobrancas,
        BigDecimal totalEmAberto) {
}
