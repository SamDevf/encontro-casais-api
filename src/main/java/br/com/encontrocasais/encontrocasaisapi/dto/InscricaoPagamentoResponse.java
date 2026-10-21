package br.com.encontrocasais.encontrocasaisapi.dto;

import br.com.encontrocasais.encontrocasaisapi.entity.StatusPagamento;

public record InscricaoPagamentoResponse(
        Long id,
        StatusPagamento statusPagamento,
        String preferenceId,
        String urlCheckout
) {
}
