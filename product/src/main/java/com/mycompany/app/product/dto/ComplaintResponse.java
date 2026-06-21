package com.mycompany.app.product.dto;

import com.mycompany.app.product.entity.Complaint;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintResponse {
    private String id;
    private String productId;
    private String productName;
    private String productImage;
    private String buyerId;
    private String buyerName;
    private String sellerId;
    private String description;
    private List<String> images;
    private String status;
    private String date;

    public static ComplaintResponse fromEntity(Complaint complaint) {
        if (complaint == null) return null;
        return ComplaintResponse.builder()
                .id(complaint.getId() != null ? complaint.getId().toString() : null)
                .productId(complaint.getProduct() != null && complaint.getProduct().getId() != null ? complaint.getProduct().getId().toString() : null)
                .productName(complaint.getProduct() != null ? complaint.getProduct().getName() : null)
                .productImage(complaint.getProduct() != null ? complaint.getProduct().getImage() : null)
                .buyerId(complaint.getBuyerId())
                .buyerName(complaint.getBuyerName())
                .sellerId(complaint.getSellerId())
                .description(complaint.getDescription())
                .images(complaint.getImages())
                .status(complaint.getStatus())
                .date(complaint.getDate())
                .build();
    }
}
