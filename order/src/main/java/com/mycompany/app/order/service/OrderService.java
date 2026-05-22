package com.mycompany.app.order.service;

import com.mycompany.app.order.dto.CheckoutRequest;
import com.mycompany.app.order.dto.OrderResponse;
import com.mycompany.app.order.dto.ProductDto;
import com.mycompany.app.order.entity.Order;
import com.mycompany.app.order.entity.OrderItem;
import com.mycompany.app.order.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;

    public OrderService(OrderRepository orderRepository, ProductClient productClient) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
    }

    /**
     * Places a new order.
     *
     * Flow:
     * 1. Validate all items against the product service (stock check)
     * 2. Deduct stock atomically for each item
     * 3. Persist the order with denormalized product snapshots
     *
     * @param buyerId     resolved from JWT in the controller
     * @param request     checkout payload (address + items)
     * @param bearerToken the buyer's JWT forwarded to product service for auth
     */
    @Transactional
    public OrderResponse placeOrder(Integer buyerId, CheckoutRequest request, String bearerToken) {
        List<CheckoutRequest.CheckoutItemDto> items = request.getItems();

        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order must contain at least one item");
        }

        // Phase 1 — Validate stock for all items before touching anything
        List<ProductDto> resolvedProducts = new ArrayList<>();
        double totalAmount = 0;

        for (CheckoutRequest.CheckoutItemDto item : items) {
            ProductDto product = productClient.getProduct(item.getProductId());

            if (product == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Product " + item.getProductId() + " not found");
            }
            if (product.getStock() < item.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Insufficient stock for: " + product.getName());
            }

            resolvedProducts.add(product);
            totalAmount += product.getPrice() * item.getQuantity();
        }

        // Phase 2 — Deduct stock (order service passes JWT to product service)
        for (int i = 0; i < items.size(); i++) {
            productClient.deductStock(items.get(i).getProductId(), items.get(i).getQuantity(), bearerToken);
        }

        // Phase 3 — Persist the order with denormalized snapshots
        Order order = new Order();
        order.setBuyerId(buyerId);
        order.setShippingAddress(request.getShippingAddress());
        order.setStatus(Order.OrderStatus.PENDING);
        order.setTotalAmount(totalAmount);

        for (int i = 0; i < items.size(); i++) {
            CheckoutRequest.CheckoutItemDto cartItem = items.get(i);
            ProductDto productSnapshot = resolvedProducts.get(i);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(cartItem.getProductId());
            // Denormalized snapshot — stored so order history is accurate even after product changes
            orderItem.setProductName(productSnapshot.getName());
            orderItem.setImageUrl(productSnapshot.getImage());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPriceAtPurchase(productSnapshot.getPrice());
            order.getItems().add(orderItem);
        }

        Order saved = orderRepository.save(order);
        return OrderResponse.fromEntity(saved);
    }

    /** Returns all orders for a buyer, newest first. */
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByBuyer(Integer buyerId) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId)
                .stream()
                .map(OrderResponse::fromEntity)
                .toList();
    }

    /** Returns a single order. Buyers can only see their own; admins can see any. */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Integer buyerId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!isAdmin && !order.getBuyerId().equals(buyerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return OrderResponse.fromEntity(order);
    }

    /** Updates order status — restricted to SELLER or ADMIN in the controller. */
    @Transactional
    public OrderResponse updateStatus(Long orderId, Order.OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        order.setStatus(newStatus);
        return OrderResponse.fromEntity(orderRepository.save(order));
    }
}
