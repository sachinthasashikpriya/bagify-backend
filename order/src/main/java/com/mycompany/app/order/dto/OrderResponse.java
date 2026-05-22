package com.mycompany.app.order.dto;

import com.mycompany.app.order.entity.Order;
import com.mycompany.app.order.entity.OrderItem;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/** Full order response returned to the buyer and admin. */
@Getter
@Builder
public class OrderResponse {
    private Long id;
    private Integer buyerId;
    private String status;
    private Double totalAmount;
    private String shippingAddress;
    private LocalDateTime createdAt;
    private List<OrderItemDto> items;

    public static OrderResponse fromEntity(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .buyerId(order.getBuyerId())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .createdAt(order.getCreatedAt())
                .items(order.getItems().stream()
                        .map(OrderItemDto::fromEntity)
                        .collect(Collectors.toList()))
                .build();
    }

    @Getter
    @Builder
    public static class OrderItemDto {
        private Long id;
        private Long productId;
        private String productName;
        private String imageUrl;
        private Integer quantity;
        private Double priceAtPurchase;

        public static OrderItemDto fromEntity(OrderItem item) {
            return OrderItemDto.builder()
                    .id(item.getId())
                    .productId(item.getProductId())
                    .productName(item.getProductName())
                    .imageUrl(item.getImageUrl())
                    .quantity(item.getQuantity())
                    .priceAtPurchase(item.getPriceAtPurchase())
                    .build();
        }
    }
}
