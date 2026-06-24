package com.mycompany.app.user.controller;

import com.mycompany.app.user.dto.AuthResponse;
import com.mycompany.app.user.dto.ForgotPasswordRequest;
import com.mycompany.app.user.dto.LoginRequest;
import com.mycompany.app.user.dto.RegisterRequest;
import com.mycompany.app.user.dto.ResetPasswordRequest;
import com.mycompany.app.user.dto.TokenRefreshRequest;
import com.mycompany.app.user.dto.UserProfileResponse;
import com.mycompany.app.user.dto.VerifyOtpRequest;
import com.mycompany.app.user.entity.User;
import com.mycompany.app.user.service.UserService;
import com.mycompany.app.user.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Value("${jwt.cookie.same-site:Lax}")
    private String cookieSameSite;

    @Value("${jwt.cookie.secure:true}")
    private boolean cookieSecure;

    @PostMapping("/register")
    public ResponseEntity<UserProfileResponse> register(@RequestBody @Valid  RegisterRequest registerRequest) {
        User user = userService.register(registerRequest);
        return ResponseEntity.ok(userService.mapToProfileResponse(user));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@RequestBody @Valid VerifyOtpRequest verifyOtpRequest, HttpServletResponse response) {
        AuthResponse authResponse = userService.verifyOtp(verifyOtpRequest);
        
        ResponseCookie cookie = ResponseCookie.from("refresh_token", authResponse.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .path("/api/v1/auth")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(new AuthResponse(authResponse.getToken(), null, authResponse.getUser()));
    }
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        AuthResponse authResponse = userService.authenticate(loginRequest);
        
        ResponseCookie cookie = ResponseCookie.from("refresh_token", authResponse.getRefreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/v1/auth")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite(cookieSameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(new AuthResponse(authResponse.getToken(), null, authResponse.getUser()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshTokenFromCookie,
            @RequestBody(required = false) TokenRefreshRequest refreshRequest,
            HttpServletResponse response
    ) {
        String tokenToUse = refreshTokenFromCookie;
        if (tokenToUse == null || tokenToUse.isBlank()) {
            if (refreshRequest != null) {
                tokenToUse = refreshRequest.getRefreshToken();
            }
        }

        if (tokenToUse == null || tokenToUse.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AuthResponse authResponse = userService.refreshToken(tokenToUse);
        
        ResponseCookie cookie = ResponseCookie.from("refresh_token", authResponse.getRefreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/v1/auth")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite(cookieSameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(new AuthResponse(authResponse.getToken(), null, authResponse.getUser()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/v1/auth")
                .maxAge(0)
                .sameSite(cookieSameSite)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        userService.forgotPassword(request);
        return ResponseEntity.ok("If an account exists with that email, a reset link has been sent.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        userService.resetPassword(request);
        return ResponseEntity.ok("Password has been reset successfully.");
    }
}
