package com.mycompany.app.order.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * DTO containing all payment parameters required by the React frontend
 * to initiate a payment using the PayHere Sandbox SDK.
 */
@Getter
@Builder
public class PayHereParamsResponse {
    private String merchantId;
    private String orderId;
    private String amount;
    private String currency;
    private String hash;
    private boolean sandbox;
}
