package com.mycompany.app.user.controller;

import com.mycompany.app.user.dto.AddToCartRequest;
import com.mycompany.app.user.entity.CartItem;
import com.mycompany.app.user.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.mycompany.app.user.dto.CartItemDto;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<List<CartItemDto>> getCartItems(Authentication authentication) {
        Integer buyerId = (Integer) authentication.getDetails();
        List<CartItemDto> items = cartService.getCartItems(buyerId);
        return ResponseEntity.ok(items);
    }

    @PostMapping("/items")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<CartItem> addToCart(@Valid @RequestBody AddToCartRequest request, Authentication authentication) {
        Integer buyerId = (Integer) authentication.getDetails();
        CartItem item = cartService.addToCart(buyerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @PutMapping("/items/{productId}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Void> updateQuantity(
            @PathVariable Long productId,
            @Valid @RequestBody com.mycompany.app.user.dto.UpdateCartItemRequest request,
            Authentication authentication) {
        Integer buyerId = (Integer) authentication.getDetails();
        cartService.updateQuantity(buyerId, productId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/items/{productId}")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Void> removeFromCart(
            @PathVariable Long productId,
            Authentication authentication) {
        Integer buyerId = (Integer) authentication.getDetails();
        cartService.removeFromCart(buyerId, productId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        Integer buyerId = (Integer) authentication.getDetails();
        cartService.clearCart(buyerId);
        return ResponseEntity.ok().build();
    }
}
