package com.jaofraccaroli.clinica.comum;

/**
 * Regras do CPF: aceita só os formatos "52998224725" ou "529.982.247-25",
 * recusa sequências repetidas (111.111.111-11) e confere os dois dígitos verificadores.
 */
public final class ValidadorCpf {

    private static final String SO_NUMEROS = "\\d{11}";
    private static final String FORMATADO = "\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}";

    private ValidadorCpf() {
    }

    public static boolean valido(String cpf) {
        if (cpf == null) {
            return false;
        }
        String texto = cpf.trim();
        if (!texto.matches(SO_NUMEROS) && !texto.matches(FORMATADO)) {
            return false;
        }
        String digitos = texto.replaceAll("\\D", "");
        if (digitos.chars().distinct().count() == 1) {
            return false;
        }
        return digitoVerificador(digitos, 9) == digitos.charAt(9) - '0'
                && digitoVerificador(digitos, 10) == digitos.charAt(10) - '0';
    }

    /** Calcula o dígito verificador usando os primeiros {@code quantidade} dígitos. */
    static int digitoVerificador(String digitos, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += (digitos.charAt(i) - '0') * (quantidade + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    /** Devolve o CPF no formato 000.000.000-00. Só deve ser chamado com CPF válido. */
    public static String formatar(String cpf) {
        String d = cpf.replaceAll("\\D", "");
        return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9);
    }
}
