package com.mycompany.app.user.client;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class OrderClient {

    private final WebClient webClient;

    public OrderClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://ORDER").build();
    }

    public boolean hasActiveOrders(String bearerToken) {
        try {
            Boolean result = webClient.get()
                    .uri("/api/v1/orders/buyer/has-active")
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

    public boolean hasActiveDeliveries(String bearerToken) {
        try {
            Boolean result = webClient.get()
                    .uri("/api/v1/orders/seller/has-active")
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

    public boolean hasActiveOrdersForSpecificUser(Integer userId, String role, String bearerToken) {
        try {
            Boolean result = webClient.get()
                    .uri("/api/v1/orders/users/{userId}/has-active?role={role}", userId, role)
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
