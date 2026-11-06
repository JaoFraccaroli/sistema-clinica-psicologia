package com.jaofraccaroli.clinica;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.jaofraccaroli.clinica.clinico.Paciente;
import com.jaofraccaroli.clinica.clinico.PacienteService;
import com.jaofraccaroli.clinica.clinico.StatusSessao;
import com.jaofraccaroli.clinica.financeiro.CobrancaService;
import com.jaofraccaroli.clinica.portal.AcessoPacienteService;

/**
 * Cria pacientes FICTÍCIOS para demonstração. Nenhum dado real.
 * Desligue com clinica.dados-exemplo=false.
 */
@Component
@ConditionalOnProperty(name = "clinica.dados-exemplo", havingValue = "true")
public class DadosDeExemplo implements CommandLineRunner {

    /** Códigos fixos para dar para entrar como paciente na demonstração. */
    static final String CODIGO_ANA = "PAC-DEMO0001";
    static final String CODIGO_BRUNO = "PAC-DEMO0002";

    private final PacienteService pacienteService;
    private final CobrancaService cobrancaService;
    private final AcessoPacienteService acessoPaciente;
    private final String senhaPacienteExemplo;

    public DadosDeExemplo(PacienteService pacienteService, CobrancaService cobrancaService,
                          AcessoPacienteService acessoPaciente,
                          @Value("${clinica.senha.paciente-exemplo:paciente123}") String senhaPacienteExemplo) {
        this.pacienteService = pacienteService;
        this.cobrancaService = cobrancaService;
        this.acessoPaciente = acessoPaciente;
        this.senhaPacienteExemplo = senhaPacienteExemplo;
    }

    @Override
    public void run(String... args) {
        if (!pacienteService.listar().isEmpty()) {
            return;
        }
        Paciente ana = paciente(CODIGO_ANA, "Ana Exemplo", "Ana Exemplo", "111.444.777-35", "150.00");
        Paciente bruno = paciente(CODIGO_BRUNO, "Bruno Exemplo", "Carla Exemplo (mãe)", "123.456.789-09", "180.00");

        pacienteService.registrarSessao(ana.getId(), LocalDateTime.now().minusDays(7),
                StatusSessao.REALIZADA, "Anotação fictícia de exemplo. Visível apenas na área clínica.");
        pacienteService.registrarSessao(ana.getId(), LocalDateTime.now().plusDays(7).withHour(14).withMinute(0),
                StatusSessao.AGENDADA, null);
        pacienteService.registrarSessao(bruno.getId(), LocalDateTime.now().plusDays(2),
                StatusSessao.AGENDADA, null);

        cobrancaService.criar(ana.getCodigo(), new BigDecimal("150.00"), LocalDate.now().plusDays(5));

        // A Ana já tem acesso de paciente: entra com PAC-DEMO0001 / paciente123
        acessoPaciente.definirSenha(ana.getCodigo(), senhaPacienteExemplo);
    }

    private Paciente paciente(String codigo, String nome, String pagador, String cpf, String valor) {
        Paciente p = new Paciente();
        p.setCodigo(codigo);
        p.setNome(nome);
        p.setNomePagador(pagador);
        p.setCpfPagador(cpf);
        p.setValorSessao(new BigDecimal(valor));
        return pacienteService.cadastrar(p);
    }
}
