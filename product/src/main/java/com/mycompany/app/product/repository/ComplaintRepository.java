package com.mycompany.app.product.repository;

import com.mycompany.app.product.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByBuyerId(String buyerId);
    List<Complaint> findBySellerId(String sellerId);
}
