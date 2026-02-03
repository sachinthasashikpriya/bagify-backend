package com.mycompany.app.user.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String name;
    private String password;
}
