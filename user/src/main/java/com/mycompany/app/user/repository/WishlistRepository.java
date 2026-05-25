package com.mycompany.app.user.repository;

import com.mycompany.app.user.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    Optional<Wishlist> findByBuyerId(int buyerId);
}
