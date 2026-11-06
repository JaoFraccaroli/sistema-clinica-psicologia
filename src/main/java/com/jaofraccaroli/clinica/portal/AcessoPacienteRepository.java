package com.jaofraccaroli.clinica.portal;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AcessoPacienteRepository extends JpaRepository<AcessoPaciente, Long> {

    Optional<AcessoPaciente> findByCodigoPaciente(String codigoPaciente);
}
