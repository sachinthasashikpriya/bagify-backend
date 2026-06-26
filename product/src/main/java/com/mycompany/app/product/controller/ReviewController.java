package com.mycompany.app.product.controller;

import com.mycompany.app.product.entity.Product;
import com.mycompany.app.product.entity.Review;
import com.mycompany.app.product.service.OrderClient;
import com.mycompany.app.product.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.mycompany.app.product.dto.ReviewResponse;
import com.mycompany.app.product.repository.ReviewRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ProductService productService;
    private final OrderClient orderClient;
    private final ReviewRepository reviewRepository;

    public ReviewController(ProductService productService, OrderClient orderClient, ReviewRepository reviewRepository) {
        this.productService = productService;
        this.orderClient = orderClient;
        this.reviewRepository = reviewRepository;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<List<ReviewResponse>> getMyReviews(Authentication authentication) {
        Integer buyerId = (Integer) authentication.getDetails();
        if (buyerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        List<Review> reviews = reviewRepository.findByBuyerId(String.valueOf(buyerId));
        List<ReviewResponse> responses = reviews.stream()
                .map(ReviewResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getReviewsByProductId(@RequestParam Long productId) {
        return productService.getProductById(productId)
                .map(product -> product.getReviews().stream()
                        .map(ReviewResponse::fromEntity)
                        .collect(Collectors.toList()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<Review> submitReview(
            @RequestBody Map<String, Object> payload,
            Authentication authentication,
            HttpServletRequest request) {

        Integer buyerId = (Integer) authentication.getDetails();
        if (buyerId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }

        Long productId = Long.valueOf(payload.get("productId").toString());
        int rating = Integer.parseInt(payload.get("rating").toString());
        String comment = (String) payload.get("comment");
        String buyerName = (String) authentication.getPrincipal(); // Assuming username is principal

        String bearerToken = request.getHeader("Authorization");

        // 1. Verify buyer has a SHIPPED order containing this product
        boolean hasPurchased = orderClient.hasPurchased(productId, bearerToken);
        if (!hasPurchased) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only review products you have purchased and had shipped.");
        }

        // 2. Reject if buyer already reviewed this product
        Optional<Product> optionalProduct = productService.getProductById(productId);
        if (optionalProduct.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
        Product product = optionalProduct.get();

        boolean alreadyReviewed = product.getReviews().stream()
                .anyMatch(r -> String.valueOf(buyerId).equals(r.getBuyerId()));
        
        if (alreadyReviewed) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already reviewed this product.");
        }

        // 3. Save review; update product's averageRating
        Review review = new Review();
        review.setBuyerId(String.valueOf(buyerId));
        review.setBuyerName(buyerName); // In a real app, you might look up the actual name, but username works for now
        review.setRating(rating);
        review.setComment(comment);
        review.setDate(LocalDate.now().toString());

        productService.addReview(productId, review);

        // Retrieve the newly added review (last one added)
        Product updatedProduct = productService.getProductById(productId).orElseThrow();
        Review savedReview = updatedProduct.getReviews().get(updatedProduct.getReviews().size() - 1);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedReview);
    }
}
