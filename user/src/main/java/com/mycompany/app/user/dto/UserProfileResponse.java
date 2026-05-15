package com.mycompany.app.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponse {
    private Integer id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String role;
    private String profileImageUrl;
    private LocalDateTime createdAt;
}
