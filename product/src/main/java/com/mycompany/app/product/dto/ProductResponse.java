package com.mycompany.app.product.dto;

import com.mycompany.app.product.entity.Product;
import lombok.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private String id;
    private String name;
    private String description;
    private double price;
    private int stock;
    private String category;
    private String image;
    private String sellerId;
    private String sellerName;
    private double sellerRating;
    private boolean sellerVerified;
    private double averageRating;
    private int reviewCount;
    private String status;
    private List<ReviewResponse> reviews;

    public static ProductResponse fromEntity(Product product) {
        if (product == null) return null;
        return ProductResponse.builder()
                .id(product.getId() != null ? product.getId().toString() : null)
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .category(product.getCategory())
                .image(product.getImage())
                .sellerId(product.getSellerId())
                .sellerName(product.getSellerName())
                .sellerRating(product.getSellerRating())
                .averageRating(product.getAverageRating())
                .reviewCount(product.getReviews() != null ? product.getReviews().size() : 0)
                .status(product.getStatus() != null ? product.getStatus().name() : null)
                .reviews(product.getReviews() != null 
                        ? product.getReviews().stream().map(ReviewResponse::fromEntity).collect(Collectors.toList())
                        : Collections.emptyList())
                .build();
    }
}
