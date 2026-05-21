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
}
