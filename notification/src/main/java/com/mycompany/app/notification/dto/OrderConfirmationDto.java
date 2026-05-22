package com.mycompany.app.notification.dto;

import lombok.Getter;
import lombok.Setter;

/** Payload for sending an order confirmation email to a buyer. */
@Getter
@Setter
public class OrderConfirmationDto {
    private String buyerEmail;
    private String buyerName;
    private Long orderId;
    private Double totalAmount;
    private String shippingAddress;
}
