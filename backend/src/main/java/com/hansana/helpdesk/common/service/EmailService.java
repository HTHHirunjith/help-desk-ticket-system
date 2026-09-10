package com.hansana.helpdesk.common.service;

import com.hansana.helpdesk.common.exception.EmailDeliveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@helpdesk.dev}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendTemporaryCredentialsEmail(String recipientEmail, String recipientName, String temporaryPassword) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (fromAddress != null && !fromAddress.isBlank()) {
                message.setFrom(fromAddress);
            }
            message.setTo(recipientEmail);
            message.setSubject("Your Help Desk Account Credentials");
            message.setText(
                    "Hello " + recipientName + ",\n\n" +
                    "An account has been created for you in the Help Desk Ticket System.\n\n" +
                    "Account Email: " + recipientEmail + "\n" +
                    "Temporary Password: " + temporaryPassword + "\n\n" +
                    "Please log in to the application and change your temporary password immediately upon your first login.\n\n" +
                    "Note: This is an automated notification. Do not reply to this email."
            );

            mailSender.send(message);
            log.info("Temporary credentials email sent successfully to {}", recipientEmail);
        } catch (Exception ex) {
            log.error("Failed to send credentials email to {}: {}", recipientEmail, ex.getMessage());
            throw new EmailDeliveryException("Failed to deliver temporary credentials email. Account creation was aborted.", ex);
        }
    }
}
