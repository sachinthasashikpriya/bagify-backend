package com.mycompany.app.product.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "complaints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String buyerId;

    @Column(nullable = false)
    private String buyerName;

    @Column(nullable = false)
    private String sellerId;

    @Column(length = 2000, nullable = false)
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "complaint_images", joinColumns = @JoinColumn(name = "complaint_id"))
    @Column(name = "image_url")
    private List<String> images;

    @Column(nullable = false)
    private String status; // e.g. "PENDING", "RESOLVED"

    @Column(nullable = false)
    private String date; // LocalDate string
}
