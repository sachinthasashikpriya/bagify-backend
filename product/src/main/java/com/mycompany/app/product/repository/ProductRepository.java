package com.mycompany.app.product.repository;

import com.mycompany.app.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findBySellerId(String sellerId);
    List<Product> findByCategory(String category);
}
