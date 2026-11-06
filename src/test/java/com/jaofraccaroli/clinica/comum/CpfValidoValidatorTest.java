package com.jaofraccaroli.clinica.comum;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** O CPF do pagador é opcional: vazio passa, mas preenchido precisa ser válido. */
class CpfValidoValidatorTest {

    private final CpfValidoValidator validador = new CpfValidoValidator();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "   " })
    void campoVazioEhAceito(String cpf) {
        assertThat(validador.isValid(cpf, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "529.982.247-25", "52998224725" })
    void cpfValidoEhAceito(String cpf) {
        assertThat(validador.isValid(cpf, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "529.982.247-24", "111.111.111-11", "123" })
    void cpfInvalidoEhRecusado(String cpf) {
        assertThat(validador.isValid(cpf, null)).isFalse();
    }
}
