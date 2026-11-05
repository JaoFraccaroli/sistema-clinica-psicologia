package com.jaofraccaroli.clinica;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Envia cada perfil para a sua área depois do login.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String inicio(Authentication autenticacao) {
        boolean ehPsicologa = autenticacao.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PSICOLOGA"));
        return ehPsicologa ? "redirect:/clinico/pacientes" : "redirect:/financeiro/cobrancas";
    }
}
