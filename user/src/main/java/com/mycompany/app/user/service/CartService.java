package com.mycompany.app.user.service;

import com.mycompany.app.user.dto.AddToCartRequest;
import com.mycompany.app.user.dto.ProductDto;
import com.mycompany.app.user.entity.Buyer;
import com.mycompany.app.user.entity.Cart;
import com.mycompany.app.user.entity.CartItem;
import com.mycompany.app.user.repository.BuyerRepository;
import com.mycompany.app.user.repository.CartRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import com.mycompany.app.user.dto.CartItemDto;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final BuyerRepository buyerRepository;
    private final RestTemplate restTemplate;

    public CartService(CartRepository cartRepository, BuyerRepository buyerRepository, RestTemplate restTemplate) {
        this.cartRepository = cartRepository;
        this.buyerRepository = buyerRepository;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public CartItem addToCart(int buyerId, AddToCartRequest request) {
        ProductDto product;
        try {
            product = restTemplate.getForObject("http://PRODUCT/api/v1/products/" + request.getProductId(), ProductDto.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error communicating with product service", e);
        }

        if (product == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }

        Cart cart = cartRepository.findByBuyerId(buyerId).orElseGet(() -> {
            Buyer buyer = buyerRepository.findById(buyerId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Buyer not found"));
            Cart newCart = new Cart();
            newCart.setBuyer(buyer);
            return cartRepository.save(newCart);
        });

        Optional<CartItem> existingItemOpt = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst();

        int currentQuantity = existingItemOpt.map(CartItem::getQuantity).orElse(0);
        int newQuantity = currentQuantity + request.getQuantity();

        if (newQuantity > product.getStock()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Requested quantity exceeds available stock");
        }

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            existingItem.setQuantity(newQuantity);
            return existingItem;
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProductId(request.getProductId());
            newItem.setQuantity(request.getQuantity());
            newItem.setAddedAt(LocalDateTime.now());
            cart.getItems().add(newItem);
            return newItem;
        }
    }

    @Transactional
    public void updateQuantity(int buyerId, Long productId, com.mycompany.app.user.dto.UpdateCartItemRequest request) {
        Cart cart = cartRepository.findByBuyerId(buyerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));

        Optional<CartItem> existingItemOpt = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();

        if (existingItemOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found in cart");
        }

        CartItem existingItem = existingItemOpt.get();

        if (request.getQuantity() <= 0) {
            cart.getItems().remove(existingItem);
            return;
        }

        ProductDto product;
        try {
            product = restTemplate.getForObject("http://PRODUCT/api/v1/products/" + productId, ProductDto.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error communicating with product service", e);
        }

        if (product == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }

        if (request.getQuantity() > product.getStock()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Requested quantity exceeds available stock");
        }

        existingItem.setQuantity(request.getQuantity());
    }

    @Transactional
    public void removeFromCart(int buyerId, Long productId) {
        Cart cart = cartRepository.findByBuyerId(buyerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));

        Optional<CartItem> existingItemOpt = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();

        if (existingItemOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found in cart");
        }

        cart.getItems().remove(existingItemOpt.get());
    }

    @Transactional(readOnly = true)
    public List<CartItemDto> getCartItems(int buyerId) {
        Optional<Cart> cartOpt = cartRepository.findByBuyerId(buyerId);
        if (cartOpt.isEmpty()) {
            return Collections.emptyList();
        }

        Cart cart = cartOpt.get();
        List<CartItemDto> result = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            try {
                ProductDto product = restTemplate.getForObject("http://PRODUCT/api/v1/products/" + item.getProductId(), ProductDto.class);
                if (product != null) {
                    CartItemDto dto = new CartItemDto();
                    dto.setId(item.getId());
                    dto.setProductId(item.getProductId());
                    dto.setProductName(product.getName());
                    dto.setProductImage(product.getImage());
                    dto.setPrice(product.getPrice());
                    dto.setStock(product.getStock());
                    dto.setQuantity(item.getQuantity());
                    result.add(dto);
                }
            } catch (Exception e) {
                System.err.println("Failed to fetch product " + item.getProductId() + ": " + e.getMessage());
            }
        }
        return result;
    }
}
