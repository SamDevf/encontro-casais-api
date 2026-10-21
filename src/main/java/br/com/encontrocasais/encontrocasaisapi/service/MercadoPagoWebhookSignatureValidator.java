package br.com.encontrocasais.encontrocasaisapi.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MercadoPagoWebhookSignatureValidator {

    private static final Pattern PAYMENT_ID_PATTERN = Pattern.compile("\\d+");
    private final String secret;

    public MercadoPagoWebhookSignatureValidator(
            @Value("${mercadopago.webhook.secret}") String secret
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("MERCADOPAGO_WEBHOOK_SECRET deve estar configurado.");
        }
        this.secret = secret;
    }

    public boolean isValid(String paymentId, String requestId, String signatureHeader) {
        if (paymentId == null || !PAYMENT_ID_PATTERN.matcher(paymentId).matches()
                || requestId == null || requestId.isBlank()
                || signatureHeader == null || signatureHeader.isBlank()) {
            return false;
        }

        Map<String, String> signatureParts = parseSignature(signatureHeader);
        String timestamp = signatureParts.get("ts");
        String receivedSignature = signatureParts.get("v1");
        if (timestamp == null || receivedSignature == null || !receivedSignature.matches("[a-fA-F0-9]{64}")) {
            return false;
        }

        String manifest = "id:" + paymentId.toLowerCase(Locale.ROOT)
                + ";request-id:" + requestId
                + ";ts:" + timestamp + ";";
        String expectedSignature = hmacSha256(manifest);
        return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.US_ASCII),
                receivedSignature.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII)
        );
    }

    private Map<String, String> parseSignature(String signatureHeader) {
        Map<String, String> parts = new HashMap<>();
        for (String part : signatureHeader.split(",")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length != 2) {
                return Map.of();
            }
            String key = pair[0].trim().toLowerCase(Locale.ROOT);
            if ((!key.equals("ts") && !key.equals("v1")) || parts.putIfAbsent(key, pair[1].trim()) != null) {
                return Map.of();
            }
        }
        return parts;
    }

    private String hmacSha256(String manifest) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = hmac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(Character.forDigit((value >> 4) & 0xF, 16));
                hex.append(Character.forDigit(value & 0xF, 16));
            }
            return hex.toString();
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("Não foi possível validar a assinatura do webhook.", exception);
        }
    }
}
