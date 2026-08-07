package com.business.erp.auth.service.impl;

import com.business.erp.auth.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${app.mail.from:no-reply@kalingalumiere.local}")
    private String fromAddress;

    @Override
    public void sendWelcomeEmail(String toEmail, String fullName, String username, String temporaryPassword) {
        String subject = "Welcome to Kalinga Lumière ERP";
        String body = "Hi " + fullName + ",\n\n" +
                "Your Kalinga Lumière ERP account has been created.\n\n" +
                "Username: " + username + "\n" +
                "Temporary Password: " + temporaryPassword + "\n\n" +
                "You will be required to change this password the first time you log in.\n\n" +
                "— Kalinga Lumière";
        send(toEmail, subject, body, "sendWelcomeEmail");
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String fullName, String resetLink) {
        String subject = "Kalinga Lumière ERP — Password Reset Request";
        String body = "Hi " + fullName + ",\n\n" +
                "We received a request to reset your password. Click the link below to choose a new one:\n\n" +
                resetLink + "\n\n" +
                "If you didn't request this, you can safely ignore this email — your password will not change.\n\n" +
                "— Kalinga Lumière";
        send(toEmail, subject, body, "sendPasswordResetEmail");
    }

    private void send(String toEmail, String subject, String body, String caller) {
        if (!StringUtils.hasText(mailHost)) {
            // Mail intentionally unconfigured — don't fail the calling flow (onboarding,
            // forgot-password) just because SMTP credentials haven't been supplied yet.
            log.warn("EmailServiceImpl:{} :: SKIPPED — spring.mail.host is not configured. to={} subject={}",
                    caller, toEmail, subject);
            return;
        }
        if (!StringUtils.hasText(toEmail)) {
            log.warn("EmailServiceImpl:{} :: SKIPPED — recipient has no email address on file", caller);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("EmailServiceImpl:{} :: sent to={}", caller, toEmail);
        } catch (Exception ex) {
            // Never let an SMTP failure break onboarding/reset — the account/token still
            // exists and works; only the notification email failed to send.
            log.error("EmailServiceImpl:{} :: FAILED to={} reason={}", caller, toEmail, ex.getMessage());
        }
    }
}