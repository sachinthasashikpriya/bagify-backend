package com.mycompany.app.user.Controller;

import com.mycompany.app.user.dto.AuthResponse;
import com.mycompany.app.user.dto.LoginRequest;
import com.mycompany.app.user.dto.RegisterRequest;
import com.mycompany.app.user.entity.User;
import com.mycompany.app.user.service.UserService;
import com.mycompany.app.user.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        User user = userService.register(registerRequest);
        return ResponseEntity.ok(user);
    }
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest) {
        User user = userService.authenticate(loginRequest);
        String token = jwtUtil.generateToken(user);
        return ResponseEntity.ok(new AuthResponse(token));
    }

}
