package com.jaofraccaroli.clinica.portal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.jaofraccaroli.clinica.clinico.Paciente;
import com.jaofraccaroli.clinica.clinico.PacienteService;
import com.jaofraccaroli.clinica.clinico.Sessao;
import com.jaofraccaroli.clinica.financeiro.Cobranca;
import com.jaofraccaroli.clinica.financeiro.CobrancaService;
import com.jaofraccaroli.clinica.financeiro.SituacaoCobranca;

/**
 * Monta a área do paciente. Só recebe o código de quem fez login, então um
 * paciente nunca consegue pedir os dados de outro. As anotações clínicas
 * ficam de fora: SessaoDoPaciente não tem esse campo.
 */
@Service
public class AreaDoPacienteService {

    private final PacienteService pacientes;
    private final CobrancaService cobrancas;

    public AreaDoPacienteService(PacienteService pacientes, CobrancaService cobrancas) {
        this.pacientes = pacientes;
        this.cobrancas = cobrancas;
    }

    public ResumoDoPaciente resumo(String codigoPaciente, LocalDateTime agora) {
        Paciente paciente = pacientes.buscarPorCodigo(codigoPaciente);
        List<Sessao> sessoes = pacientes.sessoesDo(paciente.getId());

        List<SessaoDoPaciente> proximas = sessoes.stream()
                .filter(s -> !s.getDataHora().isBefore(agora))
                .sorted(Comparator.comparing(Sessao::getDataHora))
                .map(AreaDoPacienteService::paraPaciente)
                .toList();
        List<SessaoDoPaciente> anteriores = sessoes.stream()
                .filter(s -> s.getDataHora().isBefore(agora))
                .sorted(Comparator.comparing(Sessao::getDataHora).reversed())
                .map(AreaDoPacienteService::paraPaciente)
                .toList();

        List<CobrancaDoPaciente> minhasCobrancas = cobrancas.listarDoPaciente(paciente.getCodigo()).stream()
                .map(c -> paraPaciente(c, agora))
                .toList();
        BigDecimal emAberto = minhasCobrancas.stream()
                .filter(c -> c.dataPagamento() == null)
                .map(CobrancaDoPaciente::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ResumoDoPaciente(paciente.getCodigo(), paciente.getNome(),
                proximas, anteriores, minhasCobrancas, emAberto);
    }

    private static SessaoDoPaciente paraPaciente(Sessao s) {
        return new SessaoDoPaciente(s.getDataHora(), s.getStatus().getDescricao());
    }

    private static CobrancaDoPaciente paraPaciente(Cobranca c, LocalDateTime agora) {
        SituacaoCobranca situacao = c.situacaoEm(agora.toLocalDate());
        return new CobrancaDoPaciente(c.getValor(), c.getVencimento(), situacao.getDescricao(),
                situacao == SituacaoCobranca.ATRASADA, c.getDataPagamento());
    }
}
