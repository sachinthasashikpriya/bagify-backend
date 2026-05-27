package com.mycompany.app.order.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Order entity — lives in bagify_orders database schema.
 * Note: productId, productName etc. are stored as a denormalized snapshot
 * (inside OrderItem) at write-time. We NEVER join across to the product
 * service database at read-time.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer buyerId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<OrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(nullable = false)
    private Double totalAmount;

    @Column(nullable = false)
    private String shippingAddress;

    @Column(name = "payment_status")
    private String paymentStatus = "UNPAID";

    @Column(name = "payment_id")
    private String paymentId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public enum OrderStatus {
        PENDING, PROCESSING, PARTIALLY_SHIPPED, SHIPPED, DELIVERED, CANCELLED
    }

    /**
     * Derives the consolidated parent-order status from individual item statuses.
     * Call this after any item status change and persist the result.
     *
     * Rules (industry standard):
     *  - If CANCELLED, stay CANCELLED
     *  - All items DELIVERED   → DELIVERED
     *  - All items SHIPPED+    → SHIPPED
     *  - Any item SHIPPED      → PARTIALLY_SHIPPED
     *  - Any item PROCESSING   → PROCESSING
     *  - Otherwise             → PENDING
     */
    public void computeStatus() {
        if (this.status == OrderStatus.CANCELLED) return;
        if (items == null || items.isEmpty()) return;

        long delivered = items.stream().filter(i -> i.getItemStatus() == OrderItem.ItemStatus.DELIVERED).count();
        long shipped   = items.stream().filter(i -> i.getItemStatus() == OrderItem.ItemStatus.SHIPPED).count();
        long processing= items.stream().filter(i -> i.getItemStatus() == OrderItem.ItemStatus.PROCESSING).count();
        int  total     = items.size();

        if (delivered == total) {
            this.status = OrderStatus.DELIVERED;
        } else if (shipped + delivered == total) {
            this.status = OrderStatus.SHIPPED;
        } else if (shipped > 0 || delivered > 0) {
            this.status = OrderStatus.PARTIALLY_SHIPPED;
        } else if (processing > 0) {
            this.status = OrderStatus.PROCESSING;
        } else {
            this.status = OrderStatus.PENDING;
        }
    }
}
