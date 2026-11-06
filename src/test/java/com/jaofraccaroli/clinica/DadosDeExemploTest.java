package com.jaofraccaroli.clinica;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.jaofraccaroli.clinica.clinico.PacienteService;
import com.jaofraccaroli.clinica.financeiro.CobrancaService;
import com.jaofraccaroli.clinica.portal.AcessoPacienteService;

/** Garante que a aplicação sobe como na demonstração, com os pacientes fictícios. */
@SpringBootTest(properties = {
        "clinica.dados-exemplo=true",
        "spring.datasource.url=jdbc:h2:mem:clinica_demo;DB_CLOSE_DELAY=-1"
})
class DadosDeExemploTest {

    @Autowired
    private PacienteService pacienteService;

    @Autowired
    private CobrancaService cobrancaService;

    @Autowired
    private AcessoPacienteService acessoPaciente;

    @Test
    void aplicacaoSobeComPacientesFicticios() {
        assertThat(pacienteService.listar()).hasSize(2);
        assertThat(cobrancaService.listar()).hasSize(1);
    }

    @Test
    void pacientesDeExemploTemCodigosFixos() {
        assertThat(pacienteService.listar()).extracting(p -> p.getCodigo())
                .containsExactlyInAnyOrder("PAC-DEMO0001", "PAC-DEMO0002");
    }

    @Test
    void soAAnaTemAcessoDePaciente() {
        assertThat(acessoPaciente.temAcesso("PAC-DEMO0001")).isTrue();
        assertThat(acessoPaciente.temAcesso("PAC-DEMO0002")).isFalse();
    }

    @Test
    void anaTemUmaSessaoPassadaEUmaFutura() {
        Long id = pacienteService.buscarPorCodigo("PAC-DEMO0001").getId();
        assertThat(pacienteService.sessoesDo(id)).hasSize(2);
    }
}
