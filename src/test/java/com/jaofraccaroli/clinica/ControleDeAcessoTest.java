package com.jaofraccaroli.clinica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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

    // ---------- Psicóloga: financeiro somente leitura ----------

    @Test
    void psicologaVeCobrancasEmModoSomenteLeitura() throws Exception {
        mockMvc.perform(get("/financeiro/cobrancas").with(PSICOLOGA))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Modo somente leitura")))
                .andExpect(content().string(not(containsString("Nova cobrança"))))
                .andExpect(content().string(not(containsString("Registrar pagamento"))));
    }

    @Test
    void psicologaNaoAbreFormularioDeNovaCobranca() throws Exception {
        mockMvc.perform(get("/financeiro/cobrancas/nova").with(PSICOLOGA))
                .andExpect(status().isForbidden());
    }

    @Test
    void psicologaNaoCriaCobrancaNemEnviandoOFormularioDireto() throws Exception {
        Paciente paciente = novoPaciente("Paciente Cobrança Psicóloga");
        mockMvc.perform(post("/financeiro/cobrancas").with(PSICOLOGA).with(csrf())
                        .param("codigoPaciente", paciente.getCodigo())
                        .param("valor", "150.00")
                        .param("vencimento", "2026-12-01"))
                .andExpect(status().isForbidden());
    }

    @Test
    void psicologaNaoRegistraPagamento() throws Exception {
        Paciente paciente = novoPaciente("Paciente Pagamento Psicóloga");
        Long id = cobrancaService.criar(paciente.getCodigo(), new BigDecimal("150.00"), LocalDate.now()).getId();
        mockMvc.perform(post("/financeiro/cobrancas/" + id + "/pagar").with(PSICOLOGA).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void financeiroVeBotoesECriaCobranca() throws Exception {
        mockMvc.perform(get("/financeiro/cobrancas").with(FINANCEIRO))
                .andExpect(content().string(containsString("Nova cobrança")))
                .andExpect(content().string(not(containsString("Modo somente leitura"))));

        Paciente paciente = novoPaciente("Paciente Cobrança Financeiro");
        mockMvc.perform(post("/financeiro/cobrancas").with(FINANCEIRO).with(csrf())
                        .param("codigoPaciente", paciente.getCodigo())
                        .param("valor", "150.00")
                        .param("vencimento", "2026-12-01"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/financeiro/cobrancas"));
    }

    @Test
    void financeiroRegistraPagamento() throws Exception {
        Paciente paciente = novoPaciente("Paciente Pagamento Financeiro");
        Long id = cobrancaService.criar(paciente.getCodigo(), new BigDecimal("150.00"), LocalDate.now()).getId();
        mockMvc.perform(post("/financeiro/cobrancas/" + id + "/pagar").with(FINANCEIRO).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/financeiro/cobrancas"));
    }

    // ---------- Validação do CPF no cadastro ----------

    @Test
    void cadastroComCpfInvalidoVoltaParaOFormulario() throws Exception {
        mockMvc.perform(post("/clinico/pacientes")
                        .with(PSICOLOGA).with(csrf())
                        .param("nome", "Paciente CPF")
                        .param("nomePagador", "Responsável")
                        .param("cpfPagador", "111.111.111-11")
                        .param("valorSessao", "160.00"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CPF inválido")));
    }

    @Test
    void cpfSemPontuacaoEhGuardadoFormatado() {
        Paciente p = new Paciente();
        p.setNome("Paciente Formatação");
        p.setNomePagador("Responsável");
        p.setCpfPagador("11144477735");
        p.setValorSessao(new BigDecimal("150.00"));

        assertThat(pacienteService.cadastrar(p).getCpfPagador()).isEqualTo("111.444.777-35");
    }

    @Test
    void cpfEmBrancoViraNulo() {
        Paciente p = new Paciente();
        p.setNome("Paciente Sem CPF");
        p.setNomePagador("Responsável");
        p.setCpfPagador("   ");
        p.setValorSessao(new BigDecimal("150.00"));

        assertThat(pacienteService.cadastrar(p).getCpfPagador()).isNull();
    }

    @Test
    void telaDoFinanceiroMostraASituacaoDaCobranca() throws Exception {
        Paciente paciente = novoPaciente("Paciente Atrasado");
        cobrancaService.criar(paciente.getCodigo(), new BigDecimal("150.00"), LocalDate.now().minusDays(3));

        mockMvc.perform(get("/financeiro/cobrancas").with(FINANCEIRO))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Atrasada")))
                .andExpect(content().string(containsString("3 dias")));
    }

    // ---------- Exportar planilha ----------

    @Test
    void financeiroBaixaAPlanilhaDeCobrancas() throws Exception {
        Paciente paciente = novoPaciente("Paciente Planilha");
        cobrancaService.criar(paciente.getCodigo(), new BigDecimal("150.00"), LocalDate.now().plusDays(5));

        String csv = mockMvc.perform(get("/financeiro/cobrancas/exportar").with(FINANCEIRO))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition",
                        containsString("attachment; filename=\"cobrancas-" + LocalDate.now() + ".csv\"")))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(csv).startsWith("\uFEFFCódigo do paciente;Pagador;CPF do pagador;");
        assertThat(csv).contains(paciente.getCodigo() + ";Pagador de 17;529.982.247-25;150,00;");
    }

    @Test
    void psicologaTambemBaixaAPlanilha() throws Exception {
        mockMvc.perform(get("/financeiro/cobrancas/exportar").with(PSICOLOGA))
                .andExpect(status().isOk());
    }

    @Test
    void visitanteSemLoginNaoBaixaAPlanilha() throws Exception {
        mockMvc.perform(get("/financeiro/cobrancas/exportar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void planilhaNaoLevaDadosClinicos() throws Exception {
        Paciente paciente = novoPaciente("Nome Clinico Secreto");
        pacienteService.registrarSessao(paciente.getId(), LocalDateTime.now().minusDays(1),
                StatusSessao.REALIZADA, "ANOTACAO-SIGILOSA-DA-SESSAO");
        cobrancaService.criar(paciente.getCodigo(), new BigDecimal("150.00"), LocalDate.now());

        String csv = mockMvc.perform(get("/financeiro/cobrancas/exportar").with(FINANCEIRO))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(csv).contains(paciente.getCodigo())
                .doesNotContain("Nome Clinico Secreto")
                .doesNotContain("ANOTACAO-SIGILOSA-DA-SESSAO");
    }

    @Test
    void botaoDeExportarApareceParaOsDoisPerfis() throws Exception {
        mockMvc.perform(get("/financeiro/cobrancas").with(FINANCEIRO))
                .andExpect(content().string(containsString("Exportar planilha")));
        mockMvc.perform(get("/financeiro/cobrancas").with(PSICOLOGA))
                .andExpect(content().string(containsString("Exportar planilha")));
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
        p.setCpfPagador("529.982.247-25");
        p.setValorSessao(new BigDecimal("150.00"));
        return pacienteService.cadastrar(p);
    }
}
