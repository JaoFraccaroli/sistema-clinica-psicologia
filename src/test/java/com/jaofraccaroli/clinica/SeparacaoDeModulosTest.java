package com.jaofraccaroli.clinica;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.jaofraccaroli.clinica.financeiro.DadosFaturamento;

/**
 * Garante a regra de arquitetura: o módulo financeiro não pode depender
 * de nada da área clínica, e só recebe os campos de faturamento.
 */
class SeparacaoDeModulosTest {

    private static final Path FINANCEIRO = Path.of("src/main/java/com/jaofraccaroli/clinica/financeiro");

    @Test
    void financeiroNaoImportaNadaDaAreaClinica() throws IOException {
        try (Stream<Path> arquivos = Files.walk(FINANCEIRO)) {
            List<Path> violacoes = arquivos
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(SeparacaoDeModulosTest::citaAreaClinica)
                    .toList();
            assertThat(violacoes).as("Arquivos do financeiro que citam a área clínica").isEmpty();
        }
    }

    @Test
    void areaDoPacienteNuncaLeAsAnotacoesDasSessoes() throws IOException {
        try (Stream<Path> arquivos = Files.walk(Path.of("src/main/java/com/jaofraccaroli/clinica/portal"))) {
            List<Path> violacoes = arquivos
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> conteudo(p).contains("getAnotacoes"))
                    .toList();
            assertThat(violacoes).as("Arquivos da área do paciente que leem anotações clínicas").isEmpty();
        }
    }

    @Test
    void telaDoPacienteNaoUsaAnotacoes() throws IOException {
        assertThat(Files.readString(Path.of("src/main/resources/templates/paciente/inicio.html")))
                .doesNotContain("anotac");
    }

    @Test
    void dadosDeFaturamentoTemApenasCamposPermitidos() {
        List<String> campos = Arrays.stream(DadosFaturamento.class.getRecordComponents())
                .map(c -> c.getName())
                .toList();
        assertThat(campos).containsExactly("codigoPaciente", "nomePagador", "cpfPagador", "valorSessao");
    }

    private static String conteudo(Path arquivo) {
        try {
            return Files.readString(arquivo);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean citaAreaClinica(Path arquivo) {
        try {
            return Files.readString(arquivo).contains("clinica.clinico");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
