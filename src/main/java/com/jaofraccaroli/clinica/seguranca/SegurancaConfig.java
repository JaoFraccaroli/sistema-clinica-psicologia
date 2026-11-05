package com.jaofraccaroli.clinica.seguranca;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Controle de acesso por perfil.
 *
 * PSICOLOGA: acessa a área clínica e a financeira.
 * FINANCEIRO: acessa somente a área financeira. Nunca vê dados clínicos.
 */
@Configuration
public class SegurancaConfig {

    @Bean
    public SecurityFilterChain filtroDeSeguranca(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(acesso -> acesso
                .requestMatchers("/login", "/error").permitAll()
                .requestMatchers("/clinico/**").hasRole("PSICOLOGA")
                .requestMatchers("/financeiro/**").hasAnyRole("PSICOLOGA", "FINANCEIRO")
                .anyRequest().authenticated())
            .formLogin(login -> login.defaultSuccessUrl("/", true).permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll());
        return http.build();
    }

    /**
     * Usuários de demonstração. As senhas podem ser trocadas pelas variáveis
     * de ambiente SENHA_PSICOLOGA e SENHA_FINANCEIRO.
     */
    @Bean
    public UserDetailsService usuarios(
            @Value("${clinica.senha.psicologa}") String senhaPsicologa,
            @Value("${clinica.senha.financeiro}") String senhaFinanceiro) {
        PasswordEncoder codificador = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        return new InMemoryUserDetailsManager(
                User.withUsername("psicologa")
                        .password(codificador.encode(senhaPsicologa))
                        .roles("PSICOLOGA")
                        .build(),
                User.withUsername("financeiro")
                        .password(codificador.encode(senhaFinanceiro))
                        .roles("FINANCEIRO")
                        .build());
    }
}
