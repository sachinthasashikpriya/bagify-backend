package com.mycompany.app.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Minimal buyer info exposed to other services (e.g. order service needs shipping address).
 * Only contains fields needed by consumers — not the full Buyer entity.
 */
@Getter
@AllArgsConstructor
public class BuyerInfoResponse {
    private Long id;
    private String name;
    private String email;
    private String address;
}
