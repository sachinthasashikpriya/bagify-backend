package com.mycompany.app.product.controller;

import com.mycompany.app.product.dto.ProductRequest;
import com.mycompany.app.product.dto.ProductResponse;
import com.mycompany.app.product.dto.ReviewRequest;
import com.mycompany.app.product.entity.Product;
import com.mycompany.app.product.entity.Review;
import com.mycompany.app.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // ─── Public endpoints ───────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        List<ProductResponse> responses = productService.getAllProducts(category, search)
                .stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return productService.getProductById(id)
                .map(ProductResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── Seller endpoints ────────────────────────────────────────────────────────

    @GetMapping("/my")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<List<ProductResponse>> getMyProducts(Authentication authentication) {
        Integer userId = (Integer) authentication.getDetails();
        String sellerId = userId != null ? userId.toString() : "unknown";
        List<ProductResponse> responses = productService.getProductsBySellerId(sellerId)
                .stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * Create a product.
     * Uses ProductRequest DTO — prevents mass-assignment (client cannot set sellerId or averageRating).
     * Seller identity is injected from the JWT via Authentication.
     */
    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request,
            Authentication authentication) {

        Integer userId = (Integer) authentication.getDetails();

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(request.getCategory());
        product.setImage(request.getImage());
        // Seller identity from JWT — not from client payload
        product.setSellerId(userId != null ? userId.toString() : "unknown");
        product.setSellerName(authentication.getName());
        product.setSellerRating(0.0); // Computed separately when seller rating feature is built
        product.setStatus(Product.ProductStatus.ACTIVE);

        Product created = productService.createProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.fromEntity(created));
    }

    /**
     * Update a product.
     * Only the owner seller or an admin may update.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request,
            Authentication authentication) {

        // Build a partial-update product from the DTO
        Product updates = new Product();
        updates.setName(request.getName());
        updates.setDescription(request.getDescription());
        updates.setPrice(request.getPrice());
        updates.setStock(request.getStock());
        updates.setCategory(request.getCategory());
        updates.setImage(request.getImage());

        return productService.updateProduct(id, updates, authentication)
                .map(ProductResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id, Authentication authentication) {
        if (productService.deleteProduct(id, authentication)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // ─── Buyer endpoints ─────────────────────────────────────────────────────────

    /**
     * Add a review.
     * Uses ReviewRequest DTO — client only sends rating + comment.
     * buyerId and buyerName are set server-side from the JWT.
     */
    @PostMapping("/{id}/reviews")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<ProductResponse> addReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {

        Integer userId = (Integer) authentication.getDetails();

        Review review = new Review();
        review.setBuyerId(userId != null ? userId.toString() : "unknown");
        review.setBuyerName(authentication.getName());
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setDate(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        return productService.addReview(id, review)
                .map(ProductResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── Internal service-to-service endpoint ────────────────────────────────────

    /**
     * Deduct stock — called internally by the order service.
     * Requires authentication: the order service passes its JWT when calling this.
     * NOT exposed publicly via the API Gateway.
     */
    @PostMapping("/{id}/deduct-stock")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deductStock(@PathVariable Long id, @RequestParam int quantity) {
        boolean success = productService.deductStock(id, quantity);
        if (success) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    /**
     * Restore stock — called internally by the order service.
     * Requires authentication: the order service passes its JWT when calling this.
     * NOT exposed publicly via the API Gateway.
     */
    @PostMapping("/{id}/restore-stock")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> restoreStock(@PathVariable Long id, @RequestParam int quantity) {
        boolean success = productService.restoreStock(id, quantity);
        if (success) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
