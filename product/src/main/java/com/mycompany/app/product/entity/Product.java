package com.mycompany.app.product.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(length = 2000)
    private String description;

    private double price;
    private int stock;
    private String category;

    @Column(length = 1000)
    private String image;

    private String sellerId;
    private String sellerName;
    private double sellerRating;
    private double averageRating;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    public void addReview(Review review) {
        reviews.add(review);
        review.setProduct(this);
        calculateAverageRating();
    }

    public void calculateAverageRating() {
        if (reviews.isEmpty()) {
            this.averageRating = 0.0;
        } else {
            double sum = 0;
            for (Review r : reviews) {
                sum += r.getRating();
            }
            this.averageRating = Math.round((sum / reviews.size()) * 10.0) / 10.0;
        }
    }
}
