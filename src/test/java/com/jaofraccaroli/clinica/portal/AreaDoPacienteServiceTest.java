package com.jaofraccaroli.clinica.portal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jaofraccaroli.clinica.clinico.Paciente;
import com.jaofraccaroli.clinica.clinico.PacienteService;
import com.jaofraccaroli.clinica.clinico.Sessao;
import com.jaofraccaroli.clinica.clinico.StatusSessao;
import com.jaofraccaroli.clinica.financeiro.Cobranca;
import com.jaofraccaroli.clinica.financeiro.CobrancaService;
import com.jaofraccaroli.clinica.financeiro.StatusCobranca;

@ExtendWith(MockitoExtension.class)
class AreaDoPacienteServiceTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 6, 12, 0);

    @Mock
    private PacienteService pacientes;

    @Mock
    private CobrancaService cobrancas;

    @InjectMocks
    private AreaDoPacienteService service;

    @Test
    void separaSessoesFuturasDasAnteriores() {
        paciente(List.of(
                sessao(AGORA.minusDays(14), StatusSessao.REALIZADA),
                sessao(AGORA.plusDays(14), StatusSessao.AGENDADA),
                sessao(AGORA.minusDays(7), StatusSessao.CANCELADA),
                sessao(AGORA.plusDays(7), StatusSessao.AGENDADA)), List.of());

        ResumoDoPaciente resumo = service.resumo("PAC-1", AGORA);

        // Próximas: da mais perto para a mais longe
        assertThat(resumo.proximasSessoes()).extracting(SessaoDoPaciente::dataHora)
                .containsExactly(AGORA.plusDays(7), AGORA.plusDays(14));
        // Anteriores: da mais recente para a mais antiga
        assertThat(resumo.sessoesAnteriores()).extracting(SessaoDoPaciente::dataHora)
                .containsExactly(AGORA.minusDays(7), AGORA.minusDays(14));
        assertThat(resumo.sessoesAnteriores()).extracting(SessaoDoPaciente::status)
                .containsExactly("Cancelada", "Realizada");
    }

    @Test
    void sessaoExatamenteAgoraContaComoProxima() {
        paciente(List.of(
                sessao(AGORA, StatusSessao.AGENDADA),
                sessao(AGORA.minusMinutes(1), StatusSessao.REALIZADA)), List.of());

        ResumoDoPaciente resumo = service.resumo("PAC-1", AGORA);

        assertThat(resumo.proximasSessoes()).extracting(SessaoDoPaciente::dataHora).containsExactly(AGORA);
        assertThat(resumo.sessoesAnteriores()).extracting(SessaoDoPaciente::dataHora)
                .containsExactly(AGORA.minusMinutes(1));
    }

    @Test
    void sessaoDoPacienteNaoTemCampoDeAnotacoes() {
        assertThat(SessaoDoPaciente.class.getRecordComponents())
                .extracting(c -> c.getName())
                .containsExactly("dataHora", "status");
    }

    @Test
    void mostraNomeECodigoDoProprioPaciente() {
        paciente(List.of(), List.of());

        ResumoDoPaciente resumo = service.resumo("PAC-1", AGORA);

        assertThat(resumo.codigo()).isEqualTo("PAC-1");
        assertThat(resumo.nome()).isEqualTo("Paciente Teste");
        assertThat(resumo.proximasSessoes()).isEmpty();
        assertThat(resumo.cobrancas()).isEmpty();
        assertThat(resumo.totalEmAberto()).isEqualByComparingTo("0");
    }

    @Test
    void cobrancasComSituacaoETotalEmAberto() {
        Cobranca atrasada = cobranca("150.00", AGORA.toLocalDate().minusDays(1));
        Cobranca pendente = cobranca("180.50", AGORA.toLocalDate());
        Cobranca paga = cobranca("99.90", AGORA.toLocalDate().minusDays(10));
        paga.setStatus(StatusCobranca.PAGA);
        paga.setDataPagamento(AGORA.toLocalDate().minusDays(9));
        paciente(List.of(), List.of(atrasada, pendente, paga));

        ResumoDoPaciente resumo = service.resumo("PAC-1", AGORA);

        assertThat(resumo.cobrancas()).extracting(CobrancaDoPaciente::situacao)
                .containsExactly("Atrasada", "Pendente", "Paga");
        assertThat(resumo.cobrancas()).extracting(CobrancaDoPaciente::atrasada)
                .containsExactly(true, false, false);
        assertThat(resumo.cobrancas().get(2).dataPagamento()).isEqualTo(AGORA.toLocalDate().minusDays(9));
        // Só soma o que não foi pago: 150,00 + 180,50
        assertThat(resumo.totalEmAberto()).isEqualByComparingTo("330.50");
    }

    @Test
    void pacienteDesconhecidoNaoTemArea() {
        when(pacientes.buscarPorCodigo("PAC-X")).thenThrow(new IllegalArgumentException("Paciente não encontrado"));

        assertThatThrownBy(() -> service.resumo("PAC-X", AGORA)).isInstanceOf(IllegalArgumentException.class);
    }

    private void paciente(List<Sessao> sessoes, List<Cobranca> lista) {
        Paciente p = new Paciente();
        p.setId(1L);
        p.setCodigo("PAC-1");
        p.setNome("Paciente Teste");
        when(pacientes.buscarPorCodigo("PAC-1")).thenReturn(p);
        when(pacientes.sessoesDo(1L)).thenReturn(sessoes);
        when(cobrancas.listarDoPaciente("PAC-1")).thenReturn(lista);
    }

    private static Sessao sessao(LocalDateTime quando, StatusSessao status) {
        Sessao s = new Sessao();
        s.setDataHora(quando);
        s.setStatus(status);
        s.setAnotacoes("Anotação sigilosa");
        return s;
    }

    private static Cobranca cobranca(String valor, LocalDate vencimento) {
        Cobranca c = new Cobranca();
        c.setCodigoPaciente("PAC-1");
        c.setValor(new BigDecimal(valor));
        c.setVencimento(vencimento);
        return c;
    }
}
