package com.mycompany.app.product.dto;

import com.mycompany.app.product.entity.Review;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {
    private String id;
    private String productId;
    private String buyerId;
    private String buyerName;
    private int rating;
    private String comment;
    private String date;

    public static ReviewResponse fromEntity(Review review) {
        if (review == null) return null;
        return ReviewResponse.builder()
                .id(review.getId() != null ? review.getId().toString() : null)
                .productId(review.getProduct() != null && review.getProduct().getId() != null ? review.getProduct().getId().toString() : null)
                .buyerId(review.getBuyerId())
                .buyerName(review.getBuyerName())
                .rating(review.getRating())
                .comment(review.getComment())
                .date(review.getDate())
                .build();
    }
}
