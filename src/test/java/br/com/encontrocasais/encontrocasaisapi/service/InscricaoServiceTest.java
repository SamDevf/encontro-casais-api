package br.com.encontrocasais.encontrocasaisapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.encontrocasais.encontrocasaisapi.dto.InscricaoPagamentoResponse;
import br.com.encontrocasais.encontrocasaisapi.entity.Inscricao;
import br.com.encontrocasais.encontrocasaisapi.entity.StatusPagamento;
import br.com.encontrocasais.encontrocasaisapi.repository.InscricaoRepository;

@ExtendWith(MockitoExtension.class)
class InscricaoServiceTest {

    @Mock
    private InscricaoRepository inscricaoRepository;

    @Test
    void reutilizaCheckoutDeInscricaoPendenteRecente() {
        Inscricao existente = new Inscricao(
                "Ana",
                "João",
                "(11) 99999-0000",
                "ana@example.com",
                "Igreja",
                new BigDecimal("150.00")
        );
        existente.setPreferenceId("pref-123");
        existente.setCheckoutUrl("https://mercadopago.test/checkout/pref-123");

        when(inscricaoRepository.buscarPendenteRecente(
                eq("11999990000"),
                eq("Ana"),
                eq("João"),
                any(LocalDateTime.class)
        )).thenReturn(Optional.of(existente));

        InscricaoService service = new InscricaoService(inscricaoRepository, "test-access-token", 30);
        InscricaoPagamentoResponse response = service.criarInscricao(new Inscricao(
                "Ana",
                "João",
                "11 99999-0000",
                "ana@example.com",
                "Igreja",
                new BigDecimal("150.00")
        ));

        assertEquals(StatusPagamento.PENDENTE, response.statusPagamento());
        assertEquals("pref-123", response.preferenceId());
        assertEquals("https://mercadopago.test/checkout/pref-123", response.urlCheckout());
        verify(inscricaoRepository, never()).save(any(Inscricao.class));
    }
}
