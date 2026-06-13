package com.mycompany.app.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BuyerInfoResponse {
    private Long id;
    private String name;
    private String email;
    private String address;
    private String phone;
}
