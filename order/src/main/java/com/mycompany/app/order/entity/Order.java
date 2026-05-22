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

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public enum OrderStatus {
        PENDING, SHIPPED, DELIVERED, CANCELLED
    }
}
