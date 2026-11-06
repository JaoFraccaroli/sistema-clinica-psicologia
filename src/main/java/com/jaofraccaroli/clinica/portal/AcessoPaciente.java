package com.jaofraccaroli.clinica.portal;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Login do paciente: o usuário é o código do paciente e a senha fica guardada
 * apenas como hash (BCrypt). Fica numa tabela própria, fora do cadastro clínico,
 * para que o formulário de paciente nunca consiga alterar a senha.
 */
@Entity
@Table(name = "portal_acesso")
public class AcessoPaciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigoPaciente;

    @Column(nullable = false)
    private String senhaHash;

    @Column(nullable = false)
    private LocalDateTime atualizadoEm;

    public Long getId() { return id; }
    public String getCodigoPaciente() { return codigoPaciente; }
    public void setCodigoPaciente(String codigoPaciente) { this.codigoPaciente = codigoPaciente; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
