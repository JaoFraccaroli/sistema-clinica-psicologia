package com.jaofraccaroli.clinica.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Cobrança da área financeira. Guarda apenas o código do paciente,
 * sem nenhuma ligação com as tabelas clínicas.
 */
@Entity
@Table(name = "fin_cobranca")
public class Cobranca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String codigoPaciente;

    @Column(nullable = false)
    private String nomePagador;

    private String cpfPagador;

    @Column(nullable = false)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDate vencimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusCobranca status = StatusCobranca.PENDENTE;

    private LocalDate dataPagamento;

    /**
     * Situação na data informada: paga, atrasada (vencimento já passou) ou pendente
     * (vence hoje ou depois). Recebe a data para poder ser testada sem depender do relógio.
     */
    public SituacaoCobranca situacaoEm(LocalDate hoje) {
        if (status == StatusCobranca.PAGA) {
            return SituacaoCobranca.PAGA;
        }
        return vencimento.isBefore(hoje) ? SituacaoCobranca.ATRASADA : SituacaoCobranca.PENDENTE;
    }

    /** Dias de atraso na data informada (zero se não estiver atrasada). */
    public long diasEmAtraso(LocalDate hoje) {
        if (situacaoEm(hoje) != SituacaoCobranca.ATRASADA) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(vencimento, hoje);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigoPaciente() { return codigoPaciente; }
    public void setCodigoPaciente(String codigoPaciente) { this.codigoPaciente = codigoPaciente; }
    public String getNomePagador() { return nomePagador; }
    public void setNomePagador(String nomePagador) { this.nomePagador = nomePagador; }
    public String getCpfPagador() { return cpfPagador; }
    public void setCpfPagador(String cpfPagador) { this.cpfPagador = cpfPagador; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public LocalDate getVencimento() { return vencimento; }
    public void setVencimento(LocalDate vencimento) { this.vencimento = vencimento; }
    public StatusCobranca getStatus() { return status; }
    public void setStatus(StatusCobranca status) { this.status = status; }
    public LocalDate getDataPagamento() { return dataPagamento; }
    public void setDataPagamento(LocalDate dataPagamento) { this.dataPagamento = dataPagamento; }
}
