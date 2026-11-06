package com.jaofraccaroli.clinica.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/financeiro/cobrancas")
public class FinanceiroController {

    private final CobrancaService cobrancaService;

    public FinanceiroController(CobrancaService cobrancaService) {
        this.cobrancaService = cobrancaService;
    }

    @GetMapping
    public String listar(Model model, Authentication autenticacao) {
        boolean podeEditar = autenticacao.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_FINANCEIRO"));
        model.addAttribute("cobrancas", cobrancaService.listar());
        model.addAttribute("podeEditar", podeEditar);
        return "financeiro/cobrancas";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        model.addAttribute("pacientes", cobrancaService.pacientesParaCobranca());
        return "financeiro/nova-cobranca";
    }

    @PostMapping
    public String criar(@RequestParam String codigoPaciente,
                        @RequestParam BigDecimal valor,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate vencimento,
                        RedirectAttributes redirect) {
        try {
            cobrancaService.criar(codigoPaciente, valor, vencimento);
            redirect.addFlashAttribute("mensagem", "Cobrança criada");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/financeiro/cobrancas/nova";
        }
        return "redirect:/financeiro/cobrancas";
    }

    @PostMapping("/{id}/pagar")
    public String pagar(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            cobrancaService.registrarPagamento(id, LocalDate.now());
            redirect.addFlashAttribute("mensagem", "Pagamento registrado");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/financeiro/cobrancas";
    }
}
