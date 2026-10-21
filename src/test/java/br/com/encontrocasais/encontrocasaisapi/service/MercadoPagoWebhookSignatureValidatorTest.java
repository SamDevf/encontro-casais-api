package br.com.encontrocasais.encontrocasaisapi.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MercadoPagoWebhookSignatureValidatorTest {

    private final MercadoPagoWebhookSignatureValidator validator =
            new MercadoPagoWebhookSignatureValidator("test-webhook-secret");

    @Test
    void acceptsValidMercadoPagoSignature() {
        assertTrue(validator.isValid(
                "123456",
                "req-abc",
                "ts=1700000000,v1=5e9aced68189095c47631a580ee79275fb6ab2331e427a14050eec93ae396837"
        ));
    }

    @Test
    void rejectsTamperedOrIncompleteSignature() {
        assertFalse(validator.isValid(
                "123456",
                "req-abc",
                "ts=1700000000,v1=5e9aced68189095c47631a580ee79275fb6ab2331e427a14050eec93ae396838"
        ));
        assertFalse(validator.isValid("123456", "req-abc", "ts=1700000000"));
        assertFalse(validator.isValid("not-numeric", "req-abc", "ts=1700000000,v1=invalid"));
    }
}
