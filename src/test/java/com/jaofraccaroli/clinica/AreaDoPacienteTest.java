package com.jaofraccaroli.clinica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.jaofraccaroli.clinica.clinico.Paciente;
import com.jaofraccaroli.clinica.clinico.PacienteService;
import com.jaofraccaroli.clinica.clinico.StatusSessao;
import com.jaofraccaroli.clinica.financeiro.CobrancaService;
import com.jaofraccaroli.clinica.portal.AcessoPacienteRepository;

/** Login do paciente e o que ele pode (e não pode) ver. */
@SpringBootTest
@AutoConfigureMockMvc
class AreaDoPacienteTest {

    private static final RequestPostProcessor PSICOLOGA = user("psicologa").roles("PSICOLOGA");
    private static final RequestPostProcessor FINANCEIRO = user("financeiro").roles("FINANCEIRO");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PacienteService pacienteService;

    @Autowired
    private CobrancaService cobrancaService;

    @Autowired
    private AcessoPacienteRepository acessos;

    private Paciente maria;
    private Paciente joao;

    @BeforeEach
    void pacientes() {
        maria = paciente("Maria Teste Portal");
        pacienteService.registrarSessao(maria.getId(), LocalDateTime.now().minusDays(7),
                StatusSessao.REALIZADA, "ANOTACAO-SIGILOSA-DA-MARIA");
        pacienteService.registrarSessao(maria.getId(), LocalDateTime.now().plusDays(3).withHour(15).withMinute(30),
                StatusSessao.AGENDADA, null);
        cobrancaService.criar(maria.getCodigo(), new BigDecimal("150.00"), LocalDate.now().minusDays(2));

        joao = paciente("Joao Outro Paciente");
        pacienteService.registrarSessao(joao.getId(), LocalDateTime.now().minusDays(1),
                StatusSessao.REALIZADA, "ANOTACAO-SIGILOSA-DO-JOAO");
        cobrancaService.criar(joao.getCodigo(), new BigDecimal("777.77"), LocalDate.now().plusDays(10));
    }

    // ---------- Criar o acesso ----------

    @Test
    void prontuarioMostraQueOPacienteAindaNaoTemAcesso() throws Exception {
        mockMvc.perform(get("/clinico/pacientes/" + maria.getId()).with(PSICOLOGA))
                .andExpect(content().string(containsString("Sem acesso")))
                .andExpect(content().string(containsString("Criar acesso do paciente")));
    }

    @Test
    void psicologaCriaOAcessoEASenhaApareceUmaVez() throws Exception {
        String senha = criarAcesso(maria);

        assertThat(senha).hasSize(10);
        mockMvc.perform(get("/clinico/pacientes/" + maria.getId()).with(PSICOLOGA))
                .andExpect(content().string(containsString("Acesso ativo")))
                .andExpect(content().string(not(containsString(senha))));
    }

    @Test
    void senhaFicaGuardadaSoComoHash() throws Exception {
        String senha = criarAcesso(maria);

        String hash = acessos.findByCodigoPaciente(maria.getCodigo()).orElseThrow().getSenhaHash();
        assertThat(hash).startsWith("{bcrypt}").doesNotContain(senha);
    }

