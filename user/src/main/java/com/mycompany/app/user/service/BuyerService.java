package com.mycompany.app.user.service;

import com.mycompany.app.user.dto.UpdateBuyerRequest;
import com.mycompany.app.user.entity.Buyer;
import com.mycompany.app.user.repository.BuyerRepository;
import org.springframework.stereotype.Service;

@Service
public class BuyerService {

    private final BuyerRepository buyerRepository;

    public BuyerService(BuyerRepository buyerRepository) {
        this.buyerRepository = buyerRepository;
    }

    public Buyer updateBuyerProfile(int id, UpdateBuyerRequest request) {

        Buyer buyer = buyerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Buyer not found"));

        if (request.getCartItems() != null) {
            buyer.setCartItems(request.getCartItems());
        }

        return buyerRepository.save(buyer);
    }
}
