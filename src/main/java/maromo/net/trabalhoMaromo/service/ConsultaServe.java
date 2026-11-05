package maromo.net.trabalhoMaromo.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import maromo.net.trabalhoMaromo.model.Consulta;
import maromo.net.trabalhoMaromo.repository.InterConsulta;

@Service
public class ConsultaServe {

    @Autowired
    private InterConsulta interConsulta;

    public List<Consulta> listarConsultas() {
        return interConsulta.findAll();
    }

    public Consulta agendarConsulta(Consulta consulta) {
        return interConsulta.save(consulta);
    }
}
