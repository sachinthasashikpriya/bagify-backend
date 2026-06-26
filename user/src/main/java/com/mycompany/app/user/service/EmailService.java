package com.mycompany.app.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendResetPasswordEmail(String to, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Password Reset Request");
        message.setText("To reset your password, click the link below:\n" + resetLink + "\n\nThis link will expire in 15 minutes.");
        mailSender.send(message);
    }

    public void sendVerificationOtpEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Bagify - Account Verification OTP");
        message.setText("Welcome to Bagify! Your one-time verification code is:\n\n" + otp + "\n\nThis code will expire in 10 minutes.");
        mailSender.send(message);
    }
}
