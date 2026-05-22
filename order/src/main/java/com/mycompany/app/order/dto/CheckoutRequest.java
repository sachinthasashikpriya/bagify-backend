package com.mycompany.app.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Checkout request submitted by a buyer.
 * Contains the shipping address and the list of items from the cart.
 * Stock validation is performed server-side against the product service.
 */
@Getter
@Setter
public class CheckoutRequest {

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    @NotEmpty(message = "Cart cannot be empty")
    @Valid
    private List<CheckoutItemDto> items;

    @Getter
    @Setter
    public static class CheckoutItemDto {
        private Long productId;
        private Integer quantity;
    }
}
