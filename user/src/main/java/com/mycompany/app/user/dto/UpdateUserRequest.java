package com.mycompany.app.user.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String name;
    private String email;
    private String phone;
    private String address;
    private String ProfileImageUrl;
    private String password;
    private String confirmPassword;
}
