package com.mycompany.app.order.repository;

import com.mycompany.app.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByBuyerIdOrderByCreatedAtDesc(Integer buyerId);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT o FROM Order o JOIN o.items i WHERE i.sellerId = :sellerId ORDER BY o.createdAt DESC")
    List<Order> findBySellerIdOrderByCreatedAtDesc(@org.springframework.data.repository.query.Param("sellerId") String sellerId);

    List<Order> findByStatusAndPaymentStatusAndCreatedAtBefore(
            Order.OrderStatus status,
            String paymentStatus,
            java.time.LocalDateTime dateTime
    );

    boolean existsByBuyerIdAndStatusIn(Integer buyerId, java.util.Collection<Order.OrderStatus> statuses);
}
