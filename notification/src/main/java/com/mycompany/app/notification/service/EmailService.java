package com.mycompany.app.notification.service;

import com.mycompany.app.notification.dto.OrderConfirmationDto;
import com.mycompany.app.notification.dto.SellerVerificationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Central email service — the single place in the system that sends emails.
 * Called by other services via REST. In production this would be event-driven
 * (Kafka/RabbitMQ) but REST is appropriate for this project scope.
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /** Send order confirmation email to the buyer after a successful checkout. */
    public void sendOrderConfirmation(OrderConfirmationDto dto) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(dto.getBuyerEmail());
        message.setSubject("Bagify — Order Confirmed #" + dto.getOrderId());
        message.setText(String.format(
                "Hi %s,%n%n" +
                "Thank you for your order! Your order #%d has been confirmed.%n%n" +
                "Total: $%.2f%n" +
                "Shipping to: %s%n%n" +
                "We'll notify you when your order ships.%n%n" +
                "Bagify Team",
                dto.getBuyerName(),
                dto.getOrderId(),
                dto.getTotalAmount(),
                dto.getShippingAddress()
        ));
        mailSender.send(message);
    }

    /** Send seller verification result email (APPROVED or REJECTED). */
    public void sendSellerVerificationResult(SellerVerificationDto dto) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(dto.getSellerEmail());

        if ("APPROVED".equalsIgnoreCase(dto.getStatus())) {
            message.setSubject("Bagify — Seller Verification Approved");
            message.setText(String.format(
                    "Hi %s,%n%n" +
                    "Congratulations! Your seller account has been approved.%n" +
                    "You can now start listing products on Bagify.%n%n" +
                    "Bagify Team",
                    dto.getSellerName()
            ));
        } else {
            message.setSubject("Bagify — Seller Verification Update");
            message.setText(String.format(
                    "Hi %s,%n%n" +
                    "Your seller verification was not approved.%n" +
                    "Reason: %s%n%n" +
                    "Please resubmit with the required documents.%n%n" +
                    "Bagify Team",
                    dto.getSellerName(),
                    dto.getRejectionReason() != null ? dto.getRejectionReason() : "N/A"
            ));
        }
        mailSender.send(message);
    }

    /** Password reset email — migrated from user service. */
    public void sendResetPasswordEmail(String to, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Bagify — Password Reset Request");
        message.setText(
                "To reset your password, click the link below:\n" + resetLink +
                "\n\nThis link will expire in 15 minutes.\n\nBagify Team"
        );
        mailSender.send(message);
    }
}
