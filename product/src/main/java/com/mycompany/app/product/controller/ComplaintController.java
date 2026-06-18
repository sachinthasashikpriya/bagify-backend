package com.mycompany.app.product.controller;

import com.mycompany.app.product.entity.Complaint;
import com.mycompany.app.product.entity.Product;
import com.mycompany.app.product.repository.ComplaintRepository;
import com.mycompany.app.product.service.ProductService;
import com.mycompany.app.product.dto.ComplaintResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/complaints")
public class ComplaintController {

    private final ComplaintRepository complaintRepository;
    private final ProductService productService;

    public ComplaintController(ComplaintRepository complaintRepository, ProductService productService) {
        this.complaintRepository = complaintRepository;
        this.productService = productService;
    }

    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    @SuppressWarnings("unchecked")
    public ResponseEntity<ComplaintResponse> submitComplaint(
            @RequestBody Map<String, Object> payload,
            Authentication authentication) {

        Integer buyerId = (Integer) authentication.getDetails();
        if (buyerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        if (payload.get("productId") == null || payload.get("description") == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product ID and Description are required");
        }

        Long productId = Long.valueOf(payload.get("productId").toString());
        String description = (String) payload.get("description");
        List<String> images = (List<String>) payload.get("images");
        String buyerEmail = (String) authentication.getPrincipal();

        Optional<Product> optionalProduct = productService.getProductById(productId);
        if (optionalProduct.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
        Product product = optionalProduct.get();

        Complaint complaint = Complaint.builder()
                .product(product)
                .buyerId(String.valueOf(buyerId))
                .buyerName(buyerEmail)
                .sellerId(product.getSellerId())
                .description(description)
                .images(images)
                .status("PENDING")
                .date(LocalDate.now().toString())
                .build();

        Complaint savedComplaint = complaintRepository.save(complaint);
        return ResponseEntity.status(HttpStatus.CREATED).body(ComplaintResponse.fromEntity(savedComplaint));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<List<ComplaintResponse>> getMyComplaints(Authentication authentication) {
        Integer buyerId = (Integer) authentication.getDetails();
        if (buyerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        List<Complaint> complaints = complaintRepository.findByBuyerId(String.valueOf(buyerId));
        List<ComplaintResponse> responseList = complaints.stream()
                .map(ComplaintResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseList);
    }

    @GetMapping("/seller")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<List<ComplaintResponse>> getSellerComplaints(Authentication authentication) {
        Integer sellerId = (Integer) authentication.getDetails();
        if (sellerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        List<Complaint> complaints = complaintRepository.findBySellerId(String.valueOf(sellerId));
        List<ComplaintResponse> responseList = complaints.stream()
                .map(ComplaintResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseList);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ComplaintResponse> updateComplaintStatus(
            @PathVariable Long id,
            @RequestParam String status,
            Authentication authentication) {
        
        Integer sellerId = (Integer) authentication.getDetails();
        if (sellerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        Optional<Complaint> optionalComplaint = complaintRepository.findById(id);
        if (optionalComplaint.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Complaint not found");
        }

        Complaint complaint = optionalComplaint.get();
        if (!complaint.getSellerId().equals(String.valueOf(sellerId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to update this complaint");
        }

        complaint.setStatus(status.toUpperCase());
        Complaint updatedComplaint = complaintRepository.save(complaint);
        return ResponseEntity.ok(ComplaintResponse.fromEntity(updatedComplaint));
    }
}
