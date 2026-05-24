package com.mycompany.app.order.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Denormalized snapshot of a product at purchase time. productName, imageUrl etc. are stored
 * here so the order history is accurate even after product changes. NEVER join to product DB.
 *
 * Per-item fulfillment status — allows each seller to manage their own items independently.
 * The parent Order.status is DERIVED from all items via Order.computeStatus().
 *
 * Lifecycle (seller controls):
 *   PENDING → PROCESSING → PACKED → SHIPPED
 * System/Admin controls:
 *   SHIPPED → DELIVERED
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

    @Column(name = "seller_id")
    private String sellerId;

    /**
     * Per-item fulfillment status controlled by the owning seller.
     * Industry pattern: denormalized fulfillment snapshot per order line.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @org.hibernate.annotations.ColumnDefault("'PENDING'")
    private ItemStatus itemStatus = ItemStatus.PENDING;

    public enum ItemStatus {
        PENDING, PROCESSING, PACKED, SHIPPED, DELIVERED
    }
}
