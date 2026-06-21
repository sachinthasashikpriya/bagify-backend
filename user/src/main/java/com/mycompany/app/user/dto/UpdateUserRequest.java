package com.mycompany.app.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private String name;
    private String email;
    private String phone;
    private String address;
    
    private String profileImageUrl;

    private boolean profileImageUrlSet = false;

    @JsonProperty("profileImageUrl")
    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
        this.profileImageUrlSet = true;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public boolean isProfileImageUrlSet() {
        return profileImageUrlSet;
    }

    private String currentPassword;
    private String password;
    private String confirmPassword;
}
