package com.jaofraccaroli.clinica.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CobrancaService {

    private final CobrancaRepository cobrancas;
    private final FonteDeDadosFaturamento dadosFaturamento;

    public CobrancaService(CobrancaRepository cobrancas, FonteDeDadosFaturamento dadosFaturamento) {
        this.cobrancas = cobrancas;
        this.dadosFaturamento = dadosFaturamento;
    }

    public List<Cobranca> listar() {
        return cobrancas.findAllByOrderByVencimentoAsc();
    }

    /** Cobranças de um único paciente (usado na área do paciente). */
    public List<Cobranca> listarDoPaciente(String codigoPaciente) {
        return cobrancas.findByCodigoPacienteOrderByVencimentoAsc(codigoPaciente);
    }

    public List<DadosFaturamento> pacientesParaCobranca() {
        return dadosFaturamento.listarTodos();
    }

    @Transactional
    public Cobranca criar(String codigoPaciente, BigDecimal valor, LocalDate vencimento) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor da cobrança deve ser maior que zero");
        }
        if (vencimento == null) {
            throw new IllegalArgumentException("Informe o vencimento");
        }
        DadosFaturamento dados = dadosFaturamento.buscarPorCodigo(codigoPaciente)
                .orElseThrow(() -> new IllegalArgumentException("Paciente não encontrado: " + codigoPaciente));

        Cobranca cobranca = new Cobranca();
        cobranca.setCodigoPaciente(dados.codigoPaciente());
        cobranca.setNomePagador(dados.nomePagador());
        cobranca.setCpfPagador(dados.cpfPagador());
        cobranca.setValor(valor);
        cobranca.setVencimento(vencimento);
        return cobrancas.save(cobranca);
    }

    @Transactional
    public Cobranca registrarPagamento(Long id, LocalDate dataPagamento) {
        Cobranca cobranca = cobrancas.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cobrança não encontrada: " + id));
        if (cobranca.getStatus() == StatusCobranca.PAGA) {
            throw new IllegalStateException("Esta cobrança já foi paga");
        }
        cobranca.setStatus(StatusCobranca.PAGA);
        cobranca.setDataPagamento(dataPagamento);
        return cobrancas.save(cobranca);
    }
}
