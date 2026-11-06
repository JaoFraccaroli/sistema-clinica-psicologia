package com.jaofraccaroli.clinica.clinico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jaofraccaroli.clinica.financeiro.DadosFaturamento;

/** A única saída de dados da área clínica para o financeiro. */
@ExtendWith(MockitoExtension.class)
class FaturamentoAdapterTest {

    @Mock
    private PacienteRepository pacientes;

    @InjectMocks
    private FaturamentoAdapter adapter;

    @Test
    void enviaSoCodigoPagadorCpfEValor() {
        when(pacientes.findByCodigo("PAC-1")).thenReturn(Optional.of(paciente()));

        DadosFaturamento dados = adapter.buscarPorCodigo("PAC-1").orElseThrow();

        assertThat(dados).isEqualTo(new DadosFaturamento(
                "PAC-1", "Mãe da Paciente", "529.982.247-25", new BigDecimal("150.00")));
        assertThat(dados.toString())
                .doesNotContain("Nome Sigiloso", "sigiloso@exemplo.com", "(19) 99999-0000");
    }

    @Test
    void codigoDesconhecidoNaoDevolveNada() {
        when(pacientes.findByCodigo("PAC-X")).thenReturn(Optional.empty());

        assertThat(adapter.buscarPorCodigo("PAC-X")).isEmpty();
    }

    @Test
    void listaTodosNaMesmaOrdem() {
        Paciente outro = paciente();
        outro.setCodigo("PAC-2");
        when(pacientes.findAllByOrderByNomeAsc()).thenReturn(List.of(paciente(), outro));

        assertThat(adapter.listarTodos()).extracting(DadosFaturamento::codigoPaciente)
                .containsExactly("PAC-1", "PAC-2");
    }

    private static Paciente paciente() {
        Paciente p = new Paciente();
        p.setCodigo("PAC-1");
        p.setNome("Nome Sigiloso");
        p.setEmail("sigiloso@exemplo.com");
        p.setTelefone("(19) 99999-0000");
        p.setNomePagador("Mãe da Paciente");
        p.setCpfPagador("529.982.247-25");
        p.setValorSessao(new BigDecimal("150.00"));
        return p;
    }
}
