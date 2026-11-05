package maromo.net.trabalhoMaromo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import maromo.net.trabalhoMaromo.model.Paciente;
import maromo.net.trabalhoMaromo.service.PacienteServe;

@WebMvcTest(PacienteController.class)
class PacienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PacienteServe pacienteServe;

    @Test
    void exibeFormularioDeCadastro() throws Exception {
        mockMvc.perform(get("/pacientes/cadastrar"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastrar_paciente"));
    }

    @Test
    void cadastrarRedirecionaParaListagem() throws Exception {
        mockMvc.perform(post("/pacientes/cadastrar")
                        .param("nome", "Maria")
                        .param("idade", "30")
                        .param("telefone", "19999999999")
                        .param("email", "maria@email.com")
                        .param("especialidadeDesejada", "medicina"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pacientes/listar"));

        verify(pacienteServe).cadastrarPaciente(any(Paciente.class));
    }

    @Test
    void listagemDePacientesAbre() throws Exception {
        mockMvc.perform(get("/pacientes/listar"))
                .andExpect(status().isOk())
                .andExpect(view().name("listar_pacientes"));
    }
}
