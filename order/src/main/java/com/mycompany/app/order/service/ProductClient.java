package com.mycompany.app.order.service;

import com.mycompany.app.order.dto.ProductDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/**
 * WebClient-based HTTP client for the product service.
 * Uses Eureka load balancing via lb://PRODUCT.
 * Replaces the deprecated RestTemplate pattern.
 */
@Service
public class ProductClient {

    private final WebClient webClient;

    public ProductClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://PRODUCT").build();
    }

    /** Fetch product details. Returns null if product not found. */
    public ProductDto getProduct(Long productId) {
        try {
            return webClient.get()
                    .uri("/api/v1/products/{id}", productId)
                    .retrieve()
                    .bodyToMono(ProductDto.class)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Product " + productId + " not found");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error communicating with product service: " + e.getMessage());
        }
    }

    /**
     * Atomically deduct stock on the product service.
     * The order service passes its JWT so the product service's deduct-stock
     * endpoint (which requires isAuthenticated()) accepts the call.
     */
    public void deductStock(Long productId, int quantity, String bearerToken) {
        try {
            webClient.post()
                    .uri("/api/v1/products/{id}/deduct-stock?quantity={q}", productId, quantity)
                    .header("Authorization", bearerToken)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException.Conflict e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Insufficient stock for product " + productId);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error deducting stock for product " + productId + ": " + e.getMessage());
        }
    }

    /**
     * Atomically restore stock on the product service.
     */
    public void restoreStock(Long productId, int quantity, String bearerToken) {
        try {
            webClient.post()
                    .uri("/api/v1/products/{id}/restore-stock?quantity={q}", productId, quantity)
                    .header("Authorization", bearerToken)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error restoring stock for product " + productId + ": " + e.getMessage());
        }
    }
}
