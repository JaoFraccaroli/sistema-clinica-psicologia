package maromo.net.trabalhoMaromo.controller;

import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import maromo.net.trabalhoMaromo.model.Consulta;
import maromo.net.trabalhoMaromo.service.ConsultaServe;

@Controller
@RequestMapping("/consultas")
public class ConsultaController {

    @Autowired
    private ConsultaServe consultaService;

    @GetMapping("/agendar")
    public String exibirFormularioAgendamento(Model model) {
        model.addAttribute("consulta", new Consulta());
        return "agendar_consulta";
    }

    @PostMapping("/agendar")
    public String agendarConsulta(@ModelAttribute Consulta consulta) {
        consultaService.agendarConsulta(consulta);
        return "redirect:/consultas/listar";
    }

    @GetMapping("/listar")
    public String listarConsultas(Model model) {
        model.addAttribute("consultas", consultaService.listarConsultas());
        return "listar_consulta";
    }
}