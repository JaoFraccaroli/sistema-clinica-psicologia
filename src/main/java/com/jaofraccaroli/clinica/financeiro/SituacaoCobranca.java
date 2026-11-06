package com.jaofraccaroli.clinica.financeiro;

public enum SituacaoCobranca {
    PENDENTE("Pendente"),
    ATRASADA("Atrasada"),
    PAGA("Paga");

    private final String descricao;

    SituacaoCobranca(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
