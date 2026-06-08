package com.mycompany.app.order.service;

import com.mycompany.app.order.dto.CheckoutRequest;
import com.mycompany.app.order.dto.OrderResponse;
import com.mycompany.app.order.dto.ProductDto;
import com.mycompany.app.order.dto.SellerStatsResponse;
import com.mycompany.app.order.entity.Order;
import com.mycompany.app.order.entity.OrderItem;
import com.mycompany.app.order.repository.OrderItemRepository;
import com.mycompany.app.order.repository.OrderRepository;
import com.mycompany.app.order.dto.PayHereParamsResponse;
import com.mycompany.app.order.util.PayHereSignatureGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    @Value("${payhere.merchant-id}")
    private String merchantId;

    @Value("${payhere.merchant-secret}")
    private String merchantSecret;

    @Value("${payhere.sandbox}")
    private boolean sandbox;

    @Value("${payhere.currency}")
    private String currency;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductClient productClient;
    private final UserClient userClient;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository, ProductClient productClient, UserClient userClient) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productClient = productClient;
        this.userClient = userClient;
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

    /** Computes total revenue and items sold statistics for a seller. */
    @Transactional(readOnly = true)
    public SellerStatsResponse getSellerStats(Integer sellerId) {
        List<OrderItem> sellerItems = orderItemRepository.findBySellerId(String.valueOf(sellerId));

        List<OrderItem> soldItems = sellerItems.stream()
                .filter(item -> "PAID".equals(item.getOrder().getPaymentStatus()) && item.getOrder().getStatus() != Order.OrderStatus.CANCELLED)
                .toList();

        double totalRevenue = soldItems.stream()
                .mapToDouble(item -> item.getPriceAtPurchase() * item.getQuantity())
                .sum();

        int totalItemsSold = soldItems.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();

        return new SellerStatsResponse(totalRevenue, totalItemsSold);
    }

    /**
     * Retrieves PayHere parameters and calculated signature hash for a specific order.
     */
    @Transactional(readOnly = true)
    public PayHereParamsResponse getPaymentParams(Long orderId, Integer buyerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getBuyerId().equals(buyerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: you are not the owner of this order");
        }

        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot pay for a cancelled order");
        }

        // Format amount precisely to 2 decimal places (using US locale to ensure dot decimal separator)
        String formattedAmount = String.format(java.util.Locale.US, "%.2f", order.getTotalAmount());

        // Generate the MD5 hash signature
        String hash = PayHereSignatureGenerator.generateCheckoutHash(
                merchantId,
                String.valueOf(orderId),
                formattedAmount,
                currency,
                merchantSecret
        );

        return PayHereParamsResponse.builder()
                .merchantId(merchantId)
                .orderId(String.valueOf(orderId))
                .amount(formattedAmount)
                .currency(currency)
                .hash(hash)
                .sandbox(sandbox)
                .build();
    }

    /**
     * Processes server-to-server webhook notification callback from PayHere.
     */
    @Transactional
    public void processPaymentNotification(java.util.Map<String, String> params) {
        String receivedMerchantId = params.get("merchant_id");
        String receivedOrderIdStr = params.get("order_id");
        String receivedAmountStr = params.get("payhere_amount");
        String receivedCurrency = params.get("payhere_currency");
        String receivedStatusCode = params.get("status_code");
        String receivedMd5sig = params.get("md5sig");
        String receivedPaymentId = params.get("payment_id");

        if (receivedMerchantId == null || receivedOrderIdStr == null || receivedAmountStr == null 
                || receivedCurrency == null || receivedStatusCode == null || receivedMd5sig == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required PayHere parameters");
        }

        // 1. Verify that the request is authentic by matching signatures
        String calculatedMd5sig = PayHereSignatureGenerator.generateNotificationHash(
                receivedMerchantId,
                receivedOrderIdStr,
                receivedAmountStr,
                receivedCurrency,
                receivedStatusCode,
                merchantSecret
        );

        if (!calculatedMd5sig.equalsIgnoreCase(receivedMd5sig)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid signature verification failed");
        }

        // 2. Fetch the corresponding order
        Long orderId;
        try {
            orderId = Long.parseLong(receivedOrderIdStr);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order ID format");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        // 3. Process payment status
        if ("2".equals(receivedStatusCode)) {
            // Payment success: transition order paymentStatus to PAID and overall status to PROCESSING
            boolean isAlreadyPaid = "PAID".equals(order.getPaymentStatus());
            order.setPaymentStatus("PAID");
            order.setPaymentId(receivedPaymentId);
            
            // Only update status to PROCESSING if it's currently PENDING
            if (order.getStatus() == Order.OrderStatus.PENDING) {
                order.setStatus(Order.OrderStatus.PROCESSING);
                
                // Also update individual item statuses to PROCESSING so sellers know payment is received
                if (order.getItems() != null) {
                    for (OrderItem item : order.getItems()) {
                        if (item.getItemStatus() == OrderItem.ItemStatus.PENDING) {
                            item.setItemStatus(OrderItem.ItemStatus.PROCESSING);
                        }
                    }
                }
            }
            orderRepository.save(order);

            // Update seller stats in user microservice if order wasn't paid already
            if (!isAlreadyPaid) {
                if (order.getItems() != null) {
                    for (OrderItem item : order.getItems()) {
                        try {
                            Integer sellerId = Integer.parseInt(item.getSellerId());
                            double itemRevenue = item.getPriceAtPurchase() * item.getQuantity();
                            int itemQuantity = item.getQuantity();
                            userClient.updateSellerStats(sellerId, itemRevenue, itemQuantity);
                        } catch (Exception e) {
                            System.err.println("Could not parse seller ID or call UserClient: " + e.getMessage());
                        }
                    }
                }
            }
        } else if ("0".equals(receivedStatusCode)) {
            // Payment pending
            order.setPaymentStatus("PENDING");
            orderRepository.save(order);
        } else {
            // Payment failed or declined (status_code < 0)
            order.setPaymentStatus("FAILED");
            orderRepository.save(order);
        }
    }

    @Transactional(readOnly = true)
    public boolean hasActiveOrdersForBuyer(Integer buyerId) {
        List<Order.OrderStatus> activeStatuses = List.of(
                Order.OrderStatus.PENDING,
                Order.OrderStatus.PROCESSING,
                Order.OrderStatus.PARTIALLY_SHIPPED,
                Order.OrderStatus.SHIPPED
        );
        return orderRepository.existsByBuyerIdAndStatusIn(buyerId, activeStatuses);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveDeliveriesForSeller(Integer sellerId) {
        return orderItemRepository.existsActiveDeliveriesBySellerId(
                String.valueOf(sellerId),
                OrderItem.ItemStatus.DELIVERED,
                Order.OrderStatus.CANCELLED
        );
    }
}
