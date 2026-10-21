package br.com.encontrocasais.encontrocasaisapi.service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.common.PhoneRequest;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;

import br.com.encontrocasais.encontrocasaisapi.dto.InscricaoPagamentoResponse;
import br.com.encontrocasais.encontrocasaisapi.entity.Inscricao;
import br.com.encontrocasais.encontrocasaisapi.entity.StatusPagamento;
import br.com.encontrocasais.encontrocasaisapi.repository.InscricaoRepository;

@Service
public class InscricaoService {

    private final InscricaoRepository inscricaoRepository;
    private final PreferenceClient preferenceClient;
    private final PaymentClient paymentClient;
    private final long janelaDuplicidadeMinutos;

    public InscricaoService(
            InscricaoRepository inscricaoRepository,
            @Value("${mercadopago.access.token}") String accessToken,
            @Value("${inscricao.duplicidade.janela-minutos:30}") long janelaDuplicidadeMinutos
    ) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("A variável MERCADOPAGO_ACCESS_TOKEN deve estar configurada.");
        }
        if (janelaDuplicidadeMinutos <= 0) {
            throw new IllegalArgumentException("A janela de duplicidade deve ser maior que zero.");
        }
        MercadoPagoConfig.setAccessToken(accessToken);
        this.inscricaoRepository = inscricaoRepository;
        this.preferenceClient = new PreferenceClient();
        this.paymentClient = new PaymentClient();
        this.janelaDuplicidadeMinutos = janelaDuplicidadeMinutos;
    }

    public InscricaoPagamentoResponse criarInscricao(Inscricao dados) {
        Objects.requireNonNull(dados, "Os dados da inscrição são obrigatórios.");
        if (dados.getValor() == null || dados.getValor().signum() <= 0) {
            throw new IllegalArgumentException("O valor da inscrição deve ser maior que zero.");
        }

        Optional<Inscricao> pendenteExistente = inscricaoRepository.buscarPendenteRecente(
                normalizarTelefone(dados.getTelefone()),
                dados.getNomeConjuge1(),
                dados.getNomeConjuge2(),
                LocalDateTime.now().minusMinutes(janelaDuplicidadeMinutos)
        );
        if (pendenteExistente.isPresent()) {
            return reutilizarOuCriarPreferencia(pendenteExistente.get());
        }

        Inscricao inscricao = new Inscricao(
                dados.getNomeConjuge1(),
                dados.getNomeConjuge2(),
                dados.getTelefone(),
                dados.getEmail(),
                dados.getIgreja(),
                dados.getValor()
        );
        inscricao = inscricaoRepository.save(inscricao);

        return criarPreferencia(inscricao);
    }

    private InscricaoPagamentoResponse reutilizarOuCriarPreferencia(Inscricao inscricao) {
        if (inscricao.getPreferenceId() != null) {
            if (inscricao.getCheckoutUrl() != null && !inscricao.getCheckoutUrl().isBlank()) {
                return resposta(inscricao);
            }

            try {
                Preference preference = preferenceClient.get(inscricao.getPreferenceId());
                if (preference.getInitPoint() == null || preference.getInitPoint().isBlank()) {
                    throw new IllegalStateException("A preferência existente não possui URL de checkout.");
                }
                inscricao.setCheckoutUrl(preference.getInitPoint());
                inscricaoRepository.save(inscricao);
                return resposta(inscricao);
            } catch (MPException | MPApiException exception) {
                throw new IllegalStateException("Não foi possível recuperar o checkout existente no Mercado Pago.",
                        exception);
            }
        }
        return criarPreferencia(inscricao);
    }

    private InscricaoPagamentoResponse criarPreferencia(Inscricao inscricao) {
        PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                .items(List.of(PreferenceItemRequest.builder()
                        .title("Inscrição Encontro de Casais - "
                                + inscricao.getNomeConjuge1() + " e " + inscricao.getNomeConjuge2())
                        .quantity(1)
                        .currencyId("BRL")
                        .unitPrice(inscricao.getValor())
                        .build()))
                .payer(PreferencePayerRequest.builder()
                        .name(inscricao.getNomeConjuge1())
                        .surname(inscricao.getNomeConjuge2())
                        .phone(PhoneRequest.builder().number(inscricao.getTelefone()).build())
                        .build())
                .externalReference(inscricao.getId().toString())
                .build();

        try {
            Preference preference = preferenceClient.create(preferenceRequest);
            if (preference.getId() == null || preference.getInitPoint() == null) {
                throw new IllegalStateException("Mercado Pago retornou preferência sem ID ou URL de checkout.");
            }

            inscricao.setPreferenceId(preference.getId());
            inscricao.setCheckoutUrl(preference.getInitPoint());
            inscricaoRepository.save(inscricao);
            return resposta(inscricao);
        } catch (MPException | MPApiException exception) {
            throw new IllegalStateException("Não foi possível criar a preferência de pagamento no Mercado Pago.",
                    exception);
        }
    }

    private InscricaoPagamentoResponse resposta(Inscricao inscricao) {
        return new InscricaoPagamentoResponse(
                inscricao.getId(),
                inscricao.getStatusPagamento(),
                inscricao.getPreferenceId(),
                inscricao.getCheckoutUrl()
        );
    }

    private String normalizarTelefone(String telefone) {
        return telefone == null ? "" : telefone.replaceAll("[^0-9]", "");
    }

    @Transactional
    public Optional<Inscricao> atualizarPagamento(String paymentId) {
        if (paymentId == null || !paymentId.matches("\\d+")) {
            throw new IllegalArgumentException("paymentId deve ser numérico.");
        }

        Payment payment;
        try {
            payment = paymentClient.get(Long.valueOf(paymentId));
        } catch (MPException | MPApiException exception) {
            throw new IllegalStateException("Não foi possível consultar o pagamento no Mercado Pago.", exception);
        }

        if (payment.getId() == null || !paymentId.equals(payment.getId().toString())) {
            throw new IllegalStateException("O Mercado Pago retornou um pagamento diferente do solicitado.");
        }
        if (payment.getExternalReference() == null || !payment.getExternalReference().matches("\\d+")) {
            throw new IllegalStateException("O pagamento não possui uma referência de inscrição válida.");
        }
        Long inscricaoId = Long.valueOf(payment.getExternalReference());
        Inscricao inscricao = inscricaoRepository.buscarPorIdComBloqueio(inscricaoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Não foi encontrada inscrição para o pagamento informado."));

        if (payment.getTransactionAmount() == null
                || payment.getTransactionAmount().compareTo(inscricao.getValor()) != 0
                || !"BRL".equals(payment.getCurrencyId())) {
            throw new IllegalStateException("Valor ou moeda do pagamento não corresponde à inscrição.");
        }

        StatusPagamento novoStatus = converterStatus(payment.getStatus());
        String idConfirmado = Objects.requireNonNull(payment.getId(), "O Mercado Pago retornou pagamento sem ID")
                .toString();
        if (inscricao.getStatusPagamento() == StatusPagamento.APROVADO
                && novoStatus != StatusPagamento.APROVADO) {
            return Optional.empty();
        }
        if (idConfirmado.equals(inscricao.getPaymentId())
                && novoStatus == inscricao.getStatusPagamento()) {
            return Optional.empty();
        }

        boolean aprovadaAgora = novoStatus == StatusPagamento.APROVADO
                && inscricao.getStatusPagamento() != StatusPagamento.APROVADO;
        inscricao.setPaymentId(idConfirmado);
        inscricao.setStatusPagamento(novoStatus);
        Inscricao atualizada = inscricaoRepository.save(inscricao);
        return aprovadaAgora ? Optional.of(atualizada) : Optional.empty();
    }

    private StatusPagamento converterStatus(String status) {
        if (status == null) {
            throw new IllegalStateException("O Mercado Pago retornou pagamento sem status.");
        }

        return switch (status.toLowerCase(Locale.ROOT)) {
            case "approved" -> StatusPagamento.APROVADO;
            case "rejected" -> StatusPagamento.RECUSADO;
            case "cancelled", "canceled" -> StatusPagamento.CANCELADO;
            case "pending", "in_process", "in_mediation", "authorized" -> StatusPagamento.PENDENTE;
            default -> throw new IllegalStateException(
                    "Status de pagamento do Mercado Pago não suportado: " + status);
        };
    }
}
