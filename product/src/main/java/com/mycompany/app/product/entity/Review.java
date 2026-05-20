package com.mycompany.app.product.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @JsonBackReference
    private Product product;

    private String buyerId;
    private String buyerName;
    private int rating;

    @Column(length = 1000)
    private String comment;

    private String date;

    // Helper to get productId in JSON output
    public String getProductId() {
        return product != null ? product.getId().toString() : null;
    }

    // Helper to get id as String in JSON output (matching frontend expecting string ids like 'r1')
    public String getIdString() {
        return id != null ? id.toString() : null;
    }
}
