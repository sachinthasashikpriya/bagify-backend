package com.mycompany.app.notification.controller;

import com.mycompany.app.notification.dto.OrderConfirmationDto;
import com.mycompany.app.notification.dto.SellerVerificationDto;
import com.mycompany.app.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal REST controller — NOT exposed via the API Gateway.
 * Only callable by other services within the internal network.
 *
 * Paths use /internal/notify prefix to make it easy to block at gateway level.
 */
@RestController
@RequestMapping("/internal/notify")
@RequiredArgsConstructor
public class NotificationController {

    private final EmailService emailService;

    /** Called by order service after a successful checkout. */
    @PostMapping("/order-placed")
    public ResponseEntity<Void> notifyOrderPlaced(@RequestBody OrderConfirmationDto dto) {
        emailService.sendOrderConfirmation(dto);
        return ResponseEntity.ok().build();
    }

    /** Called by user service when a seller's verification is approved or rejected. */
    @PostMapping("/verification-result")
    public ResponseEntity<Void> notifyVerificationResult(@RequestBody SellerVerificationDto dto) {
        emailService.sendSellerVerificationResult(dto);
        return ResponseEntity.ok().build();
    }
}
