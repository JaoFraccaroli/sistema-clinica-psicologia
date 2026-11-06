package com.jaofraccaroli.clinica.bdd;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.jaofraccaroli.clinica.clinico.Paciente;
import com.jaofraccaroli.clinica.clinico.PacienteService;
import com.jaofraccaroli.clinica.clinico.StatusSessao;
import com.jaofraccaroli.clinica.financeiro.Cobranca;
import com.jaofraccaroli.clinica.financeiro.CobrancaService;
import com.jaofraccaroli.clinica.financeiro.StatusCobranca;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Passos dos cenários em português. As anotações Given/When/Then servem para
 * qualquer idioma: o que vale é o texto do passo. Uma instância nova por cenário.
 */
public class PassosDaClinica {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PacienteService pacienteService;

    @Autowired
    private CobrancaService cobrancaService;

    /** Usuário simulado (sem passar pela tela de login). */
    private RequestPostProcessor usuario;
    /** Sessão real, criada quando o cenário faz login pela tela. */
    private MockHttpSession sessao;

    private ResultActions resultado;
    private Paciente paciente;
    private Cobranca cobranca;
    private final Map<String, Paciente> pacientes = new HashMap<>();
    private final Map<String, String> senhas = new HashMap<>();
    private final Map<String, String> senhasAntigas = new HashMap<>();

    // ---------- Quem está usando ----------

    @Given("que estou logado como {string}")
    public void queEstouLogadoComo(String perfil) {
        usuario = user(perfil).roles(perfil.toUpperCase());
        sessao = null;
    }

    @Given("que estou logado como o paciente {string}")
    public void queEstouLogadoComoOPaciente(String nome) {
        usuario = user(paciente(nome).getCodigo()).roles("PACIENTE");
        sessao = null;
    }

    @Given("que não estou logado")
    public void queNaoEstouLogado() {
        usuario = null;
        sessao = null;
    }

    // ---------- Dados do cenário ----------

    @Given("que existe um paciente com a anotação de sessão {string}")
    public void queExisteUmPacienteComAnotacao(String anotacao) {
        queExisteOPaciente("Paciente do cenário BDD", anotacao);
    }

    @Given("que existe o paciente {string} com a anotação de sessão {string}")
    public void queExisteOPaciente(String nome, String anotacao) {
        Paciente novo = new Paciente();
        novo.setNome(nome);
        novo.setNomePagador("Responsável financeiro");
        novo.setValorSessao(new BigDecimal("150.00"));
        paciente = pacienteService.cadastrar(novo);
        pacientes.put(nome, paciente);
        pacienteService.registrarSessao(paciente.getId(), LocalDateTime.now().minusDays(1),
                StatusSessao.REALIZADA, anotacao);
    }

    @Given("(que )o paciente {string} tem uma sessão agendada daqui a {int} dias")
    public void temSessaoAgendada(String nome, int dias) {
        pacienteService.registrarSessao(paciente(nome).getId(), LocalDateTime.now().plusDays(dias),
                StatusSessao.AGENDADA, null);
    }

    @Given("existe uma cobrança para esse paciente")
    public void existeUmaCobrancaParaEssePaciente() {
        cobranca = cobrancaService.criar(paciente.getCodigo(), new BigDecimal("150.00"), LocalDate.now().plusDays(5));
    }

    @Given("(que )o paciente {string} tem uma cobrança de {string} vencendo em {int} dia(s)")
    public void temUmaCobranca(String nome, String valor, int dias) {
        cobranca = cobrancaService.criar(paciente(nome).getCodigo(), new BigDecimal(valor), LocalDate.now().plusDays(dias));
    }

    // ---------- Acesso do paciente ----------

