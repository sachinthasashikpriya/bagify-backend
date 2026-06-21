package com.mycompany.app.product.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderClient {

    private final WebClient webClient;

    public OrderClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://ORDER").build();
    }

    /**
     * Checks if a buyer has purchased and received a specific product.
     */
    public boolean hasPurchased(Long productId, String bearerToken) {
        try {
            Boolean result = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/orders/has-purchased")
                            .queryParam("productId", productId)
                            .build())
                    .header("Authorization", bearerToken)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();
            return result != null && result;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error communicating with order service: " + e.getMessage());
        }
    }
}
