package com.jaofraccaroli.clinica.clinico;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/clinico/pacientes")
public class ClinicoController {

    private final PacienteService pacienteService;

    public ClinicoController(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("pacientes", pacienteService.listar());
        return "clinico/pacientes";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("paciente", new Paciente());
        return "clinico/novo-paciente";
    }

    @PostMapping
    public String cadastrar(@Valid @ModelAttribute("paciente") Paciente paciente,
                            BindingResult erros,
                            RedirectAttributes redirect) {
        if (erros.hasErrors()) {
            return "clinico/novo-paciente";
        }
        Paciente salvo = pacienteService.cadastrar(paciente);
        redirect.addFlashAttribute("mensagem", "Paciente cadastrado com o código " + salvo.getCodigo());
        return "redirect:/clinico/pacientes";
    }

    @GetMapping("/{id}")
    public String prontuario(@PathVariable Long id, Model model) {
        model.addAttribute("paciente", pacienteService.buscar(id));
        model.addAttribute("sessoes", pacienteService.sessoesDo(id));
        model.addAttribute("statusPossiveis", StatusSessao.values());
        return "clinico/prontuario";
    }

    @PostMapping("/{id}/sessoes")
    public String registrarSessao(@PathVariable Long id,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataHora,
                                  @RequestParam(required = false) StatusSessao status,
                                  @RequestParam(required = false) String anotacoes,
                                  RedirectAttributes redirect) {
        pacienteService.registrarSessao(id, dataHora, status, anotacoes);
        redirect.addFlashAttribute("mensagem", "Sessão registrada");
        return "redirect:/clinico/pacientes/" + id;
    }
}
