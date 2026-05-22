package com.mycompany.app.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Minimal seller info exposed to other services (e.g. product service).
 * Only contains fields needed by consumers — not the full Seller entity.
 */
@Getter
@AllArgsConstructor
public class SellerInfoResponse {
    private Long id;
    private String name;
    private String email;
    private String businessName;
    private Float rating;
}
