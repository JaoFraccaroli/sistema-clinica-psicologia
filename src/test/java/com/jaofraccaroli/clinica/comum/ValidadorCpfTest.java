package com.jaofraccaroli.clinica.comum;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;

/**
 * Testes dirigidos por dados: os casos de CPF ficam em src/test/resources/dados/cpfs.csv,
 * cada linha com o CPF, o resultado esperado e o motivo do caso.
 */
class ValidadorCpfTest {

    @ParameterizedTest(name = "[{index}] \"{0}\" → {1} ({2})")
    @CsvFileSource(resources = "/dados/cpfs.csv", delimiter = ';', numLinesToSkip = 1, ignoreLeadingAndTrailingWhitespace = false)
    void validaCpfConformeAPlanilhaDeCasos(String cpf, boolean esperado, String motivo) {
        assertThat(ValidadorCpf.valido(cpf)).as(motivo).isEqualTo(esperado);
    }

    @ParameterizedTest
    @NullSource
    void cpfNuloNaoEhValido(String cpf) {
        assertThat(ValidadorCpf.valido(cpf)).isFalse();
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "52998224725, 529.982.247-25",
            "529.982.247-25, 529.982.247-25",
            "00000000191, 000.000.001-91",
    })
    void formataNoPadraoComPontosETraco(String entrada, String esperado) {
        assertThat(ValidadorCpf.formatar(entrada)).isEqualTo(esperado);
    }

    @Test
    void calculaOsDoisDigitosVerificadores() {
        // 529.982.247-25: primeiro dígito 2, segundo 5
        assertThat(ValidadorCpf.digitoVerificador("529982247", 9)).isEqualTo(2);
        assertThat(ValidadorCpf.digitoVerificador("5299822472", 10)).isEqualTo(5);
    }

    @Test
    void restoDezViraZero() {
        // 987.654.321-00: os dois cálculos dão resto 10, que vira 0
        assertThat(ValidadorCpf.digitoVerificador("987654321", 9)).isZero();
        assertThat(ValidadorCpf.digitoVerificador("9876543210", 10)).isZero();
    }
}
