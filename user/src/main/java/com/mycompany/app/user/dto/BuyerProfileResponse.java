package com.mycompany.app.user.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BuyerProfileResponse extends UserProfileResponse {
    private Integer totalOrders;
    private BigDecimal totalSpent;

    public BuyerProfileResponse(Integer id, String name, String email, String phone, String address, String role, String profileImageUrl, LocalDateTime createdAt,
                                Integer totalOrders, BigDecimal totalSpent) {
        super(id, name, email, phone, address, role, profileImageUrl, createdAt);
        this.totalOrders = totalOrders;
        this.totalSpent = totalSpent;
    }
}
