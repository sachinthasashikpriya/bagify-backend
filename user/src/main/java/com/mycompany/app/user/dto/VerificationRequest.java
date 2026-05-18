package com.mycompany.app.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerificationRequest {
    private String businessName;
    private String registrationNumber;
    private String brCertificateUrl;
    private String nicImageUrl;
}
