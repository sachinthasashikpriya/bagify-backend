package com.mycompany.app.user.service;

import com.mycompany.app.user.entity.Buyer;
import com.mycompany.app.user.entity.Wishlist;
import com.mycompany.app.user.exception.ResourceNotFoundException;
import com.mycompany.app.user.repository.BuyerRepository;
import com.mycompany.app.user.repository.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final BuyerRepository buyerRepository;

    public WishlistService(WishlistRepository wishlistRepository, BuyerRepository buyerRepository) {
        this.wishlistRepository = wishlistRepository;
        this.buyerRepository = buyerRepository;
    }

    @Transactional
    public void addProductToWishlist(Integer buyerId, Long productId) {
        Buyer buyer = buyerRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found"));

        Wishlist wishlist = wishlistRepository.findByBuyerId(buyerId)
                .orElseGet(() -> {
                    Wishlist newWishlist = new Wishlist();
                    newWishlist.setBuyer(buyer);
                    return newWishlist;
                });

        if (wishlist.getProductIds().add(productId)) {
            wishlistRepository.save(wishlist);
            
            // Update the wishlist count in Buyer
            if (buyer.getWishList() == null) {
                buyer.setWishList(1);
            } else {
                buyer.setWishList(buyer.getWishList() + 1);
            }
            buyerRepository.save(buyer);
        }
    }

    public Set<Long> getWishlist(Integer buyerId) {
        return wishlistRepository.findByBuyerId(buyerId)
                .map(Wishlist::getProductIds)
                .orElse(Set.of());
    }
    
    @Transactional
    public void removeProductFromWishlist(Integer buyerId, Long productId) {
        Buyer buyer = buyerRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found"));
        
        Wishlist wishlist = wishlistRepository.findByBuyerId(buyerId).orElse(null);
        if (wishlist != null && wishlist.getProductIds().remove(productId)) {
            wishlistRepository.save(wishlist);
            
            if (buyer.getWishList() != null && buyer.getWishList() > 0) {
                buyer.setWishList(buyer.getWishList() - 1);
                buyerRepository.save(buyer);
            }
        }
    }
}
