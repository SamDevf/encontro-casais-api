package br.com.encontrocasais.encontrocasaisapi.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.encontrocasais.encontrocasaisapi.entity.Inscricao;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    void sendsCoupleConfirmationWithPaidAmount() {
        EmailService emailService = new EmailService(
                mailSender, "organizador@example.com", "no-reply@example.com", "test-password");
        Inscricao inscricao = inscricaoConfirmada();

        emailService.enviarConfirmacaoInscricao(inscricao);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertTrue(captor.getValue().getText().contains("R$"));
        assertTrue(captor.getValue().getText().contains("150,00"));
    }

    @Test
    void sendsOrganizerAlertToConfiguredAddress() {
        EmailService emailService = new EmailService(
                mailSender, "organizador@example.com", "no-reply@example.com", "test-password");

        emailService.enviarAlertaOrganizador(inscricaoConfirmada());

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertTrue(captor.getValue().getTo()[0].equals("organizador@example.com"));
    }

    private Inscricao inscricaoConfirmada() {
        Inscricao inscricao = new Inscricao(
                "Ana",
                "João",
                "11999990000",
                "ana@example.com",
                "Igreja",
                new BigDecimal("150.00")
        );
        ReflectionTestUtils.setField(inscricao, "id", 42L);
        return inscricao;
    }
}