    @Given("(que )a psicóloga criou o acesso do paciente {string}")
    public void aPsicologaCriouOAcesso(String nome) throws Exception {
        MvcResult resposta = mockMvc.perform(post("/clinico/pacientes/" + paciente(nome).getId() + "/acesso")
                        .with(user("psicologa").roles("PSICOLOGA")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String senha = (String) resposta.getFlashMap().get("senhaGerada");
        assertThat(senha).as("senha mostrada para a psicóloga").isNotBlank();
        senhas.put(nome, senha);
    }

    @Given("(que )a psicóloga gerou uma nova senha para o paciente {string}")
    public void aPsicologaGerouNovaSenha(String nome) throws Exception {
        senhasAntigas.put(nome, senhas.get(nome));
        aPsicologaCriouOAcesso(nome);
    }

    @When("eu tento criar o acesso do paciente {string}")
    public void euTentoCriarOAcesso(String nome) throws Exception {
        resultado = mockMvc.perform(comUsuario(post("/clinico/pacientes/" + paciente(nome).getId() + "/acesso")).with(csrf()));
    }

    // ---------- Login pela tela ----------

    @When("(que )alguém entra/entrou com o usuário {string} e a senha {string}")
    public void alguemEntra(String usuarioDigitado, String senha) throws Exception {
        resultado = mockMvc.perform(formLogin().user(usuarioDigitado).password(senha));
        MvcResult resposta = resultado.andReturn();
        sessao = (MockHttpSession) resposta.getRequest().getSession(false);
        usuario = null;
    }

    @When("o paciente {string} entra com a senha recebida")
    public void oPacienteEntraComASenhaRecebida(String nome) throws Exception {
        alguemEntra(paciente(nome).getCodigo(), senhas.get(nome));
    }

    @When("o paciente {string} entra com o código em minúsculas e a senha recebida")
    public void oPacienteEntraEmMinusculas(String nome) throws Exception {
        alguemEntra(paciente(nome).getCodigo().toLowerCase(), senhas.get(nome));
    }

    @When("o paciente {string} entra com a senha antiga")
    public void oPacienteEntraComASenhaAntiga(String nome) throws Exception {
        alguemEntra(paciente(nome).getCodigo(), senhasAntigas.get(nome));
    }

    @When("o paciente {string} entra com a senha {string}")
    public void oPacienteEntraComASenha(String nome, String senha) throws Exception {
        alguemEntra(paciente(nome).getCodigo(), senha);
    }

    @Then("o login deve ser {word}")
    public void oLoginDeveSer(String esperado) throws Exception {
        switch (esperado) {
            case "aceito" -> resultado.andExpect(authenticated()).andExpect(redirectedUrl("/"));
            case "recusado" -> resultado.andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error"));
            default -> throw new IllegalArgumentException("Resultado de login desconhecido: " + esperado);
        }
    }

    @Then("a página inicial deve levar para {string}")
    public void aPaginaInicialDeveLevarPara(String destino) throws Exception {
        euAcesso("/");
        resultado.andExpect(redirectedUrl(destino));
    }

    @When("eu saio do sistema")
    public void euSaioDoSistema() throws Exception {
        resultado = mockMvc.perform(comUsuario(post("/logout")).with(csrf()));
    }

    @Then("não consigo mais acessar {string}")
    public void naoConsigoMaisAcessar(String pagina) throws Exception {
        euAcesso(pagina);
        devoSerEnviadoParaOLogin();
    }

    // ---------- Navegação ----------

    @When("eu acesso {string}")
    public void euAcesso(String pagina) throws Exception {
        resultado = mockMvc.perform(comUsuario(get(pagina)));
    }

    @When("eu acesso o prontuário desse paciente")
    public void euAcessoOProntuarioDessePaciente() throws Exception {
        euAcesso("/clinico/pacientes/" + paciente.getId());
    }

    @When("eu acesso o prontuário do paciente {string}")
    public void euAcessoOProntuarioDoPaciente(String nome) throws Exception {
        euAcesso("/clinico/pacientes/" + paciente(nome).getId());
    }

    @When("eu acesso a minha área informando o código do paciente {string}")
    public void euAcessoAMinhaAreaComOutroCodigo(String nome) throws Exception {
        resultado = mockMvc.perform(comUsuario(get("/paciente").param("codigo", paciente(nome).getCodigo())));
    }

    // ---------- Cobranças ----------

    @When("eu crio uma cobrança de {string} para o paciente {string}")
    public void euCrioUmaCobranca(String valor, String nome) throws Exception {
        euCrioUmaCobrancaParaOCodigo(valor, paciente(nome).getCodigo());
    }

    @When("eu crio uma cobrança de {string} para o código {string}")
    public void euCrioUmaCobrancaParaOCodigo(String valor, String codigo) throws Exception {
        resultado = mockMvc.perform(comUsuario(post("/financeiro/cobrancas")).with(csrf())
                .param("codigoPaciente", codigo)
                .param("valor", valor)
                .param("vencimento", LocalDate.now().plusDays(5).toString()));
    }

    @Then("a cobrança deve ser {word}")
    public void aCobrancaDeveSer(String esperado) throws Exception {
        switch (esperado) {
            case "aceita" -> resultado.andExpect(redirectedUrl("/financeiro/cobrancas"))
                    .andExpect(r -> assertThat(r.getFlashMap().get("mensagem")).isEqualTo("Cobrança criada"));
            case "recusada" -> resultado.andExpect(redirectedUrl("/financeiro/cobrancas/nova"))
                    .andExpect(r -> assertThat(r.getFlashMap().get("erro")).isNotNull());
            default -> throw new IllegalArgumentException("Resultado de cobrança desconhecido: " + esperado);
        }
    }

    @When("(que )eu registro o pagamento dessa cobrança")
    public void euRegistroOPagamento() throws Exception {
        euRegistroOPagamentoDaCobrancaNumero(cobranca.getId());
    }

    @When("eu registro o pagamento da cobrança número {long}")
    public void euRegistroOPagamentoDaCobrancaNumero(Long id) throws Exception {
        resultado = mockMvc.perform(comUsuario(post("/financeiro/cobrancas/" + id + "/pagar")).with(csrf()));
    }

    @Then("devo ver a mensagem {string}")
    public void devoVerAMensagem(String mensagem) throws Exception {
        resultado.andExpect(r -> assertThat(r.getFlashMap().get("mensagem")).isEqualTo(mensagem));
    }

    @Then("a mensagem de erro deve conter {string}")
    public void aMensagemDeErroDeveConter(String trecho) throws Exception {
        resultado.andExpect(r -> assertThat((String) r.getFlashMap().get("erro")).contains(trecho));
    }

    @Then("a cobrança deve estar paga")
    public void aCobrancaDeveEstarPaga() {
        Cobranca atual = cobrancaService.listarDoPaciente(cobranca.getCodigoPaciente()).stream()
                .filter(c -> c.getId().equals(cobranca.getId()))
                .findFirst().orElseThrow();
        assertThat(atual.getStatus()).isEqualTo(StatusCobranca.PAGA);
        assertThat(atual.getDataPagamento()).isEqualTo(LocalDate.now());
    }

    @When("eu tento criar uma cobrança para esse paciente")
    public void euTentoCriarUmaCobranca() throws Exception {
        euCrioUmaCobrancaParaOCodigo("150.00", paciente.getCodigo());
    }

    @When("eu tento registrar o pagamento dessa cobrança")
    public void euTentoRegistrarOPagamento() throws Exception {
        euRegistroOPagamento();
    }

    // ---------- Verificações ----------

    @Then("o acesso deve ser {string}")
    public void oAcessoDeveSer(String esperado) throws Exception {
        switch (esperado) {
            case "permitido" -> resultado.andExpect(status().isOk());
            case "negado" -> resultado.andExpect(status().isForbidden());
            default -> throw new IllegalArgumentException("Resultado desconhecido no cenário: " + esperado);
        }
    }

    @Then("devo ser enviado para a tela de login")
    public void devoSerEnviadoParaOLogin() throws Exception {
        resultado.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Then("devo ser enviado para {string}")
    public void devoSerEnviadoPara(String destino) throws Exception {
        resultado.andExpect(redirectedUrl(destino));
    }

    @Then("a página deve mostrar o código do paciente")
    public void aPaginaDeveMostrarOCodigo() throws Exception {
        resultado.andExpect(content().string(containsString(paciente.getCodigo())));
    }

    @Then("a página deve conter o código do paciente {string}")
    public void aPaginaDeveConterOCodigoDe(String nome) throws Exception {
        resultado.andExpect(content().string(containsString(paciente(nome).getCodigo())));
    }

    @Then("a página não deve conter o código do paciente {string}")
    public void aPaginaNaoDeveConterOCodigoDe(String nome) throws Exception {
        resultado.andExpect(content().string(not(containsString(paciente(nome).getCodigo()))));
    }

    @Then("a página deve conter {string}")
    public void aPaginaDeveConter(String texto) throws Exception {
        resultado.andExpect(content().string(containsString(texto)));
    }

    @Then("a página não deve conter {string}")
    public void aPaginaNaoDeveConter(String texto) throws Exception {
        resultado.andExpect(content().string(not(containsString(texto))));
    }

    // ---------- Apoio ----------

    private MockHttpServletRequestBuilder comUsuario(MockHttpServletRequestBuilder requisicao) {
        if (sessao != null) {
            return requisicao.session(sessao);
        }
        return usuario == null ? requisicao : requisicao.with(usuario);
    }

    private Paciente paciente(String nome) {
        Paciente p = pacientes.get(nome);
        if (p == null) {
            throw new IllegalStateException("O cenário não criou o paciente \"" + nome + "\"");
        }
        return p;
    }
}
