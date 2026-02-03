package com.mycompany.app.user.dto;

import com.mycompany.app.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data

public class AuthResponse {

    private String token;
    private User user;

    public AuthResponse(String token, User user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() { return token; }
    public User getUser() { return user; }
}