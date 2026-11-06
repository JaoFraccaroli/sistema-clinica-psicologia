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
        if (temPerfil(autenticacao, "ROLE_PSICOLOGA")) {
            return "redirect:/clinico/pacientes";
        }
        if (temPerfil(autenticacao, "ROLE_PACIENTE")) {
            return "redirect:/paciente";
        }
        return "redirect:/financeiro/cobrancas";
    }

    private static boolean temPerfil(Authentication autenticacao, String perfil) {
        return autenticacao.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(perfil));
    }
}
