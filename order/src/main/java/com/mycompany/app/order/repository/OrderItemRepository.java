package com.mycompany.app.order.repository;

import com.mycompany.app.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    /** Find all items owned by a specific seller across all orders. */
    List<OrderItem> findBySellerId(String sellerId);
}
