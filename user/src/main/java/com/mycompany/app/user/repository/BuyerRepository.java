package com.mycompany.app.user.repository;

import com.mycompany.app.user.entity.Buyer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuyerRepository extends JpaRepository<Buyer, Integer> {}
