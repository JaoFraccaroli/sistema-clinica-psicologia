package com.jaofraccaroli.clinica.portal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.jaofraccaroli.clinica.clinico.PacienteService;

@ExtendWith(MockitoExtension.class)
class AcessoPacienteServiceTest {

    @Mock
    private AcessoPacienteRepository acessos;

    @Mock
    private PacienteService pacientes;

    @Mock
    private GeradorDeSenha gerador;

    private final PasswordEncoder codificador = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    private AcessoPacienteService service;

    @BeforeEach
    void criar() {
        service = new AcessoPacienteService(acessos, pacientes, codificador, gerador);
    }

    @Test
    void gerarNovaSenhaDevolveASenhaEGuardaSoOHash() {
        when(gerador.gerar()).thenReturn("SenhaNova23");
        when(pacientes.existeCodigo("PAC-1")).thenReturn(true);
        when(acessos.findByCodigoPaciente("PAC-1")).thenReturn(Optional.empty());

        String senha = service.gerarNovaSenha("PAC-1");

        AcessoPaciente salvo = salvo();
        assertThat(senha).isEqualTo("SenhaNova23");
        assertThat(salvo.getCodigoPaciente()).isEqualTo("PAC-1");
        assertThat(salvo.getSenhaHash()).startsWith("{bcrypt}").doesNotContain("SenhaNova23");
        assertThat(codificador.matches("SenhaNova23", salvo.getSenhaHash())).isTrue();
        assertThat(salvo.getAtualizadoEm()).isNotNull();
    }

    @Test
    void novaSenhaSubstituiAAntigaNoMesmoRegistro() {
        AcessoPaciente existente = new AcessoPaciente();
        existente.setCodigoPaciente("PAC-1");
        existente.setSenhaHash(codificador.encode("SenhaAntiga1"));
        when(pacientes.existeCodigo("PAC-1")).thenReturn(true);
        when(acessos.findByCodigoPaciente("PAC-1")).thenReturn(Optional.of(existente));

        service.definirSenha("PAC-1", "SenhaNova234");

        AcessoPaciente salvo = salvo();
        assertThat(salvo).isSameAs(existente);
        assertThat(codificador.matches("SenhaNova234", salvo.getSenhaHash())).isTrue();
        assertThat(codificador.matches("SenhaAntiga1", salvo.getSenhaHash())).isFalse();
    }

    @Test
    void naoCriaAcessoParaPacienteQueNaoExiste() {
        when(pacientes.existeCodigo("PAC-X")).thenReturn(false);

        assertThatThrownBy(() -> service.definirSenha("PAC-X", "SenhaBoa123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Paciente não encontrado");
        verify(acessos, never()).save(any());
    }

    @Test
    void naoAceitaCodigoNulo() {
        assertThatThrownBy(() -> service.definirSenha(null, "SenhaBoa123"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(acessos, never()).save(any());
    }

    // Valores de fronteira do tamanho da senha: o mínimo é 8
    @ParameterizedTest(name = "senha \"{0}\" é recusada")
    @NullAndEmptySource
    @ValueSource(strings = {"a", "1234567"})
    void recusaSenhaCurta(String senha) {
        assertThatThrownBy(() -> service.definirSenha("PAC-1", senha))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pelo menos 8");
        verify(acessos, never()).save(any());
    }

    @ParameterizedTest(name = "senha com {0} caracteres é aceita")
    @ValueSource(ints = {8, 9, 64})
    void aceitaSenhaAPartirDeOitoCaracteres(int tamanho) {
        when(pacientes.existeCodigo("PAC-1")).thenReturn(true);
        when(acessos.findByCodigoPaciente("PAC-1")).thenReturn(Optional.empty());

        service.definirSenha("PAC-1", "x".repeat(tamanho));

        assertThat(codificador.matches("x".repeat(tamanho), salvo().getSenhaHash())).isTrue();
    }

    @Test
    void informaSePacienteTemAcesso() {
        when(acessos.findByCodigoPaciente("PAC-1")).thenReturn(Optional.of(new AcessoPaciente()));
        when(acessos.findByCodigoPaciente("PAC-2")).thenReturn(Optional.empty());

        assertThat(service.temAcesso("PAC-1")).isTrue();
        assertThat(service.temAcesso("PAC-2")).isFalse();
    }

    private AcessoPaciente salvo() {
        ArgumentCaptor<AcessoPaciente> captor = ArgumentCaptor.forClass(AcessoPaciente.class);
        verify(acessos).save(captor.capture());
        return captor.getValue();
    }
}
