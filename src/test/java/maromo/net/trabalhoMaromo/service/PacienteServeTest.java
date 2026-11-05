package maromo.net.trabalhoMaromo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import maromo.net.trabalhoMaromo.model.Paciente;
import maromo.net.trabalhoMaromo.repository.InterPaciente;

@ExtendWith(MockitoExtension.class)
class PacienteServeTest {

    @Mock
    private InterPaciente interPaciente;

    @InjectMocks
    private PacienteServe pacienteServe;

    @Test
    void cadastrarPacienteSalvaNoRepositorio() {
        Paciente paciente = new Paciente();
        paciente.setNome("Maria");
        when(interPaciente.save(paciente)).thenReturn(paciente);

        Paciente salvo = pacienteServe.cadastrarPaciente(paciente);

        verify(interPaciente).save(paciente);
        assertThat(salvo.getNome()).isEqualTo("Maria");
    }

    @Test
    void listarPacientesRetornaTodosDoRepositorio() {
        when(interPaciente.findAll()).thenReturn(List.of(new Paciente(), new Paciente()));

        assertThat(pacienteServe.listarPacientes()).hasSize(2);
    }
}
