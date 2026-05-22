package com.mycompany.app.order.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Denormalized snapshot of a product at purchase time.
 * productName, imageUrl etc. are stored here so the order history is
 * always accurate even if the product is later renamed or deleted.
 * NEVER join to the product service DB — read from this snapshot.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonBackReference
    private Order order;

    @Column(nullable = false)
    private Long productId;

    /** Denormalized snapshot — stored at purchase time */
    @Column(nullable = false)
    private String productName;

    /** Denormalized snapshot — stored at purchase time */
    private String imageUrl;

    @Column(nullable = false)
    private Integer quantity;

    /** Price at the time of purchase — not the current product price */
    @Column(nullable = false)
    private Double priceAtPurchase;
}
