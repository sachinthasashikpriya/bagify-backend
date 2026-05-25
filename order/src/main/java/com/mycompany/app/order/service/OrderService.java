package com.mycompany.app.order.service;

import com.mycompany.app.order.dto.CheckoutRequest;
import com.mycompany.app.order.dto.OrderResponse;
import com.mycompany.app.order.dto.ProductDto;
import com.mycompany.app.order.entity.Order;
import com.mycompany.app.order.entity.OrderItem;
import com.mycompany.app.order.repository.OrderItemRepository;
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
    private final OrderItemRepository orderItemRepository;
    private final ProductClient productClient;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository, ProductClient productClient) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
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
            orderItem.setSellerId(productSnapshot.getSellerId());
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

    /** Returns all orders containing items from a specific seller. */
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersBySeller(Integer sellerId) {
        return orderRepository.findBySellerIdOrderByCreatedAtDesc(String.valueOf(sellerId))
                .stream()
                .map(OrderResponse::fromEntity)
                .toList();
    }

    /** Returns all orders in the system. Restricted to ADMIN. */
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
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

    /** Updates order status — restricted to ADMIN only (global override). */
    @Transactional
    public OrderResponse updateStatus(Long orderId, Order.OrderStatus newStatus, Integer userId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can update the global order status");
        }

        order.setStatus(newStatus);
        return OrderResponse.fromEntity(orderRepository.save(order));
    }

    /**
     * Updates a single item's fulfillment status — called by the owning SELLER.
     * After updating, recomputes the parent order's consolidated status automatically.
     *
     * This is the core of the hybrid model:
     *   - Seller controls their own item status (PENDING → PROCESSING → PACKED → SHIPPED)
     *   - Parent order status is derived from all items (computeStatus)
     */
    @Transactional
    public OrderResponse updateItemStatus(Long orderId, Long itemId, OrderItem.ItemStatus newStatus, Integer sellerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot update items of a cancelled order");
        }

        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order item not found"));

        // Verify the item belongs to this order
        if (!item.getOrder().getId().equals(orderId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item does not belong to this order");
        }

        // Verify this seller owns the item
        if (!String.valueOf(sellerId).equals(item.getSellerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this item");
        }

        // Sellers cannot set DELIVERED — that's reserved for admin/system
        if (newStatus == OrderItem.ItemStatus.DELIVERED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can mark items as DELIVERED");
        }

        item.setItemStatus(newStatus);
        orderItemRepository.save(item);

        // Recompute consolidated parent order status
        order.computeStatus();
        return OrderResponse.fromEntity(orderRepository.save(order));
    }

    /**
     * Admin override — can update any item status including DELIVERED.
     */
    @Transactional
    public OrderResponse updateItemStatusAdmin(Long orderId, Long itemId, OrderItem.ItemStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot update items of a cancelled order");
        }

        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order item not found"));

        if (!item.getOrder().getId().equals(orderId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item does not belong to this order");
        }

        item.setItemStatus(newStatus);
        orderItemRepository.save(item);

        order.computeStatus();
        return OrderResponse.fromEntity(orderRepository.save(order));
    }

    /** Cancels a PENDING order and restores stock. */
    @Transactional
    public OrderResponse cancelOrder(Long orderId, Integer buyerId, String bearerToken) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getBuyerId().equals(buyerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PENDING orders can be cancelled");
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        // Restore stock for all items
        for (OrderItem item : order.getItems()) {
            productClient.restoreStock(item.getProductId(), item.getQuantity(), bearerToken);
        }

        return OrderResponse.fromEntity(saved);
    }

    /** Checks if a buyer has a DELIVERED order item for a specific product. */
    @Transactional(readOnly = true)
    public boolean hasPurchased(Integer buyerId, Long productId) {
        return orderItemRepository.existsByOrderBuyerIdAndProductIdAndItemStatus(
                buyerId, productId, OrderItem.ItemStatus.DELIVERED);
    }
}
