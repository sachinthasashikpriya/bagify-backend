package com.mycompany.app.order.controller;

import com.mycompany.app.order.dto.CheckoutRequest;
import com.mycompany.app.order.dto.OrderResponse;
import com.mycompany.app.order.dto.PayHereParamsResponse;
import com.mycompany.app.order.dto.SellerStatsResponse;
import com.mycompany.app.order.entity.Order;
import com.mycompany.app.order.entity.OrderItem;
import com.mycompany.app.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
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
     * GET /api/v1/orders
     * Returns the authenticated buyer's full order history.
     */
    @GetMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<List<OrderResponse>> getMyOrders(Authentication authentication) {
        Integer buyerId = (Integer) authentication.getDetails();
        return ResponseEntity.ok(orderService.getOrdersByBuyer(buyerId));
    }

    /**
     * GET /api/v1/orders/seller
     * Returns the authenticated seller's order history.
     */
    @GetMapping("/seller")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<List<OrderResponse>> getSellerOrders(Authentication authentication) {
        Integer sellerId = (Integer) authentication.getDetails();
        return ResponseEntity.ok(orderService.getOrdersBySeller(sellerId));
    }

    /**
     * GET /api/v1/orders/seller/stats
     * Returns the authenticated seller's revenue and item sold stats.
     */
    @GetMapping("/seller/stats")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<SellerStatsResponse> getSellerStats(Authentication authentication) {
        Integer sellerId = (Integer) authentication.getDetails();
        if (sellerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(orderService.getSellerStats(sellerId));
    }

    /**
     * GET /api/v1/orders/all
     * Returns all orders in the system. Restricted to ADMIN.
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
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
     * PUT /api/v1/orders/{id}/status
     * Updates order status. Restricted to SELLER and ADMIN.
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam Order.OrderStatus status,
            Authentication authentication) {

        Integer userId = (Integer) authentication.getDetails();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        return ResponseEntity.ok(orderService.updateStatus(id, status, userId, isAdmin));
    }

    /**
     * PUT /api/v1/orders/{id}/cancel
     * Cancels a PENDING order. Restricted to the owning BUYER.
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        Integer buyerId = (Integer) authentication.getDetails();
        if (buyerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String bearerToken = httpRequest.getHeader("Authorization");

        return ResponseEntity.ok(orderService.cancelOrder(id, buyerId, bearerToken));
    }
    /**
     * PUT /api/v1/orders/{orderId}/items/{itemId}/status
     * Updates a single item's fulfillment status — called by the owning SELLER.
     * Seller can set: PENDING, PROCESSING, PACKED, SHIPPED (not DELIVERED).
     * After this call the parent order status is auto-recomputed.
     */
    @PutMapping("/{orderId}/items/{itemId}/status")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<OrderResponse> updateItemStatus(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @RequestParam OrderItem.ItemStatus status,
            Authentication authentication) {

        Integer sellerId = (Integer) authentication.getDetails();
        if (sellerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(orderService.updateItemStatus(orderId, itemId, status, sellerId));
    }

    /**
     * PUT /api/v1/orders/{orderId}/items/{itemId}/status/admin
     * Admin override — can set any status including DELIVERED.
     */
    @PutMapping("/{orderId}/items/{itemId}/status/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateItemStatusAdmin(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @RequestParam OrderItem.ItemStatus status) {

        return ResponseEntity.ok(orderService.updateItemStatusAdmin(orderId, itemId, status));
    }

    /**
     * GET /api/v1/orders/has-purchased
     * Checks if a buyer has a DELIVERED order for a specific product.
     * Internal endpoint for product service or frontend.
     */
    @GetMapping("/has-purchased")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Boolean> hasPurchased(
            @RequestParam Long productId,
            Authentication authentication) {
        Integer buyerId = (Integer) authentication.getDetails();
        return ResponseEntity.ok(orderService.hasPurchased(buyerId, productId));
    }

    /**
     * GET /api/v1/orders/{id}/payment-params
     * Returns the PayHere Sandbox payment parameters (including generated MD5 signature hash)
     * for initiating a checkout payment. Restricted to the owning BUYER.
     */
    @GetMapping("/{id}/payment-params")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<PayHereParamsResponse> getPaymentParams(
            @PathVariable Long id,
            @RequestParam String returnUrl,
            @RequestParam String cancelUrl,
            Authentication authentication,
            HttpServletRequest request) {

        Integer buyerId = (Integer) authentication.getDetails();
        if (buyerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String bearerToken = request.getHeader("Authorization");
        PayHereParamsResponse response = orderService.getPaymentParams(id, buyerId, bearerToken, returnUrl, cancelUrl);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/orders/payment/notify
     * Server-to-server webhook notification callback from PayHere.
     * Consumes application/x-www-form-urlencoded params. Publicly accessible.
     */
    @PostMapping(value = "/payment/notify", consumes = org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> handlePaymentNotification(@RequestParam Map<String, String> params) {
        orderService.processPaymentNotification(params);
        return ResponseEntity.ok("Notification Processed Successfully");
    }

    /**
     * GET /api/v1/orders/buyer/has-active
     * Checks if the authenticated buyer has any ongoing (active) orders.
     */
    @GetMapping("/buyer/has-active")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Boolean> hasActiveOrders(Authentication authentication) {
        Integer buyerId = (Integer) authentication.getDetails();
        if (buyerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(orderService.hasActiveOrdersForBuyer(buyerId));
    }

    /**
     * GET /api/v1/orders/seller/has-active
     * Checks if the authenticated seller has any ongoing (active) deliveries.
     */
    @GetMapping("/seller/has-active")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Boolean> hasActiveDeliveries(Authentication authentication) {
        Integer sellerId = (Integer) authentication.getDetails();
        if (sellerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(orderService.hasActiveDeliveriesForSeller(sellerId));
    }

    /**
     * GET /api/v1/orders/users/{userId}/has-active
     * Checks if a specific buyer or seller has ongoing orders or deliveries.
     */
    @GetMapping("/users/{userId}/has-active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Boolean> hasActiveOrdersForUser(
            @PathVariable Integer userId,
            @RequestParam String role
    ) {
        boolean hasActive = false;
        if ("BUYER".equalsIgnoreCase(role)) {
            hasActive = orderService.hasActiveOrdersForBuyer(userId);
        } else if ("SELLER".equalsIgnoreCase(role)) {
            hasActive = orderService.hasActiveDeliveriesForSeller(userId);
        }
        return ResponseEntity.ok(hasActive);
    }
}
