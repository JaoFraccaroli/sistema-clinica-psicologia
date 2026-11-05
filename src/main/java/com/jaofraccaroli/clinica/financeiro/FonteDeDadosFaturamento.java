package com.jaofraccaroli.clinica.financeiro;

import java.util.List;
import java.util.Optional;

/**
 * Porta de entrada do financeiro para os dados de faturamento.
 * O financeiro depende só desta interface; quem a implementa é a área clínica,
 * que decide o que pode sair de lá.
 */
public interface FonteDeDadosFaturamento {

    List<DadosFaturamento> listarTodos();

    Optional<DadosFaturamento> buscarPorCodigo(String codigoPaciente);
}
