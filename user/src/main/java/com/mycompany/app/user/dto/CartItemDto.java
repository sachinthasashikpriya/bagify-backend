package com.mycompany.app.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartItemDto {
    private Long id;
    private Long productId;
    private String productName;
    private String productImage;
    private Double price;
    private Integer stock;
    private Integer quantity;
}
