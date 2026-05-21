package com.mycompany.app.user.service;

import com.mycompany.app.user.dto.OrderRequest;
import com.mycompany.app.user.dto.ProductDto;
import com.mycompany.app.user.entity.Cart;
import com.mycompany.app.user.entity.CartItem;
import com.mycompany.app.user.entity.Order;
import com.mycompany.app.user.entity.OrderItem;
import com.mycompany.app.user.repository.CartRepository;
import com.mycompany.app.user.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final RestTemplate restTemplate;

    public OrderService(OrderRepository orderRepository, CartRepository cartRepository, RestTemplate restTemplate) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public Order placeOrder(int buyerId, OrderRequest request) {
        Cart cart = cartRepository.findByBuyerId(buyerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        // Validate stock
        List<CartItem> items = cart.getItems();
        List<ProductDto> products = new ArrayList<>();
        double totalAmount = 0;

        for (CartItem item : items) {
            ProductDto product;
            try {
                product = restTemplate.getForObject("http://PRODUCT/api/v1/products/" + item.getProductId(), ProductDto.class);
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error communicating with product service", e);
            }

            if (product == null || product.getStock() < item.getQuantity()) {
                String productName = product != null ? product.getName() : "Item " + item.getProductId();
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Item out of stock: " + productName);
            }
            products.add(product);
            totalAmount += product.getPrice() * item.getQuantity();
        }

        // Deduct stock
        for (CartItem item : items) {
            try {
                restTemplate.postForEntity("http://PRODUCT/api/v1/products/" + item.getProductId() + "/deduct-stock?quantity=" + item.getQuantity(), null, Void.class);
            } catch (HttpClientErrorException.Conflict e) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Item out of stock during checkout");
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deducting stock", e);
            }
        }

        // Create Order
        Order order = new Order();
        order.setBuyerId(buyerId);
        order.setShippingAddress(request.getShippingAddress());
        order.setStatus("PENDING");
        order.setTotalAmount(totalAmount);

        for (int i = 0; i < items.size(); i++) {
            CartItem cItem = items.get(i);
            ProductDto pDto = products.get(i);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(cItem.getProductId());
            orderItem.setProductName(pDto.getName());
            orderItem.setQuantity(cItem.getQuantity());
            orderItem.setPriceAtPurchase(pDto.getPrice());
            order.getItems().add(orderItem);
        }

        Order savedOrder = orderRepository.save(order);

        // Clear cart
        cart.getItems().clear();
        cartRepository.save(cart);

        return savedOrder;
    }
}
