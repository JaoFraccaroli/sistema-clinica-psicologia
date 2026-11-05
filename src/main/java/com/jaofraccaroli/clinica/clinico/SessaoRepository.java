package com.jaofraccaroli.clinica.clinico;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SessaoRepository extends JpaRepository<Sessao, Long> {

    List<Sessao> findByPacienteIdOrderByDataHoraDesc(Long pacienteId);
}
