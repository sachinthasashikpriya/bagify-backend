package com.mycompany.app.user.controller;

import com.mycompany.app.user.service.WishlistService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @PostMapping("/{productId}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Void> addToWishlist(@PathVariable Long productId, Authentication authentication) {
        Integer buyerId = (Integer) authentication.getDetails();
        wishlistService.addProductToWishlist(buyerId, productId);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{productId}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Void> removeFromWishlist(@PathVariable Long productId, Authentication authentication) {
        Integer buyerId = (Integer) authentication.getDetails();
        wishlistService.removeProductFromWishlist(buyerId, productId);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Set<Long>> getWishlist(Authentication authentication) {
        Integer buyerId = (Integer) authentication.getDetails();
        Set<Long> productIds = wishlistService.getWishlist(buyerId);
        return ResponseEntity.ok(productIds);
    }
}
