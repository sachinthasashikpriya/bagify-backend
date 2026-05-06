package com.mycompany.app.user.Controller;


import com.mycompany.app.user.dto.RegisterRequest;
import com.mycompany.app.user.dto.UpdateUserRequest;
import com.mycompany.app.user.entity.User;
import com.mycompany.app.user.service.UserService;
import com.mycompany.app.user.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping("api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    @GetMapping("/me")
    public ResponseEntity<User> getMyProfile(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(userService.getByEmail(email));
    }


    @PutMapping("/profile")
    public ResponseEntity<User> updateProfile(
            Authentication authentication,
            @RequestBody UpdateUserRequest request
    ) throws AccessDeniedException {

        String  currentEmail = authentication.getName(); // ✅ from principal
        Integer userId       = (Integer) ((UsernamePasswordAuthenticationToken)
                authentication).getDetails(); // ✅ from details

        return ResponseEntity.ok(userService.updateProfile(userId, currentEmail, request));
    }

    //Admin: get all users
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAll());
    }

    //Admin: disable user
    @PutMapping("/{id}/disable")
    public ResponseEntity<String> disableUser(@PathVariable int id) {
        userService.disableUser(id);
        return ResponseEntity.ok("User disabled successfully");
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            Authentication authentication,
            @RequestBody UpdateUserRequest request
    ) {
        Integer userId = (Integer) ((UsernamePasswordAuthenticationToken) authentication).getDetails();
        userService.changePassword(userId, request);
        return ResponseEntity.ok("Password changed successfully");
    }

}
