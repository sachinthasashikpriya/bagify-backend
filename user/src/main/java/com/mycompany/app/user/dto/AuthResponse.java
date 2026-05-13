package com.mycompany.app.user.dto;

import com.mycompany.app.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data

public class AuthResponse {

    private String token;
    private String refreshToken;
    private User user;

    public AuthResponse(String token, String refreshToken, User user) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.user = user;
    }

    public String getToken() { return token; }
    public String getRefreshToken() { return refreshToken; }
    public User getUser() { return user; }
}