    @Test
    void financeiroNaoCriaAcessoDePaciente() throws Exception {
        mockMvc.perform(post("/clinico/pacientes/" + maria.getId() + "/acesso").with(FINANCEIRO).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(acessos.findByCodigoPaciente(maria.getCodigo())).isEmpty();
    }

    @Test
    void criarAcessoSemTokenCsrfEhBloqueado() throws Exception {
        mockMvc.perform(post("/clinico/pacientes/" + maria.getId() + "/acesso").with(PSICOLOGA))
                .andExpect(status().isForbidden());
    }

    // ---------- Login ----------

    @Test
    void pacienteEntraComCodigoESenhaEVaiParaASuaArea() throws Exception {
        String senha = criarAcesso(maria);

        mockMvc.perform(formLogin().user(maria.getCodigo()).password(senha))
                .andExpect(authenticated().withUsername(maria.getCodigo()).withRoles("PACIENTE"))
                .andExpect(redirectedUrl("/"));
        mockMvc.perform(get("/").with(user(maria.getCodigo()).roles("PACIENTE")))
                .andExpect(redirectedUrl("/paciente"));
    }

    @Test
    void codigoEmMinusculasTambemFunciona() throws Exception {
        String senha = criarAcesso(maria);

        mockMvc.perform(formLogin().user(maria.getCodigo().toLowerCase()).password(senha))
                .andExpect(authenticated().withUsername(maria.getCodigo()));
    }

    @Test
    void senhaErradaNaoEntra() throws Exception {
        criarAcesso(maria);

        mockMvc.perform(formLogin().user(maria.getCodigo()).password("SenhaErrada99"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void pacienteSemAcessoNaoEntra() throws Exception {
        mockMvc.perform(formLogin().user(joao.getCodigo()).password("qualquercoisa"))
                .andExpect(unauthenticated());
    }

    @Test
    void senhaDeUmPacienteNaoServeParaOutro() throws Exception {
        String senhaMaria = criarAcesso(maria);
        criarAcesso(joao);

        mockMvc.perform(formLogin().user(joao.getCodigo()).password(senhaMaria))
                .andExpect(unauthenticated());
    }

    @Test
    void gerarNovaSenhaInvalidaAAntiga() throws Exception {
        String antiga = criarAcesso(maria);
        String nova = criarAcesso(maria);

        assertThat(nova).isNotEqualTo(antiga);
        mockMvc.perform(formLogin().user(maria.getCodigo()).password(antiga)).andExpect(unauthenticated());
        mockMvc.perform(formLogin().user(maria.getCodigo()).password(nova)).andExpect(authenticated());
    }

    @Test
    void senhaDaPsicologaNaoServeComCodigoDePaciente() throws Exception {
        criarAcesso(maria);

        mockMvc.perform(formLogin().user(maria.getCodigo()).password("senha-teste-psicologa"))
                .andExpect(unauthenticated());
    }

    // ---------- O que o paciente vê ----------

    @Test
    void pacienteVeSuasSessoesECobrancas() throws Exception {
        mockMvc.perform(get("/paciente").with(comoPaciente(maria)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Área do paciente")))
                .andExpect(content().string(containsString("Maria Teste Portal")))
                .andExpect(content().string(containsString(maria.getCodigo())))
                .andExpect(content().string(containsString("Agendada")))
                .andExpect(content().string(containsString("Realizada")))
                .andExpect(content().string(containsString("R$ 150,00")))
                .andExpect(content().string(containsString("Atrasada")));
    }

    @Test
    void pacienteNaoVeAsAnotacoesDaPropriaSessao() throws Exception {
        mockMvc.perform(get("/paciente").with(comoPaciente(maria)))
                .andExpect(content().string(not(containsString("ANOTACAO-SIGILOSA-DA-MARIA"))));
    }

    @Test
    void pacienteNaoVeNadaDeOutroPaciente() throws Exception {
        mockMvc.perform(get("/paciente").with(comoPaciente(maria)))
                .andExpect(content().string(not(containsString("Joao Outro Paciente"))))
                .andExpect(content().string(not(containsString(joao.getCodigo()))))
                .andExpect(content().string(not(containsString("777,77"))))
                .andExpect(content().string(not(containsString("ANOTACAO-SIGILOSA-DO-JOAO"))));
    }

    @Test
    void trocarOCodigoNaUrlNaoMostraOutroPaciente() throws Exception {
        // Tentativa de ver os dados de outro paciente mudando parâmetros (IDOR)
        mockMvc.perform(get("/paciente").param("codigo", joao.getCodigo()).with(comoPaciente(maria)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Maria Teste Portal")))
                .andExpect(content().string(not(containsString("Joao Outro Paciente"))));
        mockMvc.perform(get("/paciente/" + joao.getCodigo()).with(comoPaciente(maria)))
                .andExpect(status().isNotFound());
    }

    @Test
    void pacienteSemSessoesNemCobrancasVeAvisos() throws Exception {
        Paciente novo = paciente("Paciente Novo Sem Nada");

        mockMvc.perform(get("/paciente").with(comoPaciente(novo)))
                .andExpect(content().string(containsString("Nenhuma sessão agendada")))
                .andExpect(content().string(containsString("Nenhuma cobrança")))
                .andExpect(content().string(containsString("R$ 0,00")));
    }

    // ---------- O que o paciente NÃO acessa ----------

    @ParameterizedTest(name = "paciente não abre {0}")
    @ValueSource(strings = {
            "/clinico/pacientes",
            "/clinico/pacientes/novo",
            "/financeiro/cobrancas",
            "/financeiro/cobrancas/nova",
            "/financeiro/cobrancas/exportar"
    })
    void pacienteNaoAcessaAreasDaEquipe(String pagina) throws Exception {
        mockMvc.perform(get(pagina).with(comoPaciente(maria)))
                .andExpect(status().isForbidden());
    }

    @Test
    void pacienteNaoAbreProntuarioDeNinguem() throws Exception {
        mockMvc.perform(get("/clinico/pacientes/" + maria.getId()).with(comoPaciente(maria)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/clinico/pacientes/" + joao.getId()).with(comoPaciente(maria)))
                .andExpect(status().isForbidden());
    }

    @Test
    void pacienteNaoCriaAcessoNemSessao() throws Exception {
        mockMvc.perform(post("/clinico/pacientes/" + joao.getId() + "/acesso").with(comoPaciente(maria)).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/clinico/pacientes/" + maria.getId() + "/sessoes").with(comoPaciente(maria)).with(csrf())
                        .param("dataHora", "2026-12-01T10:00"))
                .andExpect(status().isForbidden());
    }

    @Test
    void pacienteNaoRegistraPagamentoDaPropriaCobranca() throws Exception {
        Long id = cobrancaService.listarDoPaciente(maria.getCodigo()).get(0).getId();

        mockMvc.perform(post("/financeiro/cobrancas/" + id + "/pagar").with(comoPaciente(maria)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void equipeNaoEntraNaAreaDoPaciente() throws Exception {
        mockMvc.perform(get("/paciente").with(PSICOLOGA)).andExpect(status().isForbidden());
        mockMvc.perform(get("/paciente").with(FINANCEIRO)).andExpect(status().isForbidden());
    }

    @Test
    void visitanteSemLoginEhEnviadoParaOLogin() throws Exception {
        mockMvc.perform(get("/paciente"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void telaDeLoginExplicaComoOPacienteEntra() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(content().string(containsString("Usuário ou código do paciente")));
    }

    // ---------- Apoio ----------

    private String criarAcesso(Paciente paciente) throws Exception {
        Object senha = mockMvc.perform(post("/clinico/pacientes/" + paciente.getId() + "/acesso")
                        .with(PSICOLOGA).with(csrf()))
                .andExpect(redirectedUrl("/clinico/pacientes/" + paciente.getId()))
                .andExpect(flash().attributeExists("senhaGerada"))
                .andReturn().getFlashMap().get("senhaGerada");
        return (String) senha;
    }

    private static RequestPostProcessor comoPaciente(Paciente paciente) {
        return user(paciente.getCodigo()).roles("PACIENTE");
    }

    private Paciente paciente(String nome) {
        Paciente p = new Paciente();
        p.setNome(nome);
        p.setNomePagador("Pagador de " + nome);
        p.setValorSessao(new BigDecimal("150.00"));
        return pacienteService.cadastrar(p);
    }
}
