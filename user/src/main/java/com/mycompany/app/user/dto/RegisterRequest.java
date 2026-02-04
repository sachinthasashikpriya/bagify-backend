package com.mycompany.app.user.dto;

import com.mycompany.app.user.entity.Role;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class  RegisterRequest {

    private String role;

    @NotBlank
    private String name;

    @Email
    @NotBlank
    private String email;

    private String phone;
    private String address;

    @NotBlank
    private String password;

    @NotBlank
    private String confirmPassword;

    @NotBlank
    @Pattern(
            regexp = "buyer|seller",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "userType must be 'buyer' or 'seller'"
    )
    private String userrole;


    @AssertTrue(message = "Passwords do not match")
    public boolean isPasswordConfirmed() {
        return password != null && password.equals(confirmPassword);
    }



}
