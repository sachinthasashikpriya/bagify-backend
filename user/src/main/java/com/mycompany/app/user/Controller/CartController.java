package com.mycompany.app.user.Controller;

import com.mycompany.app.user.dto.AddToCartRequest;
import com.mycompany.app.user.entity.CartItem;
import com.mycompany.app.user.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/items")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<CartItem> addToCart(@Valid @RequestBody AddToCartRequest request, Authentication authentication) {
        Integer buyerId = (Integer) authentication.getDetails();
        CartItem item = cartService.addToCart(buyerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }
}
