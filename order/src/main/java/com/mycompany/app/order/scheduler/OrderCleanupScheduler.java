package com.mycompany.app.order.scheduler;

import com.mycompany.app.order.entity.Order;
import com.mycompany.app.order.entity.OrderItem;
import com.mycompany.app.order.repository.OrderRepository;
import com.mycompany.app.order.service.ProductClient;
import com.mycompany.app.order.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class OrderCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderCleanupScheduler.class);

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final JwtUtil jwtUtil;

    public OrderCleanupScheduler(OrderRepository orderRepository, ProductClient productClient, JwtUtil jwtUtil) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Runs every 5 minutes.
     * Cancels any unpaid pending orders created more than 15 minutes ago,
     * and automatically restores their reserved stock items in the Product Service.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes in milliseconds
    @Transactional
    public void cleanupAbandonedOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);
        log.info("⏰ Starting scheduled cleanup of abandoned unpaid orders created before {}", cutoff);

        List<Order> abandonedOrders = orderRepository.findByStatusAndPaymentStatusAndCreatedAtBefore(
                Order.OrderStatus.PENDING,
                "UNPAID",
                cutoff
        );

        if (abandonedOrders.isEmpty()) {
            log.info("✅ No abandoned orders found.");
            return;
        }

        log.info("🔍 Found {} abandoned unpaid orders to cancel.", abandonedOrders.size());
        
        // Generate a microservice-to-microservice JWT credential
        String systemToken = "Bearer " + jwtUtil.generateSystemToken();

        for (Order order : abandonedOrders) {
            try {
                log.info("❌ Cancelling Order ID: {}", order.getId());
                order.setStatus(Order.OrderStatus.CANCELLED);
                
                // Restore stock for all items in the order
                if (order.getItems() != null) {
                    for (OrderItem item : order.getItems()) {
                        log.info("🔄 Restoring stock for Product ID: {}, Quantity: {}", 
                                item.getProductId(), item.getQuantity());
                        productClient.restoreStock(item.getProductId(), item.getQuantity(), systemToken);
                    }
                }
                
                orderRepository.save(order);
                log.info("🎉 Successfully cancelled Order ID: {} and restored its stock.", order.getId());
            } catch (Exception e) {
                log.error("💥 Failed to clean up Order ID: {}: {}", order.getId(), e.getMessage());
            }
        }
    }
}
