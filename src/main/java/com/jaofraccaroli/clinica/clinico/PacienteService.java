package com.jaofraccaroli.clinica.clinico;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jaofraccaroli.clinica.comum.ValidadorCpf;

@Service
public class PacienteService {

    private final PacienteRepository pacientes;
    private final SessaoRepository sessoes;

    public PacienteService(PacienteRepository pacientes, SessaoRepository sessoes) {
        this.pacientes = pacientes;
        this.sessoes = sessoes;
    }

    public List<Paciente> listar() {
        return pacientes.findAllByOrderByNomeAsc();
    }

    public Paciente buscar(Long id) {
        return pacientes.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Paciente não encontrado: " + id));
    }

    @Transactional
    public Paciente cadastrar(Paciente paciente) {
        if (paciente.getCodigo() == null || paciente.getCodigo().isBlank()) {
            paciente.setCodigo(gerarCodigo());
        }
        // CPF sempre guardado no mesmo formato (000.000.000-00); vazio vira nulo
        String cpf = paciente.getCpfPagador();
        if (cpf == null || cpf.isBlank()) {
            paciente.setCpfPagador(null);
        } else if (ValidadorCpf.valido(cpf)) {
            paciente.setCpfPagador(ValidadorCpf.formatar(cpf));
        }
        return pacientes.save(paciente);
    }

    public List<Sessao> sessoesDo(Long pacienteId) {
        return sessoes.findByPacienteIdOrderByDataHoraDesc(pacienteId);
    }

    @Transactional
    public Sessao registrarSessao(Long pacienteId, LocalDateTime dataHora, StatusSessao status, String anotacoes) {
        if (dataHora == null) {
            throw new IllegalArgumentException("Informe a data e a hora da sessão");
        }
        Sessao sessao = new Sessao();
        sessao.setPaciente(buscar(pacienteId));
        sessao.setDataHora(dataHora);
        sessao.setStatus(status == null ? StatusSessao.AGENDADA : status);
        sessao.setAnotacoes(anotacoes);
        return sessoes.save(sessao);
    }

    private String gerarCodigo() {
        return "PAC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
