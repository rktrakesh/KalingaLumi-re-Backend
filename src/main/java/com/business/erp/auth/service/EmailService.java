package com.business.erp.auth.service;

public interface EmailService {

    void sendWelcomeEmail(String toEmail, String fullName, String username, String temporaryPassword);

    void sendPasswordResetEmail(String toEmail, String fullName, String resetLink);
}