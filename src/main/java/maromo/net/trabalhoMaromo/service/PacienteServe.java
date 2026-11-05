package maromo.net.trabalhoMaromo.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import maromo.net.trabalhoMaromo.model.Paciente;
import maromo.net.trabalhoMaromo.repository.InterPaciente;

@Service
public class PacienteServe {

    @Autowired
    private InterPaciente interPaciente;

    public List<Paciente> listarPacientes() {
        return interPaciente.findAll();
    }

    public Paciente cadastrarPaciente(Paciente paciente) {
        return interPaciente.save(paciente);
    }
}
