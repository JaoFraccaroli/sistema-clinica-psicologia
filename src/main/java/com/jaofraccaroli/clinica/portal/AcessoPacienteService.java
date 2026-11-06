package com.jaofraccaroli.clinica.portal;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jaofraccaroli.clinica.clinico.PacienteService;

/** Cria e troca a senha de acesso do paciente. A senha nunca é guardada em texto. */
@Service
public class AcessoPacienteService {

    static final int TAMANHO_MINIMO = 8;

    private final AcessoPacienteRepository acessos;
    private final PacienteService pacientes;
    private final PasswordEncoder codificador;
    private final GeradorDeSenha gerador;

    public AcessoPacienteService(AcessoPacienteRepository acessos, PacienteService pacientes,
                                 PasswordEncoder codificador, GeradorDeSenha gerador) {
        this.acessos = acessos;
        this.pacientes = pacientes;
        this.codificador = codificador;
        this.gerador = gerador;
    }

    /**
     * Gera uma senha nova para o paciente e devolve em texto UMA única vez, para a
     * psicóloga entregar. Se já existia uma senha, a antiga deixa de funcionar.
     */
    @Transactional
    public String gerarNovaSenha(String codigoPaciente) {
        String senha = gerador.gerar();
        definirSenha(codigoPaciente, senha);
        return senha;
    }

    @Transactional
    public void definirSenha(String codigoPaciente, String senha) {
        if (senha == null || senha.length() < TAMANHO_MINIMO) {
            throw new IllegalArgumentException("A senha deve ter pelo menos " + TAMANHO_MINIMO + " caracteres");
        }
        if (codigoPaciente == null || !pacientes.existeCodigo(codigoPaciente)) {
            throw new IllegalArgumentException("Paciente não encontrado: " + codigoPaciente);
        }
        AcessoPaciente acesso = acessos.findByCodigoPaciente(codigoPaciente).orElseGet(AcessoPaciente::new);
        acesso.setCodigoPaciente(codigoPaciente);
        acesso.setSenhaHash(codificador.encode(senha));
        acesso.setAtualizadoEm(LocalDateTime.now());
        acessos.save(acesso);
    }

    public boolean temAcesso(String codigoPaciente) {
        return acessos.findByCodigoPaciente(codigoPaciente).isPresent();
    }
}
