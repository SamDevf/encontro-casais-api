package br.com.encontrocasais.encontrocasaisapi.service;

import java.text.NumberFormat;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import br.com.encontrocasais.encontrocasaisapi.entity.Inscricao;

@Service
public class EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String organizadorEmail;
    private final String remetente;
    private final String senhaSmtp;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${organizador.email:}") String organizadorEmail,
            @Value("${spring.mail.username:}") String remetente,
            @Value("${spring.mail.password:}") String senhaSmtp
    ) {
        this.mailSender = mailSender;
        this.organizadorEmail = organizadorEmail;
        this.remetente = remetente;
        this.senhaSmtp = senhaSmtp;
    }

    @Async
    public void enviarConfirmacaoInscricao(Inscricao inscricao) {
        if (!smtpConfigurado() || inscricao.getEmail() == null || inscricao.getEmail().isBlank()) {
            LOGGER.warn("E-mail de confirmação da inscrição {} não enviado: SMTP ou e-mail do casal não configurado.",
                    inscricao.getId());
            return;
        }
        String valorPago = formatarValor(inscricao);
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(inscricao.getEmail());
        mensagem.setSubject("Pagamento confirmado - Encontro de Casais");
        mensagem.setText("""
                Olá, %s e %s!

                Agradecemos pela inscrição no Encontro de Casais. Seu pagamento foi confirmado e a vaga do casal está garantida.

                Igreja: %s
                Valor pago: %s
                Código da inscrição: %d

                Será uma alegria receber vocês!
                """.formatted(
                inscricao.getNomeConjuge1(),
                inscricao.getNomeConjuge2(),
                inscricao.getIgreja() == null || inscricao.getIgreja().isBlank()
                        ? "Não informada"
                        : inscricao.getIgreja(),
                valorPago,
                inscricao.getId()
        ));
        enviar(mensagem, "confirmação", inscricao.getId());
    }

    @Async
    public void enviarAlertaOrganizador(Inscricao inscricao) {
        if (!smtpConfigurado() || organizadorEmail == null || organizadorEmail.isBlank()) {
            LOGGER.warn("Alerta da inscrição {} não enviado: SMTP ou e-mail do organizador não configurado.",
                    inscricao.getId());
            return;
        }
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(organizadorEmail);
        mensagem.setSubject("Nova vaga paga - Encontro de Casais");
        mensagem.setText("""
                Uma nova inscrição foi paga e confirmada.

                Casal: %s e %s
                E-mail: %s
                Telefone: %s
                Igreja: %s
                Valor pago: %s
                Código da inscrição: %d
                """.formatted(
                inscricao.getNomeConjuge1(),
                inscricao.getNomeConjuge2(),
                inscricao.getEmail(),
                inscricao.getTelefone(),
                inscricao.getIgreja() == null || inscricao.getIgreja().isBlank()
                        ? "Não informada"
                        : inscricao.getIgreja(),
                formatarValor(inscricao),
                inscricao.getId()
        ));
        enviar(mensagem, "alerta ao organizador", inscricao.getId());
    }

    private String formatarValor(Inscricao inscricao) {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
                .format(inscricao.getValor());
    }

    private boolean smtpConfigurado() {
        return remetente != null && !remetente.isBlank()
                && senhaSmtp != null && !senhaSmtp.isBlank();
    }

    private void enviar(SimpleMailMessage mensagem, String tipo, Long inscricaoId) {
        try {
            mailSender.send(mensagem);
        } catch (RuntimeException exception) {
            LOGGER.error("Falha ao enviar e-mail de {} da inscrição {}", tipo, inscricaoId, exception);
            throw exception;
        }
    }
}
