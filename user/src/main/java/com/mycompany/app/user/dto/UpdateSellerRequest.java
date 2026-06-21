package com.mycompany.app.user.dto;

import lombok.Data;
@Data
public class UpdateSellerRequest {

    private String businessName;
    private String registrationNumber;
    private String NICNumber;

    // getters and setters
}