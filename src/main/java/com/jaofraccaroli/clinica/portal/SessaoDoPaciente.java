package com.jaofraccaroli.clinica.portal;

import java.time.LocalDateTime;

/** O que o paciente vê de uma sessão: só data e situação, nunca as anotações. */
public record SessaoDoPaciente(LocalDateTime dataHora, String status) {
}
