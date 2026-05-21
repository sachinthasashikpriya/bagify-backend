package com.mycompany.app.user.Controller;

import com.mycompany.app.user.dto.OrderRequest;
import com.mycompany.app.user.entity.Order;
import com.mycompany.app.user.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Order> placeOrder(@RequestBody OrderRequest request, Authentication authentication) {
        Integer buyerId = (Integer) authentication.getDetails();
        if (buyerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Order order = orderService.placeOrder(buyerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
}
