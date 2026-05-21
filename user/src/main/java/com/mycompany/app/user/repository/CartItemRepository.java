package com.mycompany.app.user.repository;

import com.mycompany.app.user.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}
