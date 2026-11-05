package maromo.net.trabalhoMaromo.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import maromo.net.trabalhoMaromo.model.Consulta;
import maromo.net.trabalhoMaromo.service.ConsultaServe;

@WebMvcTest(ConsultaController.class)
class ConsultaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConsultaServe consultaServe;

    @Test
    void agendarRedirecionaParaListagem() throws Exception {
        mockMvc.perform(post("/consultas/agendar")
                        .param("especialidade", "odontologia")
                        .param("horario", "2026-11-05T14:30"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/consultas/listar"));

        verify(consultaServe).agendarConsulta(any(Consulta.class));
    }

    @Test
    void listagemMostraConsultaSemPacienteSemErro() throws Exception {
        // Bug encontrado: a tela quebrava quando a consulta não tinha paciente vinculado
        Consulta consulta = new Consulta();
        consulta.setId(1L);
        consulta.setEspecialidade("medicina");
        consulta.setHorario(LocalDateTime.of(2026, 11, 5, 14, 30));
        when(consultaServe.listarConsultas()).thenReturn(List.of(consulta));

        mockMvc.perform(get("/consultas/listar"))
                .andExpect(status().isOk())
                .andExpect(view().name("listar_consulta"));
    }
}
