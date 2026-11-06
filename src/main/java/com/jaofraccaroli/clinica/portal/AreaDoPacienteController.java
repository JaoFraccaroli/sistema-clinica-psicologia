package com.jaofraccaroli.clinica.portal;

import java.time.LocalDateTime;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/paciente")
public class AreaDoPacienteController {

    private final AreaDoPacienteService areaDoPaciente;

    public AreaDoPacienteController(AreaDoPacienteService areaDoPaciente) {
        this.areaDoPaciente = areaDoPaciente;
    }

    /** O código vem do login, nunca da URL: não dá para trocar e ver outro paciente. */
    @GetMapping
    public String inicio(Authentication autenticacao, Model model) {
        model.addAttribute("resumo", areaDoPaciente.resumo(autenticacao.getName(), LocalDateTime.now()));
        return "paciente/inicio";
    }
}
