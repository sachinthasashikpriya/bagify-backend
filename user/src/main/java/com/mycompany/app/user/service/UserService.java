package com.mycompany.app.user.service;

import com.mycompany.app.user.exception.ResourceNotFoundException;
import com.mycompany.app.user.dto.*;
import com.mycompany.app.user.entity.Buyer;
import com.mycompany.app.user.entity.Role;
import com.mycompany.app.user.entity.Seller;
import com.mycompany.app.user.entity.User;
import com.mycompany.app.user.repository.BuyerRepository;
import com.mycompany.app.user.repository.SellerRepository;
import com.mycompany.app.user.repository.UserRepository;
import com.mycompany.app.user.repository.CartRepository;
import com.mycompany.app.user.repository.WishlistRepository;
import com.mycompany.app.user.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final BuyerRepository buyerRepository;
    private final CartRepository cartRepository;
    private final WishlistRepository wishlistRepository;
    private final com.mycompany.app.user.client.OrderClient orderClient;
    private final SseService sseService;

    @org.springframework.transaction.annotation.Transactional
    public User register(RegisterRequest registerRequest) {

        if(userRepository.existsByEmail(registerRequest.getEmail())) {
            User existingUser = userRepository.findByEmail(registerRequest.getEmail()).orElse(null);
            if (existingUser != null && !existingUser.isEnabled()) {
                userRepository.delete(existingUser);
                userRepository.flush();
            } else {
                throw new RuntimeException("Email address already in use");
            }
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

        String otp = String.format("%06d", new java.util.Random().nextInt(1000000));

        User userToSave;
        if(role == Role.BUYER){
            Buyer buyer = new Buyer();
            buyer.setName(registerRequest.getName());
            buyer.setEmail(registerRequest.getEmail());
            buyer.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            buyer.setPhone(registerRequest.getPhone());
            buyer.setAddress(registerRequest.getAddress());
            buyer.setRole(role);
            buyer.setEnabled(false);
            buyer.setVerificationCode(otp);
            buyer.setVerificationCodeExpiry(java.time.LocalDateTime.now().plusMinutes(10));
            userToSave = buyer;
        } else if(role == Role.SELLER){
            Seller seller = new Seller();
            seller.setName(registerRequest.getName());
            seller.setEmail(registerRequest.getEmail());
            seller.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            seller.setPhone(registerRequest.getPhone());
            seller.setAddress(registerRequest.getAddress());
            seller.setRole(role);
            seller.setEnabled(false);
            seller.setVerificationCode(otp);
            seller.setVerificationCodeExpiry(java.time.LocalDateTime.now().plusMinutes(10));
            userToSave = seller;
        } else {
            throw new IllegalArgumentException("Invalid role");
        }

        User savedUser = userRepository.save(userToSave);
        System.out.println("🔑 Generated OTP for " + savedUser.getEmail() + " is: " + otp);

        try {
            emailService.sendVerificationOtpEmail(savedUser.getEmail(), otp);
        } catch (Exception e) {
            System.err.println("Failed to send OTP email: " + e.getMessage());
            throw new RuntimeException("Failed to send verification email. Please check SMTP settings.");
        }

        return savedUser;
    }

    @org.springframework.transaction.annotation.Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isEnabled()) {
            throw new IllegalArgumentException("User account is already verified");
        }

        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(request.getCode())) {
            throw new IllegalArgumentException("Invalid verification code");
        }

        if (user.getVerificationCodeExpiry() == null || 
            user.getVerificationCodeExpiry().isBefore(java.time.LocalDateTime.now())) {
            throw new IllegalArgumentException("Verification code has expired");
        }

        user.setEnabled(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiry(null);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        return new AuthResponse(token, refreshToken, mapToProfileResponse(user));
    }



    public AuthResponse authenticate(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        if (!user.isEnabled()) {
            if (user.getVerificationCode() != null) {
                throw new IllegalArgumentException("User account is unverified. Please verify your email first.");
            }
            throw new IllegalArgumentException("User account is disabled");
        }

        String token = jwtUtil.generateToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        return new AuthResponse(token, refreshToken, mapToProfileResponse(user));
    }

    public AuthResponse refreshToken(TokenRefreshRequest request) {
        return refreshToken(request.getRefreshToken());
    }

    public AuthResponse refreshToken(String refreshToken) {
        String email = jwtUtil.extractEmail(refreshToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!jwtUtil.isTokenValid(refreshToken, user.getEmail())) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        if (!user.isEnabled()) {
            if (user.getVerificationCode() != null) {
                throw new IllegalArgumentException("User account is unverified. Please verify your email first.");
            }
            throw new IllegalArgumentException("User account is disabled");
        }

        String newToken = jwtUtil.generateToken(user);
        String newRefreshToken = jwtUtil.generateRefreshToken(user);

        return new AuthResponse(newToken, newRefreshToken, mapToProfileResponse(user));
    }

    public UserProfileResponse getProfileByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return mapToProfileResponse(user);
    }

    public UserProfileResponse mapToProfileResponse(User user) {
        if (user instanceof Seller) {
            Seller seller = (Seller) user;
            return new SellerProfileResponse(
                    seller.getId(),
                    seller.getName(),
                    seller.getEmail(),
                    seller.getPhone(),
                    seller.getAddress(),
                    seller.getRole().name(),
                    seller.getProfileImageUrl(),
                    seller.getCreatedAt(),
                    seller.getVerificationStatus(),
                    seller.getBusinessName(),
                    seller.getRegistrationNumber(),
                    seller.getNicImageUrl(),
                    seller.getBrCertificateUrl(),
                    seller.getRejectionReason(),
                    seller.getSubmittedAt(),
                    seller.getReviewedAt(),
                    seller.getItemsSold(),
                    seller.getRevenue()
            );
        }
        if (user instanceof Buyer) {
            Buyer buyer = (Buyer) user;
            return new BuyerProfileResponse(
                    buyer.getId(),
                    buyer.getName(),
                    buyer.getEmail(),
                    buyer.getPhone(),
                    buyer.getAddress(),
                    buyer.getRole().name(),
                    buyer.getProfileImageUrl(),
                    buyer.getCreatedAt(),
                    buyer.getTotalOrders(),
                    buyer.getTotalSpent()
            );
        }
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getAddress(),
                user.getRole().name(),
                user.getProfileImageUrl(),
                user.getCreatedAt()
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
    public UserProfileResponse updateProfile(Integer userId, String currentEmail, UpdateUserRequest request) throws AccessDeniedException {

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
        if (request.isProfileImageUrlSet()) {
            String newUrl = request.getProfileImageUrl();
            user.setProfileImageUrl((newUrl == null || newUrl.isBlank()) ? null : newUrl);
        }

        // Update password
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                throw new IllegalArgumentException("Passwords do not match");
            }
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        user = userRepository.save(user);
        return mapToProfileResponse(user);
    }

    public void disableUser(int id, String bearerToken) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Invalid user"));

        if (user instanceof Buyer) {
            if (orderClient.hasActiveOrdersForSpecificUser(user.getId(), "BUYER", bearerToken)) {
                throw new IllegalStateException("Cannot disable account: This buyer has ongoing orders that are not yet delivered or cancelled.");
            }
        } else if (user instanceof Seller) {
            if (orderClient.hasActiveOrdersForSpecificUser(user.getId(), "SELLER", bearerToken)) {
                throw new IllegalStateException("Cannot disable account: This seller has ongoing deliveries. Please fulfill or cancel them first.");
            }
        }

        user.setEnabled(false);
        userRepository.save(user);
    }

    public void enableUser(int id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Invalid user"));

        user.setEnabled(true);
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

    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("No account registered with this email address"));

        String token = java.util.UUID.randomUUID().toString();
        user.setPasswordResetToken(token);
        user.setPasswordResetTokenExpiry(java.time.LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        // In a real app, the base URL should be in properties
        String resetLink = "http://localhost:5173/reset-password?token=" + token;
        emailService.sendResetPasswordEmail(user.getEmail(), resetLink);
    }

    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        User user = userRepository.findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token"));

        if (user.getPasswordResetTokenExpiry().isBefore(java.time.LocalDateTime.now())) {
            throw new IllegalArgumentException("Token has expired");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteAccount(Integer userId, String bearerToken) {
        System.out.println("🗑 Deleting account for user ID: " + userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user instanceof Buyer) {
            if (orderClient.hasActiveOrders(bearerToken)) {
                throw new IllegalStateException("Cannot delete account: You have ongoing orders that are not yet delivered or cancelled.");
            }
            cartRepository.findByBuyerId(userId).ifPresent(cartRepository::delete);
            wishlistRepository.findByBuyerId(userId).ifPresent(wishlistRepository::delete);
        } else if (user instanceof Seller) {
            if (orderClient.hasActiveDeliveries(bearerToken)) {
                throw new IllegalStateException("Cannot delete account: You have ongoing deliveries. Please fulfill or cancel them first.");
            }
        }

        userRepository.delete(user);
        System.out.println("✅ Account deleted successfully");
    }

    public UserProfileResponse submitVerification(Integer userId, VerificationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!(user instanceof Seller)) {
            throw new IllegalArgumentException("Only sellers can submit verification");
        }

        Seller seller = (Seller) user;

        if (seller.getVerificationStatus() == Seller.VerificationStatus.APPROVED ||
                seller.getVerificationStatus() == Seller.VerificationStatus.PENDING) {
            throw new IllegalStateException("Verification is already approved or pending review");
        }

        seller.setBusinessName(request.getBusinessName());
        seller.setRegistrationNumber(request.getRegistrationNumber());
        seller.setBrCertificateUrl(request.getBrCertificateUrl());
        seller.setNicImageUrl(request.getNicImageUrl());
        seller.setVerificationStatus(Seller.VerificationStatus.PENDING);
        seller.setSubmittedAt(java.time.LocalDateTime.now());
        seller.setRejectionReason(null);

        seller = userRepository.save(seller);
        return mapToProfileResponse(seller);
    }

    // ─── Inter-service endpoints ───────────────────────────────────────────────

    /**
     * Returns minimal seller info for the product service.
     * Used when a seller creates a product and their real name/rating is needed.
     */
    public Optional<SellerInfoResponse> findSellerById(Integer sellerId) {
        return sellerRepository.findById(sellerId)
                .map(s -> new SellerInfoResponse(
                        (long) s.getId(),
                        s.getName(),
                        s.getEmail(),
                        s.getBusinessName(),
                        s.getRating()
                ));
    }

    /**
     * Returns minimal buyer info for the order service.
     * Used to pre-fill shipping address at checkout.
     */
    public Optional<BuyerInfoResponse> findBuyerById(Integer buyerId) {
        return buyerRepository.findById(buyerId)
                .map(b -> new BuyerInfoResponse(
                        (long) b.getId(),
                        b.getName(),
                        b.getEmail(),
                        b.getAddress(),
                        b.getPhone()
                ));
    }

    public List<UserProfileResponse> getPendingVerifications() {
        List<Seller> pendingSellers = sellerRepository.findAllByVerificationStatus(Seller.VerificationStatus.PENDING);
        return pendingSellers.stream()
                .map(this::mapToProfileResponse)
                .toList();
    }

    public UserProfileResponse reviewVerification(int sellerId, VerificationReviewRequest request) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found with ID: " + sellerId));

        String decision = request.getDecision();
        if (decision == null) {
            throw new IllegalArgumentException("Decision is required");
        }

        if (decision.equalsIgnoreCase("APPROVED")) {
            seller.setVerificationStatus(Seller.VerificationStatus.APPROVED);
            seller.setRejectionReason(null);
        } else if (decision.equalsIgnoreCase("REJECTED")) {
            if (request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
                throw new IllegalArgumentException("Rejection reason is required for REJECTED decision");
            }
            seller.setVerificationStatus(Seller.VerificationStatus.REJECTED);
            seller.setRejectionReason(request.getRejectionReason());
        } else {
            throw new IllegalArgumentException("Invalid decision: " + decision + ". Must be APPROVED or REJECTED.");
        }

        seller.setReviewedAt(java.time.LocalDateTime.now());
        Seller updatedSeller = sellerRepository.save(seller);

        // Notify the seller in real-time via SSE
        sseService.sendVerificationUpdate(sellerId, updatedSeller.getVerificationStatus().name());

        return mapToProfileResponse(updatedSeller);
    }

    public java.util.Map<Integer, Boolean> getBatchVerifiedSellers(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        List<Seller> sellers = sellerRepository.findAllById(ids);
        java.util.Map<Integer, Boolean> result = new java.util.HashMap<>();
        for (Integer id : ids) {
            result.put(id, false);
        }
        for (Seller s : sellers) {
            result.put(s.getId(), s.getVerificationStatus() == Seller.VerificationStatus.APPROVED);
        }
        return result;
    }

    @org.springframework.transaction.annotation.Transactional
    public void updateBuyerStats(Integer buyerId, double spentDelta) {
        Buyer buyer = buyerRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found"));
        
        if (buyer.getTotalOrders() == null) {
            buyer.setTotalOrders(0);
        }
        if (buyer.getTotalSpent() == null) {
            buyer.setTotalSpent(java.math.BigDecimal.ZERO);
        }
        
        buyer.setTotalOrders(buyer.getTotalOrders() + 1);
        buyer.setTotalSpent(buyer.getTotalSpent().add(java.math.BigDecimal.valueOf(spentDelta)));
        
        buyerRepository.save(buyer);
    }

    @org.springframework.transaction.annotation.Transactional
    public void updateSellerStats(Integer sellerId, double revenueDelta, int itemsSoldDelta) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));
        
        if (seller.getItemsSold() == null) {
            seller.setItemsSold(0);
        }
        if (seller.getRevenue() == null) {
            seller.setRevenue(java.math.BigDecimal.ZERO);
        }
        
        seller.setItemsSold(seller.getItemsSold() + itemsSoldDelta);
        seller.setRevenue(seller.getRevenue().add(java.math.BigDecimal.valueOf(revenueDelta)));
        
        sellerRepository.save(seller);
    }

    @org.springframework.transaction.annotation.Transactional
    public void updateSellerRating(Integer sellerId, float rating) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));
        seller.setRating(rating);
        sellerRepository.save(seller);
    }
}

