package com.mycompany.app.user.controller;


import com.mycompany.app.user.dto.RegisterRequest;
import com.mycompany.app.user.dto.UpdateUserRequest;
import com.mycompany.app.user.dto.VerificationRequest;
import com.mycompany.app.user.dto.UserProfileResponse;
import com.mycompany.app.user.dto.SellerInfoResponse;
import com.mycompany.app.user.dto.BuyerInfoResponse;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(userService.getProfileByEmail(email));
    }


    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            Authentication authentication,
            @RequestBody UpdateUserRequest request
    ) throws AccessDeniedException {

        String  currentEmail = authentication.getName(); // ✅ from principal
        Integer userId       = (Integer) ((UsernamePasswordAuthenticationToken)
                authentication).getDetails(); // ✅ from details

        return ResponseEntity.ok(userService.updateProfile(userId, currentEmail, request));
    }

    //Admin: get all users
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAll());
    }

    //Admin: disable user
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/disable")
    public ResponseEntity<String> disableUser(
            @PathVariable int id,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        String bearerToken = request.getHeader("Authorization");
        userService.disableUser(id, bearerToken);
        return ResponseEntity.ok("User disabled successfully");
    }

    //Admin: enable user
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/enable")
    public ResponseEntity<String> enableUser(@PathVariable int id) {
        userService.enableUser(id);
        return ResponseEntity.ok("User enabled successfully");
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

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(
            Authentication authentication,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        Integer userId = (Integer) ((UsernamePasswordAuthenticationToken) authentication).getDetails();
        String bearerToken = request.getHeader("Authorization");
        userService.deleteAccount(userId, bearerToken);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('SELLER')")
    @PostMapping("/profile/verification")
    public ResponseEntity<UserProfileResponse> submitVerification(
            Authentication authentication,
            @RequestBody VerificationRequest request
    ) {
        Integer userId = (Integer) ((UsernamePasswordAuthenticationToken) authentication).getDetails();
        return ResponseEntity.ok(userService.submitVerification(userId, request));
    }

    // ─── Internal service-to-service endpoints ─────────────────────────────────

    /**
     * Returns minimal seller info (name, businessName, rating) for the product service.
     * Called by the product service when a seller creates a product so real seller data is stored.
     * Requires authentication to prevent public crawling of seller data.
     */
    @GetMapping("/sellers/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SellerInfoResponse> getSellerById(@PathVariable Integer id) {
        return userService.findSellerById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Public batch-verified endpoint to check verification status of multiple sellers.
     */
    @GetMapping("/sellers/batch-verified")
    public ResponseEntity<java.util.Map<Integer, Boolean>> getBatchVerifiedSellers(@RequestParam List<Integer> ids) {
        return ResponseEntity.ok(userService.getBatchVerifiedSellers(ids));
    }

    /**
     * Returns minimal buyer info (name, email, address) for the order service.
     * Called by the order service to fetch the buyer's shipping address at checkout.
     * Requires authentication to prevent public data exposure.
     */
    @GetMapping("/buyers/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BuyerInfoResponse> getBuyerById(@PathVariable Integer id) {
        return userService.findBuyerById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/sellers/{id}/stats")
    public ResponseEntity<Void> updateSellerStats(
            @PathVariable Integer id,
            @RequestParam double revenueDelta,
            @RequestParam int itemsSoldDelta
    ) {
        userService.updateSellerStats(id, revenueDelta, itemsSoldDelta);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/sellers/{id}/rating")
    public ResponseEntity<Void> updateSellerRating(
            @PathVariable Integer id,
            @RequestParam float rating
    ) {
        userService.updateSellerRating(id, rating);
        return ResponseEntity.ok().build();
    }
}
