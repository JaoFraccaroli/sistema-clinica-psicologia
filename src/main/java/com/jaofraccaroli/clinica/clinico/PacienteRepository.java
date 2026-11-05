package com.jaofraccaroli.clinica.clinico;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    Optional<Paciente> findByCodigo(String codigo);

    List<Paciente> findAllByOrderByNomeAsc();
}
