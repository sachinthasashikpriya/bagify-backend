package com.mycompany.app.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductDto {
    private String id;
    private String name;
    private String description;
    private Double price;
    private String category;
    private String image;
    private String sellerId;
    private String sellerName;
    private Double sellerRating;
    private int stock;
    private Double averageRating;
}
