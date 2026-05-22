package com.mycompany.app.order.controller;

import com.mycompany.app.order.dto.CheckoutRequest;
import com.mycompany.app.order.dto.OrderResponse;
import com.mycompany.app.order.entity.Order;
import com.mycompany.app.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * POST /api/v1/orders/checkout
     * Places a new order from the buyer's cart items.
     * The buyer's JWT is forwarded to the product service for the deduct-stock call.
     */
    @PostMapping("/checkout")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<OrderResponse> checkout(
            @Valid @RequestBody CheckoutRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        Integer buyerId = (Integer) authentication.getDetails();
        if (buyerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Forward the JWT so product service accepts the authenticated deduct-stock call
        String bearerToken = httpRequest.getHeader("Authorization");

        OrderResponse response = orderService.placeOrder(buyerId, request, bearerToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/orders/my-orders
     * Returns the authenticated buyer's full order history.
     */
    @GetMapping("/my-orders")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<List<OrderResponse>> getMyOrders(Authentication authentication) {
        Integer buyerId = (Integer) authentication.getDetails();
        return ResponseEntity.ok(orderService.getOrdersByBuyer(buyerId));
    }

    /**
     * GET /api/v1/orders/{id}
     * Returns a single order. Buyers can only see their own; admins can see any.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('BUYER') or hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable Long id,
            Authentication authentication) {

        Integer buyerId = (Integer) authentication.getDetails();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        return ResponseEntity.ok(orderService.getOrderById(id, buyerId, isAdmin));
    }

    /**
     * PATCH /api/v1/orders/{id}/status
     * Updates order status. Restricted to SELLER and ADMIN.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam Order.OrderStatus status) {

        return ResponseEntity.ok(orderService.updateStatus(id, status));
    }
}
