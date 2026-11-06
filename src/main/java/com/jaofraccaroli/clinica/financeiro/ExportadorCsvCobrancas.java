package com.jaofraccaroli.clinica.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Gera a planilha (CSV) das cobranças para abrir no Excel.
 *
 * Usa ponto e vírgula e vírgula decimal, que é o padrão do Excel em português.
 * Só leva colunas da área financeira: nenhum dado clínico sai por aqui.
 */
@Component
public class ExportadorCsvCobrancas {

    /** Marca que faz o Excel reconhecer os acentos (UTF-8). */
    static final String BOM = "﻿";

    static final String CABECALHO =
            "Código do paciente;Pagador;CPF do pagador;Valor;Vencimento;Situação;Data do pagamento";

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String gerar(List<Cobranca> cobrancas, LocalDate hoje) {
        StringBuilder csv = new StringBuilder(BOM).append(CABECALHO).append("\r\n");
        for (Cobranca c : cobrancas) {
            csv.append(String.join(";",
                    celula(c.getCodigoPaciente()),
                    celula(c.getNomePagador()),
                    celula(c.getCpfPagador()),
                    valor(c.getValor()),
                    data(c.getVencimento()),
                    c.situacaoEm(hoje).getDescricao(),
                    data(c.getDataPagamento())))
               .append("\r\n");
        }
        return csv.toString();
    }

    /**
     * Protege a célula: coloca entre aspas quando tem ; " ou quebra de linha, e
     * impede que um texto como "=SOMA(...)" vire fórmula no Excel (injeção de CSV).
     */
    static String celula(String texto) {
        if (texto == null) {
            return "";
        }
        String seguro = texto;
        if (!seguro.isEmpty() && "=+-@\t\r".indexOf(seguro.charAt(0)) >= 0) {
            seguro = "'" + seguro;
        }
        if (seguro.contains(";") || seguro.contains("\"") || seguro.contains("\n") || seguro.contains("\r")) {
            seguro = "\"" + seguro.replace("\"", "\"\"") + "\"";
        }
        return seguro;
    }

    static String valor(BigDecimal valor) {
        return valor.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    static String data(LocalDate data) {
        return data == null ? "" : data.format(DATA);
    }
}
