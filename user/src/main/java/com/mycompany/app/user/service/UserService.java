package com.mycompany.app.user.service;

import com.mycompany.app.user.dto.*;
import com.mycompany.app.user.entity.Role;
import com.mycompany.app.user.entity.User;
import com.mycompany.app.user.repository.UserRepository;
import com.mycompany.app.user.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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


        Role role = switch (registerRequest.getUserType().toLowerCase()){
            case "buyer" -> Role.BUYER;
            case "seller" -> Role.SELLER;
            default -> throw new IllegalArgumentException("Invalid user type");
        };


        User user = new User();
        user.setName(registerRequest.getName());
        user.setEmail(registerRequest.getEmail());
        user.setPhone(registerRequest.getPhone());
        user.setAddress(registerRequest.getAddress());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRole(role);

        return userRepository.save(user);
    }

//    public User authenticate(LoginRequest loginRequest) {
//        User user = userRepository.findByEmail(loginRequest.getEmail())
//                .orElseThrow(() -> new RuntimeException("Invalid username"));
//
//        if(!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
//            throw new RuntimeException("Invalid password");
//        }
//
//        return user;
//    }
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

    public User updateProfile(String email, UpdateUserRequest updateUserRequest) {
        User user = getByEmail(email);

        if(updateUserRequest.getName() != null) {
            user.setName(updateUserRequest.getName());
        }

        if(updateUserRequest.getPassword() != null && !updateUserRequest.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(updateUserRequest.getPassword()));
        }

        return userRepository.save(user);
    }

    public void disableUser(int id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Invalid user"));

        user.setEnabled(false);
        userRepository.save(user);
    }

}
