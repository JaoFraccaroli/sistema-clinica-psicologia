package com.jaofraccaroli.clinica.bdd;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TitulosDoRelatorioCucumberTest {

    @Test
    void leONomeDaFuncionalidade() {
        String feature = "# language: pt\nFuncionalidade: Área do paciente\n  Para acompanhar...\n";

        assertThat(TitulosDoRelatorioCucumber.nomeDaFuncionalidade(feature)).contains("Área do paciente");
    }

    @Test
    void ignoraEspacosEmVolta() {
        assertThat(TitulosDoRelatorioCucumber.nomeDaFuncionalidade("   Funcionalidade:   Login   \n"))
                .contains("Login");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "# language: pt\n", "Funcionalidade:   \n", "Cenário: sem funcionalidade"})
    void semNomeNaoMudaNada(String conteudo) {
        assertThat(TitulosDoRelatorioCucumber.nomeDaFuncionalidade(conteudo)).isEmpty();
    }

    @Test
    void trocaOCaminhoPeloNomeNasDuasFormas() {
        String html = "{\"uri\":\"classpath:features\\/login.feature\"} uri=classpath:features/login.feature";

        String renomeado = TitulosDoRelatorioCucumber.renomear(html, Map.of("login.feature", "Login e saída"));

        assertThat(renomeado).isEqualTo("{\"uri\":\"Login e saída\"} uri=Login e saída");
    }

    @Test
    void naoMexeEmOutrosArquivos() {
        String html = "\"classpath:features\\/cobrancas.feature\"";

        assertThat(TitulosDoRelatorioCucumber.renomear(html, Map.of("login.feature", "Login")))
                .isEqualTo(html);
    }

    @Test
    void nomeComAspasNaoQuebraOJson() {
        String renomeado = TitulosDoRelatorioCucumber.renomear(
                "\"classpath:features\\/a.feature\"", Map.of("a.feature", "Nome \"com\" aspas"));

        assertThat(renomeado).isEqualTo("\"Nome \\\"com\\\" aspas\"");
    }

    @Test
    void leTodosOsArquivosDaPasta(@TempDir Path pasta) throws IOException {
        Files.writeString(pasta.resolve("b.feature"), "Funcionalidade: Bê");
        Files.writeString(pasta.resolve("a.feature"), "Funcionalidade: A");
        Files.writeString(pasta.resolve("sem-nome.feature"), "# vazio");
        Files.writeString(pasta.resolve("leia-me.txt"), "Funcionalidade: não é feature");

        assertThat(TitulosDoRelatorioCucumber.titulos(pasta))
                .containsExactly(Map.entry("a.feature", "A"), Map.entry("b.feature", "Bê"));
    }

    @Test
    void funcionalidadesDoProjetoTemNomesDiferentes() throws IOException {
        Map<String, String> titulos = TitulosDoRelatorioCucumber.titulos(TitulosDoRelatorioCucumber.FEATURES);

        assertThat(titulos).hasSize(4);
        assertThat(titulos.values()).doesNotHaveDuplicates()
                .contains("Área do paciente", "Cobranças");
    }
}
