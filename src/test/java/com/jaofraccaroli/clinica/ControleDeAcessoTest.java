package com.jaofraccaroli.clinica;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.jaofraccaroli.clinica.clinico.Paciente;
import com.jaofraccaroli.clinica.clinico.PacienteService;
import com.jaofraccaroli.clinica.clinico.StatusSessao;
import com.jaofraccaroli.clinica.financeiro.CobrancaService;

@SpringBootTest
@AutoConfigureMockMvc
class ControleDeAcessoTest {

    private static final RequestPostProcessor PSICOLOGA = user("psicologa").roles("PSICOLOGA");
    private static final RequestPostProcessor FINANCEIRO = user("financeiro").roles("FINANCEIRO");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PacienteService pacienteService;

    @Autowired
    private CobrancaService cobrancaService;

    @Test
    void visitanteSemLoginEhEnviadoParaOLogin() throws Exception {
        mockMvc.perform(get("/clinico/pacientes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void psicologaAcessaPacientes() throws Exception {
        mockMvc.perform(get("/clinico/pacientes").with(PSICOLOGA))
                .andExpect(status().isOk());
    }

    @Test
    void psicologaTambemAcessaOFinanceiro() throws Exception {
        mockMvc.perform(get("/financeiro/cobrancas").with(PSICOLOGA))
                .andExpect(status().isOk());
    }

    @Test
    void financeiroAcessaCobrancas() throws Exception {
        mockMvc.perform(get("/financeiro/cobrancas").with(FINANCEIRO))
                .andExpect(status().isOk());
    }

    @Test
    void financeiroNaoAcessaListaDePacientes() throws Exception {
        mockMvc.perform(get("/clinico/pacientes").with(FINANCEIRO))
                .andExpect(status().isForbidden());
    }

    @Test
    void financeiroNaoAcessaProntuario() throws Exception {
        Paciente paciente = novoPaciente("Paciente Prontuário");
        mockMvc.perform(get("/clinico/pacientes/" + paciente.getId()).with(FINANCEIRO))
                .andExpect(status().isForbidden());
    }

    @Test
    void financeiroNaoConsegueRegistrarSessao() throws Exception {
        Paciente paciente = novoPaciente("Paciente Sessão");
        mockMvc.perform(post("/clinico/pacientes/" + paciente.getId() + "/sessoes")
                        .with(FINANCEIRO).with(csrf())
                        .param("dataHora", "2026-11-05T14:30")
                        .param("anotacoes", "tentativa indevida"))
                .andExpect(status().isForbidden());
    }

    @Test
    void telaDoFinanceiroNaoMostraDadosClinicos() throws Exception {
        Paciente paciente = novoPaciente("Nome Clinico Sigiloso");
        pacienteService.registrarSessao(paciente.getId(), LocalDateTime.now(),
                StatusSessao.REALIZADA, "ANOTACAO-SIGILOSA-123");
        cobrancaService.criar(paciente.getCodigo(), new BigDecimal("150.00"), LocalDate.now());

        mockMvc.perform(get("/financeiro/cobrancas").with(FINANCEIRO))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(paciente.getCodigo())))
                .andExpect(content().string(not(containsString("ANOTACAO-SIGILOSA-123"))))
                .andExpect(content().string(not(containsString("Nome Clinico Sigiloso"))));
    }

    @Test
    void psicologaCadastraPaciente() throws Exception {
        mockMvc.perform(post("/clinico/pacientes")
                        .with(PSICOLOGA).with(csrf())
                        .param("nome", "Paciente Novo")
                        .param("nomePagador", "Responsável Novo")
                        .param("valorSessao", "160.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clinico/pacientes"));
    }

    @Test
    void cadastroSemNomeVoltaParaOFormulario() throws Exception {
        mockMvc.perform(post("/clinico/pacientes")
                        .with(PSICOLOGA).with(csrf())
                        .param("nomePagador", "Responsável")
                        .param("valorSessao", "160.00"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Informe o nome")));
    }

    // ---------- Login e logout ----------

    @Test
    void telaDeLoginAbreEmPortugues() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Entre com o seu usuário")));
    }

    @Test
    void loginComSenhaCertaVaiParaOInicio() throws Exception {
        mockMvc.perform(formLogin().user("psicologa").password("senha-teste-psicologa"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void loginComSenhaErradaMostraErro() throws Exception {
        mockMvc.perform(formLogin().user("psicologa").password("errada"))
                .andExpect(redirectedUrl("/login?error"));
        mockMvc.perform(get("/login").param("error", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Usuário ou senha inválidos")));
    }

    @Test
    void sairLevaParaOLoginComAviso() throws Exception {
        // Defeito encontrado: depois de sair, /login?logout dava 404
        mockMvc.perform(post("/logout").with(PSICOLOGA).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
        mockMvc.perform(get("/login").param("logout", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Você saiu do sistema")));
    }

    private Paciente novoPaciente(String nome) {
        Paciente p = new Paciente();
        p.setNome(nome);
        p.setNomePagador("Pagador de " + nome.length());
        p.setCpfPagador("000.000.000-00");
        p.setValorSessao(new BigDecimal("150.00"));
        return pacienteService.cadastrar(p);
    }
}
