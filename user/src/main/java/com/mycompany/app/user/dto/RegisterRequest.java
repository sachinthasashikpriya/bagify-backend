package com.mycompany.app.user.dto;

import com.mycompany.app.user.entity.Role;
import lombok.Data;

@Data
public class RegisterRequest {
    private String role;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String password;
    private String confirmPassword;


}
