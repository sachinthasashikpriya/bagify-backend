package com.mycompany.app.product.service;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserClient {

    private final WebClient webClient;

    public UserClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://USER").build();
    }

    public Map<Integer, Boolean> getBatchVerifiedSellers(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            String idsParam = ids.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/users/sellers/batch-verified")
                            .queryParam("ids", idsParam)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<Integer, Boolean>>() {})
                    .block();
        } catch (Exception e) {
            System.err.println("Error communicating with user service: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    public void updateSellerRating(String sellerId, double rating) {
        if (sellerId == null || !sellerId.matches("\\d+")) {
            return;
        }
        try {
            webClient.put()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/users/sellers/" + sellerId + "/rating")
                            .queryParam("rating", rating)
                            .build())
                    .retrieve()
                    .toBodilessEntity()
                    .retry(3)
                    .block();
        } catch (Exception e) {
            System.err.println("Error updating seller rating in user service: " + e.getMessage());
        }
    }
}
