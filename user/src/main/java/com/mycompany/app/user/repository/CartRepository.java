package com.mycompany.app.user.repository;

import com.mycompany.app.user.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByBuyerId(int buyerId);
}
