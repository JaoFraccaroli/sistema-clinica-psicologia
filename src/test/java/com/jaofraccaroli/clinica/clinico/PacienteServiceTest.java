package com.jaofraccaroli.clinica.clinico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PacienteServiceTest {

    @Mock
    private PacienteRepository pacientes;

    @Mock
    private SessaoRepository sessoes;

    @InjectMocks
    private PacienteService service;

    @Test
    void geraCodigoNoFormatoPacMaisOitoCaracteres() {
        salvarDevolveOMesmo();

        Paciente salvo = service.cadastrar(paciente(null));

        assertThat(salvo.getCodigo()).matches("PAC-[0-9A-F]{8}");
    }

    @ParameterizedTest(name = "código \"{0}\" é gerado")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void codigoVazioEhGerado(String codigo) {
        salvarDevolveOMesmo();

        assertThat(service.cadastrar(paciente(codigo)).getCodigo()).startsWith("PAC-");
    }

    @Test
    void mantemCodigoInformado() {
        salvarDevolveOMesmo();

        assertThat(service.cadastrar(paciente("PAC-DEMO0001")).getCodigo()).isEqualTo("PAC-DEMO0001");
    }

    @Test
    void cemCadastrosGeramCemCodigosDiferentes() {
        salvarDevolveOMesmo();
        Set<String> codigos = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            codigos.add(service.cadastrar(paciente(null)).getCodigo());
        }
        assertThat(codigos).hasSize(100);
    }

    @ParameterizedTest(name = "CPF \"{0}\" é guardado como \"{1}\"")
    @CsvSource(nullValues = "NULO", value = {
            "52998224725,     529.982.247-25",
            "529.982.247-25,  529.982.247-25",
            "' 52998224725 ', 529.982.247-25",
            "'',              NULO",
            "'   ',           NULO",
            "NULO,            NULO",
            "123,             123"
    })
    void normalizaOCpf(String digitado, String guardado) {
        salvarDevolveOMesmo();
        Paciente p = paciente(null);
        p.setCpfPagador(digitado);

        assertThat(service.cadastrar(p).getCpfPagador()).isEqualTo(guardado);
    }

    @Test
    void buscarPacienteQueNaoExisteDaErro() {
        when(pacientes.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    @Test
    void buscarPorCodigo() {
        Paciente p = paciente("PAC-1");
        when(pacientes.findByCodigo("PAC-1")).thenReturn(Optional.of(p));
        when(pacientes.findByCodigo("PAC-2")).thenReturn(Optional.empty());

        assertThat(service.buscarPorCodigo("PAC-1")).isSameAs(p);
        assertThat(service.existeCodigo("PAC-1")).isTrue();
        assertThat(service.existeCodigo("PAC-2")).isFalse();
        assertThatThrownBy(() -> service.buscarPorCodigo("PAC-2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PAC-2");
    }

    @Test
    void listarEmOrdemAlfabetica() {
        List<Paciente> lista = List.of(paciente("PAC-1"));
        when(pacientes.findAllByOrderByNomeAsc()).thenReturn(lista);

        assertThat(service.listar()).isSameAs(lista);
    }

    @Test
    void sessaoSemDataNaoEhRegistrada() {
        assertThatThrownBy(() -> service.registrarSessao(1L, null, StatusSessao.AGENDADA, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("data");
        verify(sessoes, never()).save(any());
    }

    @Test
    void sessaoSemStatusFicaAgendada() {
        pacienteExiste();

        Sessao sessao = service.registrarSessao(1L, LocalDateTime.of(2026, 11, 10, 14, 0), null, null);

        assertThat(sessao.getStatus()).isEqualTo(StatusSessao.AGENDADA);
    }

    @ParameterizedTest
    @EnumSource(StatusSessao.class)
    void sessaoGuardaStatusDataEAnotacoes(StatusSessao status) {
        Paciente p = pacienteExiste();
        LocalDateTime quando = LocalDateTime.of(2026, 11, 10, 14, 0);

        Sessao sessao = service.registrarSessao(1L, quando, status, "Anotação");

        assertThat(sessao.getPaciente()).isSameAs(p);
        assertThat(sessao.getDataHora()).isEqualTo(quando);
        assertThat(sessao.getStatus()).isEqualTo(status);
        assertThat(sessao.getAnotacoes()).isEqualTo("Anotação");
    }

    @Test
    void sessaoParaPacienteQueNaoExisteDaErro() {
        when(pacientes.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarSessao(99L, LocalDateTime.now(), null, null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(sessoes, never()).save(any());
    }

    @Test
    void sessoesDoPacienteDaMaisRecenteParaAMaisAntiga() {
        List<Sessao> lista = List.of(new Sessao());
        when(sessoes.findByPacienteIdOrderByDataHoraDesc(1L)).thenReturn(lista);

        assertThat(service.sessoesDo(1L)).isSameAs(lista);
    }

    @ParameterizedTest
    @CsvSource({"AGENDADA, Agendada", "REALIZADA, Realizada", "CANCELADA, Cancelada"})
    void cadaStatusDeSessaoTemDescricao(StatusSessao status, String descricao) {
        assertThat(status.getDescricao()).isEqualTo(descricao);
    }

    private void salvarDevolveOMesmo() {
        when(pacientes.save(any(Paciente.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Paciente pacienteExiste() {
        Paciente p = paciente("PAC-1");
        when(pacientes.findById(1L)).thenReturn(Optional.of(p));
        when(sessoes.save(any(Sessao.class))).thenAnswer(i -> i.getArgument(0));
        return p;
    }

    private static Paciente paciente(String codigo) {
        Paciente p = new Paciente();
        p.setCodigo(codigo);
        p.setNome("Paciente Teste");
        p.setNomePagador("Pagador");
        p.setValorSessao(new BigDecimal("150.00"));
        return p;
    }
}
