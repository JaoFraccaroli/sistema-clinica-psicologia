package com.jaofraccaroli.clinica.seguranca;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Tela de login própria, em português (também usada depois de sair do sistema). */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
