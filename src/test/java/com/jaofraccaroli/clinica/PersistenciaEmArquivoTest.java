package com.jaofraccaroli.clinica;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import com.jaofraccaroli.clinica.clinico.Paciente;
import com.jaofraccaroli.clinica.clinico.PacienteService;
import com.jaofraccaroli.clinica.financeiro.CobrancaService;

/**
 * Os dados ficam salvos em arquivo: desligar e ligar a aplicação não apaga nada,
 * e os pacientes fictícios não são criados de novo.
 */
class PersistenciaEmArquivoTest {

    @TempDir
    Path pasta;

    @Test
    void dadosContinuamDepoisDeReiniciarAAplicacao() {
        String banco = pasta.resolve("clinica").toAbsolutePath().toString().replace('\\', '/');

        String codigoNovo;
        try (ConfigurableApplicationContext app = iniciar(banco)) {
            PacienteService pacientes = app.getBean(PacienteService.class);
            assertThat(pacientes.listar()).hasSize(2);
            codigoNovo = pacientes.cadastrar(paciente("Paciente Cadastrado Antes De Reiniciar")).getCodigo();
        }

        assertThat(pasta.resolve("clinica.mv.db")).exists();

        try (ConfigurableApplicationContext app = iniciar(banco)) {
            PacienteService pacientes = app.getBean(PacienteService.class);
            assertThat(pacientes.listar()).hasSize(3)
                    .extracting(Paciente::getCodigo).contains(codigoNovo);
            assertThat(app.getBean(CobrancaService.class).listar()).hasSize(1);
        }
    }

    @Test
    void configuracaoPadraoUsaBancoEmArquivo() throws IOException {
        String propriedades = Files.readString(
                Path.of("src/main/resources/application.properties"), StandardCharsets.UTF_8);

        assertThat(propriedades)
                .contains("spring.datasource.url=jdbc:h2:file:./dados/clinica")
                .contains("spring.jpa.hibernate.ddl-auto=update")
                .doesNotContain("jdbc:h2:mem:");
    }

    @Test
    void pastaDoBancoNaoVaiParaOGitHub() throws IOException {
        assertThat(Files.readAllLines(Path.of(".gitignore"))).contains("/dados/");
    }

    private static ConfigurableApplicationContext iniciar(String banco) {
        return new SpringApplicationBuilder(ClinicaApplication.class).run(
                "--spring.datasource.url=jdbc:h2:file:" + banco,
                "--spring.jpa.hibernate.ddl-auto=update",
                "--clinica.dados-exemplo=true",
                "--server.port=0");
    }

    private static Paciente paciente(String nome) {
        Paciente p = new Paciente();
        p.setNome(nome);
        p.setNomePagador("Pagador Ficticio");
        p.setValorSessao(new BigDecimal("150.00"));
        return p;
    }
}
