package com.medibook.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendAfterCommit(String toEmail, String subject, String htmlBody) {
        if (!mailEnabled) {
            log.debug("Email notification is disabled. Skipping email to {}", toEmail);
            return;
        }

        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Cannot send email notification because recipient email is blank. Subject: {}", subject);
            return;
        }

        boolean transactionActive = TransactionSynchronizationManager.isSynchronizationActive();

        if (transactionActive) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendEmail(toEmail, subject, htmlBody);
                }
            });
        } else {
            sendEmail(toEmail, subject, htmlBody);
        }
    }

    private void sendEmail(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    "UTF-8"
            );

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);

            log.info("Email notification sent to {} with subject: {}", toEmail, subject);
        } catch (MessagingException | RuntimeException e) {
            log.error(
                    "Failed to send email notification to {} with subject: {}. Business operation remains successful.",
                    toEmail,
                    subject,
                    e
            );
        }
    }
}