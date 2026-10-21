package br.com.encontrocasais.encontrocasaisapi.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Email;

public record InscricaoRequestDTO(
        @NotBlank String nomeConjuge1,
        @NotBlank String nomeConjuge2,
        @NotBlank String telefone,
        @NotBlank @Email String email,
        @NotBlank String igreja,
        @NotNull @Positive BigDecimal valor
) {
}
