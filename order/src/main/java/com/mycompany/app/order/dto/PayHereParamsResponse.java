package com.mycompany.app.order.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * DTO containing all payment parameters required by the React frontend
 * to initiate a payment using the PayHere Sandbox SDK.
 */
@Getter
@Builder
public class PayHereParamsResponse {
    private boolean sandbox;

    @JsonProperty("merchant_id")
    private String merchantId;

    @JsonProperty("return_url")
    private String returnUrl;

    @JsonProperty("cancel_url")
    private String cancelUrl;

    @JsonProperty("notify_url")
    private String notifyUrl;

    @JsonProperty("order_id")
    private String orderId;

    private String items;
    private String amount;
    private String currency;
    private String hash;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    private String email;
    private String phone;
    private String address;
    private String city;
    private String country;
}
