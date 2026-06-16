package com.mycompany.app.order.service;

import com.mycompany.app.order.entity.Order;
import com.mycompany.app.order.entity.OrderItem;
import com.mycompany.app.order.repository.OrderItemRepository;
import com.mycompany.app.order.repository.OrderRepository;
import com.mycompany.app.order.util.PayHereSignatureGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceWebhookTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private OrderService orderService;

    private final String merchantId = "1235988";
    private final String merchantSecret = "MzI2OTAwOTA4NDM2NDMxOTMyOTIxMTAzNDY4ODExMTIzNDUxNzkwNg==";
    private final String currency = "LKR";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "merchantId", merchantId);
        ReflectionTestUtils.setField(orderService, "merchantSecret", merchantSecret);
        ReflectionTestUtils.setField(orderService, "currency", currency);
    }

    @Test
    void testProcessPaymentNotification_Success() {
        String orderId = "45";
        String amount = "250.00";
        String statusCode = "2";
        String paymentId = "pay_9999";

        String validSignature = PayHereSignatureGenerator.generateNotificationHash(
                merchantId, orderId, amount, currency, statusCode, merchantSecret
        );

        Map<String, String> params = new HashMap<>();
        params.put("merchant_id", merchantId);
        params.put("order_id", orderId);
        params.put("payhere_amount", amount);
        params.put("payhere_currency", currency);
        params.put("status_code", statusCode);
        params.put("md5sig", validSignature);
        params.put("payment_id", paymentId);

        Order order = new Order();
        order.setId(45L);
        order.setStatus(Order.OrderStatus.PENDING);
        order.setPaymentStatus("UNPAID");

        OrderItem item = new OrderItem();
        item.setSellerId("10");
        item.setPriceAtPurchase(25.0);
        item.setQuantity(10);
        item.setItemStatus(OrderItem.ItemStatus.PENDING);
        order.getItems().add(item);

        when(orderRepository.findByIdForUpdate(45L)).thenReturn(Optional.of(order));

        orderService.processPaymentNotification(params);

        assertEquals("PAID", order.getPaymentStatus());
        assertEquals("pay_9999", order.getPaymentId());
        assertEquals(Order.OrderStatus.PROCESSING, order.getStatus());
        assertEquals(OrderItem.ItemStatus.PROCESSING, item.getItemStatus());

        verify(orderRepository, times(1)).save(order);
        verify(userClient, times(1)).updateSellerStats(10, 250.0, 10);
    }

    @Test
    void testProcessPaymentNotification_CaseInsensitiveSignature() {
        String orderId = "45";
        String amount = "250.00";
        String statusCode = "2";
        String paymentId = "pay_9999";

        String validSignature = PayHereSignatureGenerator.generateNotificationHash(
                merchantId, orderId, amount, currency, statusCode, merchantSecret
        );
        // Convert to lowercase to test case insensitivity
        String lowercaseSignature = validSignature.toLowerCase();

        Map<String, String> params = new HashMap<>();
        params.put("merchant_id", merchantId);
        params.put("order_id", orderId);
        params.put("payhere_amount", amount);
        params.put("payhere_currency", currency);
        params.put("status_code", statusCode);
        params.put("md5sig", lowercaseSignature);
        params.put("payment_id", paymentId);

        Order order = new Order();
        order.setId(45L);
        order.setStatus(Order.OrderStatus.PENDING);
        order.setPaymentStatus("UNPAID");

        when(orderRepository.findByIdForUpdate(45L)).thenReturn(Optional.of(order));

        orderService.processPaymentNotification(params);

        assertEquals("PAID", order.getPaymentStatus());
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void testProcessPaymentNotification_TamperedAmount() {
        String orderId = "45";
        String amount = "250.00";
        String statusCode = "2";

        // Generate signature for 250.00
        String originalSignature = PayHereSignatureGenerator.generateNotificationHash(
                merchantId, orderId, amount, currency, statusCode, merchantSecret
        );

        // Tampered payload with altered amount (999.00) but keeping the original signature
        Map<String, String> params = new HashMap<>();
        params.put("merchant_id", merchantId);
        params.put("order_id", orderId);
        params.put("payhere_amount", "999.00");
        params.put("payhere_currency", currency);
        params.put("status_code", statusCode);
        params.put("md5sig", originalSignature);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            orderService.processPaymentNotification(params);
        });

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Invalid signature verification failed"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testProcessPaymentNotification_TamperedStatusCode() {
        String orderId = "45";
        String amount = "250.00";
        String statusCode = "2";

        // Generate signature for success status (2)
        String originalSignature = PayHereSignatureGenerator.generateNotificationHash(
                merchantId, orderId, amount, currency, statusCode, merchantSecret
        );

        // Tampered payload with altered status code (-1) but keeping the original signature
        Map<String, String> params = new HashMap<>();
        params.put("merchant_id", merchantId);
        params.put("order_id", orderId);
        params.put("payhere_amount", amount);
        params.put("payhere_currency", currency);
        params.put("status_code", "-1");
        params.put("md5sig", originalSignature);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            orderService.processPaymentNotification(params);
        });

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Invalid signature verification failed"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testProcessPaymentNotification_ArbitrarySignature() {
        Map<String, String> params = new HashMap<>();
        params.put("merchant_id", merchantId);
        params.put("order_id", "45");
        params.put("payhere_amount", "250.00");
        params.put("payhere_currency", currency);
        params.put("status_code", "2");
        params.put("md5sig", "some_completely_arbitrary_md5_signature");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            orderService.processPaymentNotification(params);
        });

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Invalid signature verification failed"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testProcessPaymentNotification_MissingParameters() {
        Map<String, String> params = new HashMap<>();
        params.put("merchant_id", merchantId);
        params.put("order_id", "45");
        // payhere_amount is missing!
        params.put("payhere_currency", currency);
        params.put("status_code", "2");
        params.put("md5sig", "some_sig");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            orderService.processPaymentNotification(params);
        });

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Missing required PayHere parameters"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testProcessPaymentNotification_NonExistentOrder() {
        String orderId = "999";
        String amount = "250.00";
        String statusCode = "2";

        String validSignature = PayHereSignatureGenerator.generateNotificationHash(
                merchantId, orderId, amount, currency, statusCode, merchantSecret
        );

        Map<String, String> params = new HashMap<>();
        params.put("merchant_id", merchantId);
        params.put("order_id", orderId);
        params.put("payhere_amount", amount);
        params.put("payhere_currency", currency);
        params.put("status_code", statusCode);
        params.put("md5sig", validSignature);

        when(orderRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            orderService.processPaymentNotification(params);
        });

        assertEquals(404, ex.getStatusCode().value());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testProcessPaymentNotification_NonNumericOrderId() {
        String orderId = "invalid_id";
        String amount = "250.00";
        String statusCode = "2";

        String validSignature = PayHereSignatureGenerator.generateNotificationHash(
                merchantId, orderId, amount, currency, statusCode, merchantSecret
        );

        Map<String, String> params = new HashMap<>();
        params.put("merchant_id", merchantId);
        params.put("order_id", orderId);
        params.put("payhere_amount", amount);
        params.put("payhere_currency", currency);
        params.put("status_code", statusCode);
        params.put("md5sig", validSignature);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            orderService.processPaymentNotification(params);
        });

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Invalid order ID format"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testProcessPaymentNotification_InactiveCancelledOrder() {
        String orderId = "45";
        String amount = "250.00";
        String statusCode = "2";

        String validSignature = PayHereSignatureGenerator.generateNotificationHash(
                merchantId, orderId, amount, currency, statusCode, merchantSecret
        );

        Map<String, String> params = new HashMap<>();
        params.put("merchant_id", merchantId);
        params.put("order_id", orderId);
        params.put("payhere_amount", amount);
        params.put("payhere_currency", currency);
        params.put("status_code", statusCode);
        params.put("md5sig", validSignature);

        Order order = new Order();
        order.setId(45L);
        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setPaymentStatus("UNPAID");

        when(orderRepository.findByIdForUpdate(45L)).thenReturn(Optional.of(order));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            orderService.processPaymentNotification(params);
        });

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Cannot process payment for a cancelled order"));
        verify(orderRepository, never()).save(any());
    }
}
