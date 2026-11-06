package com.jaofraccaroli.clinica.bdd;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

/**
 * Deixa o relatório HTML do Cucumber mais legível: troca o caminho do arquivo
 * (ex.: "classpath:features/area_do_paciente.feature") pelo nome da funcionalidade
 * (ex.: "Área do paciente").
 *
 * Roda sozinho no fim dos testes, tanto pelo VS Code quanto pelo Maven
 * (registrado em META-INF/services).
 */
public class TitulosDoRelatorioCucumber implements TestExecutionListener {

    static final Path RELATORIO = Path.of("target/relatorio-cucumber.html");
    static final Path FEATURES = Path.of("src/test/resources/features");
    private static final String PREFIXO = "Funcionalidade:";

    @Override
    public void testPlanExecutionFinished(TestPlan plano) {
        if (!Files.isRegularFile(RELATORIO) || !Files.isDirectory(FEATURES)) {
            return;
        }
        try {
            String html = Files.readString(RELATORIO, StandardCharsets.UTF_8);
            String renomeado = renomear(html, titulos(FEATURES));
            if (!renomeado.equals(html)) {
                Files.writeString(RELATORIO, renomeado, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ajustar o relatório do Cucumber", e);
        }
    }

    /** Nome do arquivo -> nome da funcionalidade, para cada .feature da pasta. */
    static Map<String, String> titulos(Path pasta) throws IOException {
        Map<String, String> titulos = new LinkedHashMap<>();
        try (Stream<Path> arquivos = Files.list(pasta)) {
            for (Path arquivo : arquivos.filter(p -> p.toString().endsWith(".feature")).sorted().toList()) {
                nomeDaFuncionalidade(Files.readString(arquivo, StandardCharsets.UTF_8))
                        .ifPresent(nome -> titulos.put(arquivo.getFileName().toString(), nome));
            }
        }
        return titulos;
    }

    /** Lê o texto depois de "Funcionalidade:" (a primeira linha que começa assim). */
    static Optional<String> nomeDaFuncionalidade(String conteudo) {
        return conteudo.lines()
                .map(String::trim)
                .filter(linha -> linha.startsWith(PREFIXO))
                .map(linha -> linha.substring(PREFIXO.length()).trim())
                .filter(nome -> !nome.isEmpty())
                .findFirst();
    }

    /** Troca o caminho de cada arquivo pelo nome, nas duas formas em que ele aparece no HTML. */
    static String renomear(String html, Map<String, String> titulos) {
        String resultado = html;
        for (Map.Entry<String, String> titulo : titulos.entrySet()) {
            String nome = paraJson(titulo.getValue());
            resultado = resultado
                    .replace("classpath:features\\/" + titulo.getKey(), nome)
                    .replace("classpath:features/" + titulo.getKey(), nome);
        }
        return resultado;
    }

    private static String paraJson(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
