package com.jaofraccaroli.clinica;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.jaofraccaroli.clinica.clinico.PacienteService;
import com.jaofraccaroli.clinica.financeiro.CobrancaService;

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

    @Test
    void aplicacaoSobeComPacientesFicticios() {
        assertThat(pacienteService.listar()).hasSize(2);
        assertThat(cobrancaService.listar()).hasSize(1);
    }
}
