package com.mycompany.app.user.repository;

import com.mycompany.app.user.entity.Seller;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SellerRepository extends JpaRepository<Seller, Integer> {
    List<Seller> findAllByVerificationStatus(Seller.VerificationStatus verificationStatus);
}