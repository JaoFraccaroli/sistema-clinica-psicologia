package com.jaofraccaroli.clinica.clinico;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "clinico_paciente")
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código usado fora da área clínica no lugar do nome do paciente. */
    @Column(nullable = false, unique = true)
    private String codigo;

    @NotBlank(message = "Informe o nome")
    private String nome;

    private LocalDate dataNascimento;

    private String telefone;

    @Email(message = "E-mail inválido")
    private String email;

    /** Quem paga as sessões (o próprio paciente ou um responsável). */
    @NotBlank(message = "Informe quem paga as sessões")
    private String nomePagador;

    private String cpfPagador;

    @NotNull(message = "Informe o valor da sessão")
    @Positive(message = "O valor da sessão deve ser maior que zero")
    private BigDecimal valorSessao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNomePagador() { return nomePagador; }
    public void setNomePagador(String nomePagador) { this.nomePagador = nomePagador; }
    public String getCpfPagador() { return cpfPagador; }
    public void setCpfPagador(String cpfPagador) { this.cpfPagador = cpfPagador; }
    public BigDecimal getValorSessao() { return valorSessao; }
    public void setValorSessao(BigDecimal valorSessao) { this.valorSessao = valorSessao; }
}
