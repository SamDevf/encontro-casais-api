package br.com.encontrocasais.encontrocasaisapi.dto;

import java.math.BigDecimal;

import br.com.encontrocasais.encontrocasaisapi.entity.StatusPagamento;

public record InscricaoResponseDTO(
        Long id,
        String nomeConjuge1,
        String nomeConjuge2,
        String telefone,
        String igreja,
        BigDecimal valor,
        StatusPagamento statusPagamento,
        String initPoint
) {
}
