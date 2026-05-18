package com.mycompany.app.user.dto;

import com.mycompany.app.user.entity.Seller.VerificationStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SellerProfileResponse extends UserProfileResponse {
    private VerificationStatus verificationStatus;
    private String businessName;
    private String registrationNumber;
    private String nicImageUrl;
    private String brCertificateUrl;
    private String rejectionReason;
    private LocalDateTime submittedAt;

    public SellerProfileResponse(Integer id, String name, String email, String phone, String address, String role, String profileImageUrl, LocalDateTime createdAt,
                                 VerificationStatus verificationStatus, String businessName, String registrationNumber, String nicImageUrl, String brCertificateUrl, String rejectionReason, LocalDateTime submittedAt) {
        super(id, name, email, phone, address, role, profileImageUrl, createdAt);
        this.verificationStatus = verificationStatus;
        this.businessName = businessName;
        this.registrationNumber = registrationNumber;
        this.nicImageUrl = nicImageUrl;
        this.brCertificateUrl = brCertificateUrl;
        this.rejectionReason = rejectionReason;
        this.submittedAt = submittedAt;
    }
}
