package maromo.net.trabalhoMaromo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import maromo.net.trabalhoMaromo.model.Paciente;

public interface InterPaciente extends JpaRepository<Paciente, Long> {
}
