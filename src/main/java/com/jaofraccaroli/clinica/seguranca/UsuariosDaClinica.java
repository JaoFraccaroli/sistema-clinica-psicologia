package com.jaofraccaroli.clinica.seguranca;

import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.jaofraccaroli.clinica.portal.AcessoPacienteRepository;

/**
 * Quem pode entrar no sistema:
 * - a equipe (psicologa e financeiro), com senhas da configuração;
 * - os pacientes, com o código do paciente como usuário e a senha gerada pela psicóloga.
 */
@Component
public class UsuariosDaClinica implements UserDetailsService {

    static final String NAO_ENCONTRADO = "Usuário não encontrado";

    /** Usuário -> {perfil, hash da senha} */
    private final Map<String, String[]> equipe;
    private final AcessoPacienteRepository acessos;

    public UsuariosDaClinica(@Value("${clinica.senha.psicologa}") String senhaPsicologa,
                             @Value("${clinica.senha.financeiro}") String senhaFinanceiro,
                             PasswordEncoder codificador,
                             AcessoPacienteRepository acessos) {
        this.equipe = Map.of(
                "psicologa", new String[] {"PSICOLOGA", codificador.encode(senhaPsicologa)},
                "financeiro", new String[] {"FINANCEIRO", codificador.encode(senhaFinanceiro)});
        this.acessos = acessos;
    }

    @Override
    public UserDetails loadUserByUsername(String usuario) {
        if (usuario == null || usuario.isBlank()) {
            throw new UsernameNotFoundException(NAO_ENCONTRADO);
        }
        String nome = usuario.trim();
        String[] daEquipe = equipe.get(nome.toLowerCase(Locale.ROOT));
        if (daEquipe != null) {
            // Objeto novo a cada login: o Spring apaga a senha do objeto depois de autenticar
            return User.withUsername(nome.toLowerCase(Locale.ROOT))
                    .password(daEquipe[1])
                    .roles(daEquipe[0])
                    .build();
        }
        return acessos.findByCodigoPaciente(nome.toUpperCase(Locale.ROOT))
                .map(a -> User.withUsername(a.getCodigoPaciente())
                        .password(a.getSenhaHash())
                        .roles("PACIENTE")
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException(NAO_ENCONTRADO));
    }
}
