package com.mycompany.app.user.repository;

import com.mycompany.app.user.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerRepository extends JpaRepository<Seller, Integer> {}