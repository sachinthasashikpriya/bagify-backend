package com.mycompany.app.product.repository;

import com.mycompany.app.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findBySellerId(String sellerId);
    List<Product> findByCategoryIgnoreCase(String category);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Product p WHERE " +
           "(:category IS NULL OR LOWER(p.category) = LOWER(:category)) AND " +
           "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Product> findByCategoryAndSearch(@org.springframework.data.repository.query.Param("category") String category, @org.springframework.data.repository.query.Param("search") String search);
}
