package com.mycompany.app.notification.dto;

import lombok.Getter;
import lombok.Setter;

/** Payload for seller approval/rejection email notifications. */
@Getter
@Setter
public class SellerVerificationDto {
    private String sellerEmail;
    private String sellerName;
    private String status;          // "APPROVED" or "REJECTED"
    private String rejectionReason; // Only set when status is "REJECTED"
}
