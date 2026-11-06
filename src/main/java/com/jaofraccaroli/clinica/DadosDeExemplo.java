package com.jaofraccaroli.clinica;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.jaofraccaroli.clinica.clinico.Paciente;
import com.jaofraccaroli.clinica.clinico.PacienteService;
import com.jaofraccaroli.clinica.clinico.StatusSessao;
import com.jaofraccaroli.clinica.financeiro.CobrancaService;

/**
 * Cria pacientes FICTÍCIOS para demonstração. Nenhum dado real.
 * Desligue com clinica.dados-exemplo=false.
 */
@Component
@ConditionalOnProperty(name = "clinica.dados-exemplo", havingValue = "true")
public class DadosDeExemplo implements CommandLineRunner {

    private final PacienteService pacienteService;
    private final CobrancaService cobrancaService;

    public DadosDeExemplo(PacienteService pacienteService, CobrancaService cobrancaService) {
        this.pacienteService = pacienteService;
        this.cobrancaService = cobrancaService;
    }

    @Override
    public void run(String... args) {
        if (!pacienteService.listar().isEmpty()) {
            return;
        }
        Paciente ana = paciente("Ana Exemplo", "Ana Exemplo", "111.444.777-35", "150.00");
        Paciente bruno = paciente("Bruno Exemplo", "Carla Exemplo (mãe)", "123.456.789-09", "180.00");

        pacienteService.registrarSessao(ana.getId(), LocalDateTime.now().minusDays(7),
                StatusSessao.REALIZADA, "Anotação fictícia de exemplo. Visível apenas na área clínica.");
        pacienteService.registrarSessao(bruno.getId(), LocalDateTime.now().plusDays(2),
                StatusSessao.AGENDADA, null);

        cobrancaService.criar(ana.getCodigo(), new BigDecimal("150.00"), LocalDate.now().plusDays(5));
    }

    private Paciente paciente(String nome, String pagador, String cpf, String valor) {
        Paciente p = new Paciente();
        p.setNome(nome);
        p.setNomePagador(pagador);
        p.setCpfPagador(cpf);
        p.setValorSessao(new BigDecimal(valor));
        return pacienteService.cadastrar(p);
    }
}
