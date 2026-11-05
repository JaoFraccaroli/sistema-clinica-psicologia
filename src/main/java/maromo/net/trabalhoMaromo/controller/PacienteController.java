package maromo.net.trabalhoMaromo.controller;

import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import maromo.net.trabalhoMaromo.model.Paciente;
import maromo.net.trabalhoMaromo.service.PacienteServe;

@Controller
@RequestMapping ("/pacientes")
public class PacienteController {

    @Autowired
    private PacienteServe pacienteService;

    @GetMapping("/cadastrar")
    public String exibirFormularioCadastro(Model model) {
        model.addAttribute("paciente", new Paciente());
        return "cadastrar_paciente";
    }

    @PostMapping("/cadastrar")
    public String cadastrarPaciente(@ModelAttribute Paciente paciente) {
        pacienteService.cadastrarPaciente(paciente);
        return "redirect:/pacientes/listar";
    }

    @GetMapping("/listar")
    public String listarPacientes(Model model) {
        model.addAttribute("pacientes", pacienteService.listarPacientes());
        return "listar_pacientes";
    }
}
