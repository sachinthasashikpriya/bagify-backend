package com.mycompany.app.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for creating or updating a product.
 * Using a dedicated request DTO instead of the entity directly prevents
 * mass-assignment vulnerabilities (e.g., client cannot set averageRating or sellerId).
 */
@Getter
@Setter
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    @NotBlank(message = "Description is required")
    private String description;

    @Positive(message = "Price must be greater than 0")
    private double price;

    @Min(value = 0, message = "Stock cannot be negative")
    private int stock;

    @NotBlank(message = "Category is required")
    private String category;

    private String image;
}
