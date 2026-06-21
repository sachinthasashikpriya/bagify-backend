package com.mycompany.app.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for submitting a product review.
 * Buyer identity (buyerId, buyerName) is set server-side from the JWT — not from the client.
 */
@Getter
@Setter
public class ReviewRequest {

    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private int rating;

    @NotBlank(message = "Comment is required")
    private String comment;
}
