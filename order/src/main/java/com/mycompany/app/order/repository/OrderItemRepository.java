package com.mycompany.app.order.repository;

import com.mycompany.app.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    /** Find all items owned by a specific seller across all orders. */
    List<OrderItem> findBySellerId(String sellerId);

    /** Find all items owned by a specific seller with a specific status */
    List<OrderItem> findBySellerIdAndItemStatus(String sellerId, OrderItem.ItemStatus itemStatus);

    /** Check if a buyer has purchased a product with a specific item status */
    boolean existsByOrderBuyerIdAndProductIdAndItemStatus(Integer buyerId, Long productId, OrderItem.ItemStatus itemStatus);
}
