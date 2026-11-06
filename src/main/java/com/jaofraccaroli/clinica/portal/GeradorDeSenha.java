package com.jaofraccaroli.clinica.portal;

import java.security.SecureRandom;
import java.util.Random;

import org.springframework.stereotype.Component;

/**
 * Gera senhas aleatórias para o paciente. Não usa caracteres que se confundem
 * ao ditar ou ler (0 e O, 1, l e I), porque a psicóloga entrega a senha ao paciente.
 */
@Component
public class GeradorDeSenha {

    static final String CARACTERES = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    static final int TAMANHO = 10;

    private final Random aleatorio;

    public GeradorDeSenha() {
        this(new SecureRandom());
    }

    GeradorDeSenha(Random aleatorio) {
        this.aleatorio = aleatorio;
    }

    public String gerar() {
        StringBuilder senha = new StringBuilder(TAMANHO);
        for (int i = 0; i < TAMANHO; i++) {
            senha.append(CARACTERES.charAt(aleatorio.nextInt(CARACTERES.length())));
        }
        return senha.toString();
    }
}
