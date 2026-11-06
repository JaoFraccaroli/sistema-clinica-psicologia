package com.jaofraccaroli.clinica.portal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GeradorDeSenhaTest {

    private final GeradorDeSenha gerador = new GeradorDeSenha();

    @RepeatedTest(20)
    void senhaTemDezCaracteresPermitidos() {
        String senha = gerador.gerar();

        assertThat(senha).hasSize(10);
        assertThat(senha.chars()).allMatch(c -> GeradorDeSenha.CARACTERES.indexOf(c) >= 0);
    }

    @ParameterizedTest(name = "nunca usa ''{0}''")
    @ValueSource(chars = {'0', 'O', 'o', '1', 'l', 'I'})
    void naoUsaCaracteresQueSeConfundem(char confuso) {
        assertThat(GeradorDeSenha.CARACTERES).doesNotContain(String.valueOf(confuso));
    }

    @Test
    void alfabetoTemMaiusculasMinusculasENumeros() {
        assertThat(GeradorDeSenha.CARACTERES).containsPattern("[A-Z]").containsPattern("[a-z]").containsPattern("[2-9]");
    }

    @Test
    void milSenhasSaoTodasDiferentes() {
        Set<String> senhas = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            senhas.add(gerador.gerar());
        }
        assertThat(senhas).hasSize(1000);
    }

    @Test
    void usaOSorteioParaCadaCaractere() {
        // Sorteio controlado: sempre o primeiro e depois sempre o último caractere
        GeradorDeSenha primeiro = new GeradorDeSenha(new Random() {
            @Override
            public int nextInt(int limite) {
                return 0;
            }
        });
        GeradorDeSenha ultimo = new GeradorDeSenha(new Random() {
            @Override
            public int nextInt(int limite) {
                return limite - 1;
            }
        });

        assertThat(primeiro.gerar()).isEqualTo("AAAAAAAAAA");
        assertThat(ultimo.gerar()).isEqualTo("9999999999");
    }
}
