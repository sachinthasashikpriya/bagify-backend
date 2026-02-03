package com.mycompany.app.user.Controller;


import com.mycompany.app.user.dto.RegisterRequest;
import com.mycompany.app.user.dto.UpdateUserRequest;
import com.mycompany.app.user.entity.User;
import com.mycompany.app.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    @GetMapping("/me")
    public ResponseEntity<User> getMyProfile(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(userService.getByEmail(email));
    }


    //update logging user profile
    @PutMapping("/me")
    public ResponseEntity<User> updateMyProfile(
            Authentication authentication,
            @RequestBody UpdateUserRequest request
    ) {
        String email = authentication.getName();
        return ResponseEntity.ok(userService.updateProfile(email, request));
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

}
