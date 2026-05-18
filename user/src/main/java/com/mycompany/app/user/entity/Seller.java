package com.mycompany.app.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "sellers")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
public class Seller extends User {
    private Integer totalProducts;
    private Integer itemsSold;
    private BigDecimal revenue;
    private Float rating;
    private String businessName;
    private String registrationNumber;
    private String NICNumber;
    private String nicImageUrl;
    private String brCertificateUrl;

    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus = VerificationStatus.NONE;

    private String rejectionReason;

    private java.time.LocalDateTime submittedAt;

    public enum VerificationStatus {
        NONE, PENDING, APPROVED, REJECTED
    }
}
