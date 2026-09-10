package com.hansana.helpdesk.common.service;

import com.hansana.helpdesk.common.exception.EmailDeliveryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);
    }

    @Test
    void sendTemporaryCredentialsEmail_preparesAndSendsMessage() {
        emailService.sendTemporaryCredentialsEmail("agent@helpdesk.dev", "Agent Dev", "TempSecretPass123");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertNotNull(sentMessage);
        assertEquals("agent@helpdesk.dev", Objects.requireNonNull(sentMessage.getTo())[0]);
        assertEquals("Your Help Desk Account Credentials", sentMessage.getSubject());
        assertNotNull(sentMessage.getText());
        assertTrue(sentMessage.getText().contains("TempSecretPass123"));
        assertTrue(sentMessage.getText().contains("Agent Dev"));
        assertTrue(sentMessage.getText().contains("change your temporary password"));
    }

    @Test
    void sendTemporaryCredentialsEmail_whenMailSenderFails_throwsEmailDeliveryException() {
        doThrow(new MailSendException("SMTP connection refused"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        EmailDeliveryException ex = assertThrows(EmailDeliveryException.class, () ->
                emailService.sendTemporaryCredentialsEmail("agent@helpdesk.dev", "Agent Dev", "TempSecretPass123"));

        assertTrue(ex.getMessage().contains("Failed to deliver temporary credentials email"));
    }
}
