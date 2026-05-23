package com.mycompany.app.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerificationReviewRequest {
    private String decision; // "APPROVED" or "REJECTED"
    private String rejectionReason;
}
