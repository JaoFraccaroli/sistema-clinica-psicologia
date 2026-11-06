package com.jaofraccaroli.clinica.comum;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidoValidator implements ConstraintValidator<CpfValido, String> {

    @Override
    public boolean isValid(String cpf, ConstraintValidatorContext contexto) {
        if (cpf == null || cpf.isBlank()) {
            return true;
        }
        return ValidadorCpf.valido(cpf);
    }
}
