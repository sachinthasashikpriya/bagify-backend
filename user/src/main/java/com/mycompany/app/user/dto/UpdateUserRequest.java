package com.mycompany.app.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private String name;
    private String email;
    private String phone;
    private String address;
    
    @JsonProperty("profileImageUrl")
    private String profileImageUrl;

    private String currentPassword;
    private String password;
    private String confirmPassword;
}
