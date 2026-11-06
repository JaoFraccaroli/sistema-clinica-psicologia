package com.jaofraccaroli.clinica.bdd;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.jaofraccaroli.clinica.clinico.Paciente;
import com.jaofraccaroli.clinica.clinico.PacienteService;
import com.jaofraccaroli.clinica.clinico.StatusSessao;
import com.jaofraccaroli.clinica.financeiro.Cobranca;
import com.jaofraccaroli.clinica.financeiro.CobrancaService;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Passos dos cenários em português. As anotações Given/When/Then servem para
 * qualquer idioma: o que vale é o texto do passo.
 */
public class SeparacaoDosDadosSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PacienteService pacienteService;

    @Autowired
    private CobrancaService cobrancaService;

    private RequestPostProcessor usuario;
    private ResultActions resultado;
    private Paciente paciente;
    private Cobranca cobranca;

    @Given("que estou logado como {string}")
    public void queEstouLogadoComo(String perfil) {
        usuario = user(perfil).roles(perfil.toUpperCase());
    }

    @Given("que não estou logado")
    public void queNaoEstouLogado() {
        usuario = null;
    }

    @Given("que existe um paciente com a anotação de sessão {string}")
    public void queExisteUmPacienteComAnotacao(String anotacao) {
        Paciente novo = new Paciente();
        novo.setNome("Paciente do cenário BDD");
        novo.setNomePagador("Pagador do cenário BDD");
        novo.setValorSessao(new BigDecimal("150.00"));
        paciente = pacienteService.cadastrar(novo);
        pacienteService.registrarSessao(paciente.getId(), LocalDateTime.now(), StatusSessao.REALIZADA, anotacao);
    }

    @Given("existe uma cobrança para esse paciente")
    public void existeUmaCobrancaParaEssePaciente() {
        cobranca = cobrancaService.criar(paciente.getCodigo(), new BigDecimal("150.00"), LocalDate.now().plusDays(5));
    }

    @When("eu acesso {string}")
    public void euAcesso(String pagina) throws Exception {
        MockHttpServletRequestBuilder requisicao = get(pagina);
        if (usuario != null) {
            requisicao = requisicao.with(usuario);
        }
        resultado = mockMvc.perform(requisicao);
    }

    @Then("o acesso deve ser {string}")
    public void oAcessoDeveSer(String esperado) throws Exception {
        if (esperado.equals("permitido")) {
            resultado.andExpect(status().isOk());
        } else if (esperado.equals("negado")) {
            resultado.andExpect(status().isForbidden());
        } else {
            throw new IllegalArgumentException("Resultado desconhecido no cenário: " + esperado);
        }
    }

    @Then("devo ser enviado para a tela de login")
    public void devoSerEnviadoParaOLogin() throws Exception {
        resultado.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Then("a página deve mostrar o código do paciente")
    public void aPaginaDeveMostrarOCodigo() throws Exception {
        resultado.andExpect(content().string(containsString(paciente.getCodigo())));
    }

    @Then("a página não deve conter {string}")
    public void aPaginaNaoDeveConter(String texto) throws Exception {
        resultado.andExpect(content().string(not(containsString(texto))));
    }

    @When("eu tento criar uma cobrança para esse paciente")
    public void euTentoCriarUmaCobranca() throws Exception {
        resultado = mockMvc.perform(post("/financeiro/cobrancas").with(usuario).with(csrf())
                .param("codigoPaciente", paciente.getCodigo())
                .param("valor", "150.00")
                .param("vencimento", LocalDate.now().plusDays(5).toString()));
    }

    @When("eu tento registrar o pagamento dessa cobrança")
    public void euTentoRegistrarOPagamento() throws Exception {
        resultado = mockMvc.perform(post("/financeiro/cobrancas/" + cobranca.getId() + "/pagar")
                .with(usuario).with(csrf()));
    }

    @Then("a página deve conter {string}")
    public void aPaginaDeveConter(String texto) throws Exception {
        resultado.andExpect(content().string(containsString(texto)));
    }
}
