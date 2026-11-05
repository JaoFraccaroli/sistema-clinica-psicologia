package com.jaofraccaroli.clinica.financeiro;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CobrancaRepository extends JpaRepository<Cobranca, Long> {

    List<Cobranca> findAllByOrderByVencimentoAsc();
}
