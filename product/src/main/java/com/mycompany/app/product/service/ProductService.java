package com.mycompany.app.product.service;

import com.mycompany.app.product.entity.Product;
import com.mycompany.app.product.entity.Review;
import com.mycompany.app.product.repository.ProductRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @PostConstruct
    @Transactional
    public void seedInitialData() {
        if (productRepository.count() == 0) {
            System.out.println("🌱 Database is empty. Seeding initial products...");

            // Product 1
            Product p1 = Product.builder()
                    .name("Classic Leather Handbag")
                    .description("Elegant handcrafted leather handbag with premium finish. Features multiple compartments and adjustable strap.")
                    .price(129.99)
                    .category("Handbag")
                    .image("https://images.unsplash.com/photo-1548036328-c9fa89d128fa?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxsZWF0aGVyJTIwaGFuZGJhZ3xlbnwxfHx8fDE3NjUwOTg1Mjl8MA&ixlib=rb-4.1.0&q=80&w=1080")
                    .sellerId("s1")
                    .sellerName("Luxe Bags Co")
                    .sellerRating(4.8)
                    .stock(15)
                    .build();

            // Reviews for Product 1
            Review r1 = Review.builder()
                    .buyerId("b1")
                    .buyerName("John Smith")
                    .rating(5)
                    .comment("Excellent quality! The leather is soft and the stitching is perfect.")
                    .date("2024-11-15")
                    .build();

            Review r2 = Review.builder()
                    .buyerId("b2")
                    .buyerName("Lisa Anderson")
                    .rating(4)
                    .comment("Beautiful bag, though slightly smaller than I expected.")
                    .date("2024-11-20")
                    .build();

            p1.addReview(r1);
            p1.addReview(r2);

            // Product 2
            Product p2 = Product.builder()
                    .name("Urban Backpack")
                    .description("Modern backpack designed for urban professionals. Water-resistant fabric with laptop compartment.")
                    .price(89.99)
                    .category("Backpack")
                    .image("https://images.unsplash.com/photo-1574271143443-3a7b2e7a36bd?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxiYWNrcGFjayUyMGZhc2hpb258ZW58MXx8fHwxNzY1MjA5NjI3fDA&ixlib=rb-4.1.0&q=80&w=1080")
                    .sellerId("s2")
                    .sellerName("Urban Carry")
                    .sellerRating(4.6)
                    .stock(22)
                    .build();

            Review r3 = Review.builder()
                    .buyerId("b1")
                    .buyerName("John Smith")
                    .rating(5)
                    .comment("Perfect for daily commute. Very comfortable and spacious.")
                    .date("2024-11-18")
                    .build();

            p2.addReview(r3);

            // Product 3
            Product p3 = Product.builder()
                    .name("Travel Duffel Bag")
                    .description("Spacious travel bag perfect for weekend getaways. Durable construction with multiple pockets.")
                    .price(149.99)
                    .category("Travel Bag")
                    .image("https://images.unsplash.com/photo-1448582649076-3981753123b5?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHx0cmF2ZWwlMjBiYWd8ZW58MXx8fHwxNzY1MjEwODM3fDA&ixlib=rb-4.1.0&q=80&w=1080")
                    .sellerId("s3")
                    .sellerName("Craft Leather Studio")
                    .sellerRating(4.9)
                    .stock(8)
                    .build();

            // Product 4
            Product p4 = Product.builder()
                    .name("Canvas Tote Bag")
                    .description("Eco-friendly canvas tote bag. Perfect for shopping or daily use. Strong handles and large capacity.")
                    .price(39.99)
                    .category("Tote Bag")
                    .image("https://images.unsplash.com/photo-1574365569389-a10d488ca3fb?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHx0b3RlJTIwYmFnfGVufDF8fHx8MTc2NTE4NDg3NXww&ixlib=rb-4.1.0&q=80&w=1080")
                    .sellerId("s1")
                    .sellerName("Luxe Bags Co")
                    .sellerRating(4.8)
                    .stock(30)
                    .build();

            // Product 5
            Product p5 = Product.builder()
                    .name("Messenger Bag")
                    .description("Professional messenger bag with padded shoulder strap. Ideal for carrying documents and laptop.")
                    .price(99.99)
                    .category("Messenger Bag")
                    .image("https://images.unsplash.com/photo-1528976915572-6a0cf746802e?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxtZXNzZW5nZXIlMjBiYWd8ZW58MXx8fHwxNzY1MjEwODM5fDA&ixlib=rb-4.1.0&q=80&w=1080")
                    .sellerId("s2")
                    .sellerName("Urban Carry")
                    .sellerRating(4.6)
                    .stock(18)
                    .build();

            // Product 6
            Product p6 = Product.builder()
                    .name("Designer Crossbody Purse")
                    .description("Stylish crossbody purse with gold chain strap. Premium materials and elegant design.")
                    .price(179.99)
                    .category("Purse")
                    .image("https://images.unsplash.com/photo-1601924928357-22d3b3abfcfb?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxkZXNpZ25lciUyMHB1cnNlfGVufDF8fHx8MTc2NTE4NTYxNHww&ixlib=rb-4.1.0&q=80&w=1080")
                    .sellerId("s3")
                    .sellerName("Craft Leather Studio")
                    .sellerRating(4.9)
                    .stock(12)
                    .build();

            productRepository.saveAll(List.of(p1, p2, p3, p4, p5, p6));
            System.out.println("✅ Seeding completed. 6 products created!");
        }
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts(String category, String search) {
        if ((category == null || category.trim().isEmpty()) && (search == null || search.trim().isEmpty())) {
            return productRepository.findAll();
        }
        return productRepository.findByCategoryAndSearch(
            category != null && !category.trim().isEmpty() ? category : null,
            search != null && !search.trim().isEmpty() ? search : null
        );
    }

    @Transactional(readOnly = true)
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    @Transactional
    public Product createProduct(Product product) {
        product.calculateAverageRating();
        return productRepository.save(product);
    }

    @Transactional
    public Optional<Product> updateProduct(Long id, Product updates) {
        return productRepository.findById(id).map(existing -> {
            if (updates.getName() != null) existing.setName(updates.getName());
            if (updates.getDescription() != null) existing.setDescription(updates.getDescription());
            if (updates.getPrice() > 0) existing.setPrice(updates.getPrice());
            if (updates.getStock() >= 0) existing.setStock(updates.getStock());
            if (updates.getCategory() != null) existing.setCategory(updates.getCategory());
            if (updates.getImage() != null) existing.setImage(updates.getImage());
            if (updates.getSellerName() != null) existing.setSellerName(updates.getSellerName());
            if (updates.getSellerRating() > 0) existing.setSellerRating(updates.getSellerRating());
            existing.calculateAverageRating();
            return productRepository.save(existing);
        });
    }

    @Transactional
    public boolean deleteProduct(Long id) {
        return productRepository.findById(id).map(product -> {
            productRepository.delete(product);
            return true;
        }).orElse(false);
    }

    @Transactional
    public Optional<Product> addReview(Long productId, Review review) {
        return productRepository.findById(productId).map(product -> {
            product.addReview(review);
            return productRepository.save(product);
        });
    }
}
