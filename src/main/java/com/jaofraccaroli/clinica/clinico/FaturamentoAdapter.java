package com.jaofraccaroli.clinica.clinico;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.jaofraccaroli.clinica.financeiro.DadosFaturamento;
import com.jaofraccaroli.clinica.financeiro.FonteDeDadosFaturamento;

/**
 * Único ponto onde dados saem da área clínica para o financeiro.
 * Só são enviados o código do paciente, quem paga e o valor da sessão.
 */
@Component
public class FaturamentoAdapter implements FonteDeDadosFaturamento {

    private final PacienteRepository pacientes;

    public FaturamentoAdapter(PacienteRepository pacientes) {
        this.pacientes = pacientes;
    }

    @Override
    public List<DadosFaturamento> listarTodos() {
        return pacientes.findAllByOrderByNomeAsc().stream().map(this::paraFaturamento).toList();
    }

    @Override
    public Optional<DadosFaturamento> buscarPorCodigo(String codigoPaciente) {
        return pacientes.findByCodigo(codigoPaciente).map(this::paraFaturamento);
    }

    private DadosFaturamento paraFaturamento(Paciente p) {
        return new DadosFaturamento(p.getCodigo(), p.getNomePagador(), p.getCpfPagador(), p.getValorSessao());
    }
}
