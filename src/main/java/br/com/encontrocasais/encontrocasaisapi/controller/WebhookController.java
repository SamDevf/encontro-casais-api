package br.com.encontrocasais.encontrocasaisapi.controller;

import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.encontrocasais.encontrocasaisapi.service.InscricaoService;
import br.com.encontrocasais.encontrocasaisapi.service.EmailService;
import br.com.encontrocasais.encontrocasaisapi.service.MercadoPagoWebhookSignatureValidator;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final InscricaoService inscricaoService;
    private final EmailService emailService;
    private final MercadoPagoWebhookSignatureValidator signatureValidator;

    public WebhookController(
            InscricaoService inscricaoService,
            EmailService emailService,
            MercadoPagoWebhookSignatureValidator signatureValidator
    ) {
        this.inscricaoService = inscricaoService;
        this.emailService = emailService;
        this.signatureValidator = signatureValidator;
    }

    @PostMapping("/mercadopago")
    public ResponseEntity<Void> receberNotificacao(
            @RequestParam Map<String, String> queryParams,
            @RequestBody(required = false) Map<String, Object> payload,
            HttpServletRequest request
    ) {
        String queryTopic = queryParams.get("topic");
        String queryType = queryParams.get("type");
        String bodyType = value(payload, "type");
        String action = value(payload, "action");
        boolean notificacaoPagamento = isPayment(queryTopic)
                || isPayment(queryType)
                || isPayment(bodyType)
                || (action != null && action.toLowerCase().startsWith("payment."));

        if (!notificacaoPagamento) {
            return ResponseEntity.ok().build();
        }

        String queryPaymentId = Optional.ofNullable(queryParams.get("data.id"))
                .orElse(queryParams.get("id"));
        String bodyPaymentId = nestedValue(payload, "data", "id");
        if (queryPaymentId != null && bodyPaymentId != null && !queryPaymentId.equals(bodyPaymentId)) {
            return ResponseEntity.badRequest().build();
        }
        String paymentId = Optional.ofNullable(queryPaymentId).orElse(bodyPaymentId);
        if (paymentId == null || paymentId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (!signatureValidator.isValid(
                paymentId,
                request.getHeader("x-request-id"),
                request.getHeader("x-signature"))) {
            return ResponseEntity.status(401).build();
        }

        inscricaoService.atualizarPagamento(paymentId).ifPresent(inscricao -> {
            emailService.enviarConfirmacaoInscricao(inscricao);
            emailService.enviarAlertaOrganizador(inscricao);
        });
        return ResponseEntity.ok().build();
    }

    private boolean isPayment(String value) {
        return "payment".equalsIgnoreCase(value);
    }

    private String value(Map<String, Object> map, String key) {
        if (map == null || map.get(key) == null) {
            return null;
        }
        return map.get(key).toString();
    }

    private String nestedValue(Map<String, Object> map, String parent, String key) {
        if (map == null || !(map.get(parent) instanceof Map<?, ?> nested)) {
            return null;
        }
        Object value = nested.get(key);
        return value == null ? null : value.toString();
    }
}
