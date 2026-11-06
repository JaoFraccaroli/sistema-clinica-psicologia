package com.jaofraccaroli.clinica.financeiro;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final ExportadorCsvCobrancas exportador;

    public FinanceiroController(CobrancaService cobrancaService, ExportadorCsvCobrancas exportador) {
        this.cobrancaService = cobrancaService;
        this.exportador = exportador;
    }

    @GetMapping
    public String listar(Model model, Authentication autenticacao) {
        boolean podeEditar = autenticacao.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_FINANCEIRO"));
        model.addAttribute("cobrancas", cobrancaService.listar());
        model.addAttribute("podeEditar", podeEditar);
        model.addAttribute("hoje", LocalDate.now());
        return "financeiro/cobrancas";
    }

    /** Baixa a planilha das cobranças (.csv, abre no Excel). Só dados financeiros. */
    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar() {
        LocalDate hoje = LocalDate.now();
        byte[] csv = exportador.gerar(cobrancaService.listar(), hoje).getBytes(StandardCharsets.UTF_8);
        ContentDisposition anexo = ContentDisposition.attachment()
                .filename("cobrancas-" + hoje + ".csv")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, anexo.toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
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
