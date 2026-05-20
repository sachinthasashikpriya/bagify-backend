package com.mycompany.app.user.Controller;

import com.mycompany.app.user.dto.VerificationReviewRequest;
import com.mycompany.app.user.dto.UserProfileResponse;
import com.mycompany.app.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/verifications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // ✅ Protect all endpoints with ADMIN role check
public class AdminVerificationController {

    private final UserService userService;

    /**
     * GET /api/v1/admin/verifications
     * Returns all sellers with verificationStatus = PENDING
     */
    @GetMapping
    public ResponseEntity<List<UserProfileResponse>> getPendingVerifications() {
        return ResponseEntity.ok(userService.getPendingVerifications());
    }

    /**
     * PUT /api/v1/admin/verifications/{sellerId}
     * Review a seller's business verification request (APPROVED / REJECTED)
     */
    @PutMapping("/{sellerId}")
    public ResponseEntity<UserProfileResponse> reviewVerification(
            @PathVariable int sellerId,
            @RequestBody VerificationReviewRequest request
    ) {
        return ResponseEntity.ok(userService.reviewVerification(sellerId, request));
    }
}
