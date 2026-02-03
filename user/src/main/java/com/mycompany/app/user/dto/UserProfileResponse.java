package com.mycompany.app.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserProfileResponse {
    private Integer id;
    private String name;
    private String email;
    private String role;
}
