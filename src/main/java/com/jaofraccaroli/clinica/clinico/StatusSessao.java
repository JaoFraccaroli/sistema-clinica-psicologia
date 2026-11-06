package com.jaofraccaroli.clinica.clinico;

public enum StatusSessao {
    AGENDADA("Agendada"),
    REALIZADA("Realizada"),
    CANCELADA("Cancelada");

    private final String descricao;

    StatusSessao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
