package com.jaofraccaroli.clinica.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.jaofraccaroli.clinica.portal.AcessoPaciente;
import com.jaofraccaroli.clinica.portal.AcessoPacienteRepository;

class UsuariosDaClinicaTest {

    private final PasswordEncoder codificador = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    private final AcessoPacienteRepository acessos = mock(AcessoPacienteRepository.class);
    private UsuariosDaClinica usuarios;

    @BeforeEach
    void criar() {
        lenient().when(acessos.findByCodigoPaciente(anyString())).thenReturn(Optional.empty());
        usuarios = new UsuariosDaClinica("senha-psi", "senha-fin", codificador, acessos);
    }

    @ParameterizedTest(name = "\"{0}\" entra como {1}")
    @CsvSource({
            "psicologa, ROLE_PSICOLOGA, senha-psi",
            "financeiro, ROLE_FINANCEIRO, senha-fin",
            "PSICOLOGA, ROLE_PSICOLOGA, senha-psi",
            "'  financeiro  ', ROLE_FINANCEIRO, senha-fin"
    })
    void encontraAEquipe(String usuario, String perfil, String senha) {
        UserDetails encontrado = usuarios.loadUserByUsername(usuario);

        assertThat(encontrado.getUsername()).isEqualTo(usuario.trim().toLowerCase());
        assertThat(encontrado.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly(perfil);
        assertThat(codificador.matches(senha, encontrado.getPassword())).isTrue();
    }

    @Test
    void senhaDaEquipeNaoFicaEmTexto() {
        assertThat(usuarios.loadUserByUsername("psicologa").getPassword())
                .startsWith("{bcrypt}").isNotEqualTo("senha-psi");
    }

    @Test
    void cadaLoginRecebeUmObjetoNovo() {
        // O Spring apaga a senha do objeto depois do login; o próximo login não pode ser afetado
        assertThat(usuarios.loadUserByUsername("psicologa"))
                .isNotSameAs(usuarios.loadUserByUsername("psicologa"));
    }

    @ParameterizedTest(name = "código \"{0}\" entra como paciente")
    @ValueSource(strings = {"PAC-1A2B3C4D", "pac-1a2b3c4d", " PAC-1A2B3C4D "})
    void encontraPacientePeloCodigo(String digitado) {
        AcessoPaciente acesso = new AcessoPaciente();
        acesso.setCodigoPaciente("PAC-1A2B3C4D");
        acesso.setSenhaHash(codificador.encode("SenhaDoPaciente"));
        when(acessos.findByCodigoPaciente("PAC-1A2B3C4D")).thenReturn(Optional.of(acesso));

        UserDetails paciente = usuarios.loadUserByUsername(digitado);

        assertThat(paciente.getUsername()).isEqualTo("PAC-1A2B3C4D");
        assertThat(paciente.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_PACIENTE");
        assertThat(codificador.matches("SenhaDoPaciente", paciente.getPassword())).isTrue();
    }

    @Test
    void usuarioEmBrancoNemConsultaOBanco() {
        assertThatThrownBy(() -> usuarios.loadUserByUsername("   "))
                .isInstanceOf(UsernameNotFoundException.class);
        org.mockito.Mockito.verify(acessos, org.mockito.Mockito.never()).findByCodigoPaciente(anyString());
    }

    @ParameterizedTest(name = "\"{0}\" não existe")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "PAC-SEMACESSO", "admin", "root", "psicologa2", "' OR '1'='1"})
    void usuarioDesconhecidoNaoEntra(String usuario) {
        assertThatThrownBy(() -> usuarios.loadUserByUsername(usuario))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage(UsuariosDaClinica.NAO_ENCONTRADO);
    }
}
