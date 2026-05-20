package com.mycompany.app.product.controller;

import com.mycompany.app.product.dto.ProductResponse;
import com.mycompany.app.product.entity.Product;
import com.mycompany.app.product.entity.Review;
import com.mycompany.app.product.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts(@RequestParam(required = false) String category) {
        List<Product> products = productService.getAllProducts(category);
        List<ProductResponse> responses = products.stream()
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

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ProductResponse> createProduct(@RequestBody Product product, Authentication authentication) {
        // Retrieve authenticated seller's details
        Integer userId = (Integer) authentication.getDetails();
        product.setSellerId(userId != null ? userId.toString() : "unknown");
        product.setSellerName(authentication.getName());
        product.setSellerRating(4.5); // Default seller rating

        Product created = productService.createProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.fromEntity(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long id, @RequestBody Product updates) {
        return productService.updateProduct(id, updates)
                .map(ProductResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        if (productService.deleteProduct(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/reviews")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<ProductResponse> addReview(@PathVariable Long id, @RequestBody Review review, Authentication authentication) {
        Integer userId = (Integer) authentication.getDetails();
        review.setBuyerId(userId != null ? userId.toString() : "unknown");
        review.setBuyerName(authentication.getName());
        review.setDate(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

        return productService.addReview(id, review)
                .map(ProductResponse::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
