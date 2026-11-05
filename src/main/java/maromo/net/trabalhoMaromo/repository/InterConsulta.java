package maromo.net.trabalhoMaromo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import maromo.net.trabalhoMaromo.model.Consulta;

public interface InterConsulta extends JpaRepository<Consulta, Long> {
}
