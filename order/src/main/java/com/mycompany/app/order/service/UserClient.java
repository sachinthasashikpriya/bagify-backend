package com.mycompany.app.order.service;

import com.mycompany.app.order.dto.BuyerInfoResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient-based HTTP client for the user service.
 * Uses Eureka load balancing via lb://USER.
 */
@Service
public class UserClient {

    private final WebClient webClient;

    public UserClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://USER").build();
    }

    /**
     * Fetches buyer details by ID from the user service.
     */
    public BuyerInfoResponse getBuyerById(Integer buyerId, String bearerToken) {
        try {
            return webClient.get()
                    .uri("/api/v1/users/buyers/{id}", buyerId)
                    .header("Authorization", bearerToken)
                    .retrieve()
                    .bodyToMono(BuyerInfoResponse.class)
                    .block();
        } catch (Exception e) {
            System.err.println("Failed to fetch buyer details for ID " + buyerId + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Updates buyer statistics (totalSpent and totalOrders) in the user service.
     */
    public void updateBuyerStats(Integer buyerId, double spentDelta) {
        try {
            webClient.put()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/users/buyers/{id}/stats")
                            .queryParam("spentDelta", spentDelta)
                            .build(buyerId))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            System.err.println("Failed to update buyer stats for buyer ID " + buyerId + ": " + e.getMessage());
        }
    }

    /**
     * Updates seller statistics (itemsSold and revenue) in the user service.
     */
    public void updateSellerStats(Integer sellerId, double revenueDelta, int itemsSoldDelta) {
        try {
            webClient.put()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/users/sellers/{id}/stats")
                            .queryParam("revenueDelta", revenueDelta)
                            .queryParam("itemsSoldDelta", itemsSoldDelta)
                            .build(sellerId))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            // Log warning/error but don't fail the transaction if user service is temporarily unavailable
            System.err.println("Failed to update seller stats for seller ID " + sellerId + ": " + e.getMessage());
        }
    }
}
