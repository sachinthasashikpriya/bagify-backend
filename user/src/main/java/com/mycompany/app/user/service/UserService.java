package com.mycompany.app.user.service;

import com.mycompany.app.user.Exception.ResourceNotFoundException;
import com.mycompany.app.user.dto.*;
import com.mycompany.app.user.entity.Buyer;
import com.mycompany.app.user.entity.Role;
import com.mycompany.app.user.entity.Seller;
import com.mycompany.app.user.entity.User;
import com.mycompany.app.user.repository.UserRepository;
import com.mycompany.app.user.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public User register(RegisterRequest registerRequest) {

        if(userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Email address already in use");
        }

        if (registerRequest.getPassword() == null || registerRequest.getConfirmPassword() == null
                ||
        !registerRequest.getPassword().equals(registerRequest.getConfirmPassword())){
            throw new IllegalArgumentException("Passwords do not match");
        }
        System.out.println("UserType: " + registerRequest.getUserrole());


        Role role = switch (registerRequest.getUserrole().toLowerCase()){
            case "buyer" -> Role.BUYER;
            case "seller" -> Role.SELLER;
            default -> throw new IllegalArgumentException("Invalid user type");
        };

        if(role == Role.BUYER){

            Buyer buyer = new Buyer();
            buyer.setName(registerRequest.getName());
            buyer.setEmail(registerRequest.getEmail());
            buyer.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            buyer.setRole(role);

            return userRepository.save(buyer);
        }

        if(role == Role.SELLER){

            Seller seller = new Seller();
            seller.setName(registerRequest.getName());
            seller.setEmail(registerRequest.getEmail());
            seller.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            seller.setRole(role);

            return userRepository.save(seller);
        }

        throw new IllegalArgumentException("Invalid role");
    }



    public AuthResponse authenticate(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String token = jwtUtil.generateToken(user);

        return new AuthResponse(token, user);
    }

    public UserProfileResponse getProfileByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }


    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid email"));
    }
    public List<User> getAll() {
        return userRepository.findAll();
    }

    // ✅ Correct — use userId from JWT for fast lookup
    public User updateProfile(Integer userId, String currentEmail, UpdateUserRequest request) throws AccessDeniedException {

        User user = userRepository.findById(userId)  // ✅ fast primary key lookup
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // ✅ Extra security — verify the userId actually belongs to this email
        // Prevents any mismatch or token manipulation edge cases
        if (!user.getEmail().equalsIgnoreCase(currentEmail)) {
            throw new AccessDeniedException("Token identity mismatch");
        }

        // Update name
        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }

        // Update phone
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            user.setPhone(request.getPhone());
        }

        // Update address
        if (request.getAddress() != null && !request.getAddress().isBlank()) {
            user.setAddress(request.getAddress());
        }

        // Update email
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (!request.getEmail().equalsIgnoreCase(currentEmail)) {
                if (userRepository.existsByEmail(request.getEmail())) {
                    throw new IllegalArgumentException("Email is already in use");
                }
                user.setEmail(request.getEmail());
            }
        }

        // Update profile image
        if (request.getProfileImageUrl() != null && !request.getProfileImageUrl().isBlank()) {
            user.setProfileImageUrl(request.getProfileImageUrl());
        }

        // Update password
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                throw new IllegalArgumentException("Passwords do not match");
            }
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return userRepository.save(user);
    }

    public void disableUser(int id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Invalid user"));

        user.setEnabled(false);
        userRepository.save(user);
    }

    public void changePassword(Integer userId, UpdateUserRequest request) {
        // 1. Basic validation
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("New password is required");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        if (request.getPassword().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }

        // 2. Fetch user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 3. Verify current password
        if (request.getCurrentPassword() == null ||
            !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // 4. Update and save
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
    }

}
