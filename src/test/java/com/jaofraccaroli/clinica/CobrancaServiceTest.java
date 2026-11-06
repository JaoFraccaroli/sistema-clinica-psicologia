package com.jaofraccaroli.clinica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jaofraccaroli.clinica.financeiro.Cobranca;
import com.jaofraccaroli.clinica.financeiro.CobrancaRepository;
import com.jaofraccaroli.clinica.financeiro.CobrancaService;
import com.jaofraccaroli.clinica.financeiro.DadosFaturamento;
import com.jaofraccaroli.clinica.financeiro.FonteDeDadosFaturamento;
import com.jaofraccaroli.clinica.financeiro.StatusCobranca;

@ExtendWith(MockitoExtension.class)
class CobrancaServiceTest {

    @Mock
    private CobrancaRepository cobrancas;

    @Mock
    private FonteDeDadosFaturamento dadosFaturamento;

    @InjectMocks
    private CobrancaService service;

    @Test
    void criaCobrancaComOsDadosDoPagador() {
        when(dadosFaturamento.buscarPorCodigo("PAC-1"))
                .thenReturn(Optional.of(new DadosFaturamento("PAC-1", "Maria Pagadora", "123", new BigDecimal("150"))));
        when(cobrancas.save(any(Cobranca.class))).thenAnswer(i -> i.getArgument(0));

        Cobranca cobranca = service.criar("PAC-1", new BigDecimal("150.00"), LocalDate.of(2026, 12, 1));

        assertThat(cobranca.getCodigoPaciente()).isEqualTo("PAC-1");
        assertThat(cobranca.getNomePagador()).isEqualTo("Maria Pagadora");
        assertThat(cobranca.getStatus()).isEqualTo(StatusCobranca.PENDENTE);
    }

    // Valores de fronteira do valor da cobrança: o limite é zero
    @ParameterizedTest(name = "valor {0} é recusado")
    @ValueSource(strings = { "0", "0.00", "-0.01", "-150.00" })
    void naoAceitaValorZeroOuNegativo(String valor) {
        assertThatThrownBy(() -> service.criar("PAC-1", new BigDecimal(valor), LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maior que zero");
    }

    @ParameterizedTest(name = "valor {0} é aceito")
    @ValueSource(strings = { "0.01", "1", "150.00", "99999.99" })
    void aceitaValorPositivo(String valor) {
        when(dadosFaturamento.buscarPorCodigo("PAC-1"))
                .thenReturn(Optional.of(new DadosFaturamento("PAC-1", "Maria", "529.982.247-25", new BigDecimal("150"))));
        when(cobrancas.save(any(Cobranca.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(service.criar("PAC-1", new BigDecimal(valor), LocalDate.now()).getValor())
                .isEqualByComparingTo(valor);
    }

    @Test
    void naoAceitaValorNulo() {
        assertThatThrownBy(() -> service.criar("PAC-1", null, LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void naoAceitaVencimentoNulo() {
        assertThatThrownBy(() -> service.criar("PAC-1", BigDecimal.TEN, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vencimento");
    }

    @Test
    void copiaOCpfDoPagador() {
        when(dadosFaturamento.buscarPorCodigo("PAC-1"))
                .thenReturn(Optional.of(new DadosFaturamento("PAC-1", "Maria", "529.982.247-25", new BigDecimal("150"))));
        when(cobrancas.save(any(Cobranca.class))).thenAnswer(i -> i.getArgument(0));

        Cobranca cobranca = service.criar("PAC-1", BigDecimal.TEN, LocalDate.now());

        assertThat(cobranca.getCpfPagador()).isEqualTo("529.982.247-25");
        assertThat(cobranca.getVencimento()).isEqualTo(LocalDate.now());
    }

    @Test
    void naoRegistraPagamentoDeCobrancaInexistente() {
        when(cobrancas.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarPagamento(99L, LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void naoCriaCobrancaParaPacienteInexistente() {
        when(dadosFaturamento.buscarPorCodigo("NAO-EXISTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar("NAO-EXISTE", BigDecimal.TEN, LocalDate.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registraPagamento() {
        Cobranca pendente = new Cobranca();
        when(cobrancas.findById(1L)).thenReturn(Optional.of(pendente));
        when(cobrancas.save(any(Cobranca.class))).thenAnswer(i -> i.getArgument(0));

        Cobranca paga = service.registrarPagamento(1L, LocalDate.of(2026, 11, 5));

        assertThat(paga.getStatus()).isEqualTo(StatusCobranca.PAGA);
        assertThat(paga.getDataPagamento()).isEqualTo(LocalDate.of(2026, 11, 5));
    }

    @Test
    void naoPagaDuasVezes() {
        Cobranca jaPaga = new Cobranca();
        jaPaga.setStatus(StatusCobranca.PAGA);
        when(cobrancas.findById(1L)).thenReturn(Optional.of(jaPaga));

        assertThatThrownBy(() -> service.registrarPagamento(1L, LocalDate.now()))
                .isInstanceOf(IllegalStateException.class);
    }

    // Encontrados pelo PIT: estes métodos não tinham teste de unidade (mutantes "sem cobertura")
    @Test
    void listaAsCobrancasDoRepositorio() {
        Cobranca cobranca = new Cobranca();
        when(cobrancas.findAllByOrderByVencimentoAsc()).thenReturn(List.of(cobranca));

        assertThat(service.listar()).containsExactly(cobranca);
    }

    @Test
    void pacientesParaCobrancaVemDaAreaClinica() {
        DadosFaturamento dados = new DadosFaturamento("PAC-1", "Maria", "529.982.247-25", new BigDecimal("150"));
        when(dadosFaturamento.listarTodos()).thenReturn(List.of(dados));

        assertThat(service.pacientesParaCobranca()).containsExactly(dados);
    }
}
