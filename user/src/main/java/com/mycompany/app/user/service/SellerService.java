package com.mycompany.app.user.service;

import com.mycompany.app.user.dto.UpdateSellerRequest;
import com.mycompany.app.user.entity.Seller;
import com.mycompany.app.user.repository.SellerRepository;
import org.springframework.stereotype.Service;

@Service
public class SellerService {

    private final SellerRepository sellerRepository;

    public SellerService(SellerRepository sellerRepository) {
        this.sellerRepository = sellerRepository;
    }

    public Seller updateSellerProfile(int id, UpdateSellerRequest request) {

        Seller seller = sellerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        if (request.getBusinessName() != null) {
            seller.setBusinessName(request.getBusinessName());
        }

        if (request.getRegistrationNumber() != null) {
            seller.setRegistrationNumber(request.getRegistrationNumber());
        }

        if (request.getNICNumber() != null) {
            seller.setNICNumber(request.getNICNumber());
        }

        return sellerRepository.save(seller);
    }
}
