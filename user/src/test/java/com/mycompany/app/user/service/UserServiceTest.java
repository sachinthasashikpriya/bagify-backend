package com.mycompany.app.user.service;

import com.mycompany.app.user.entity.Seller;
import com.mycompany.app.user.exception.ResourceNotFoundException;
import com.mycompany.app.user.repository.SellerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private SellerRepository sellerRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void testUpdateSellerStats_Success() {
        // Arrange
        Integer sellerId = 10;
        double revenueDelta = 250.0;
        int itemsSoldDelta = 10;

        Seller seller = new Seller();
        seller.setId(sellerId);
        seller.setItemsSold(5);
        seller.setRevenue(new BigDecimal("100.0"));

        when(sellerRepository.findById(sellerId)).thenReturn(Optional.of(seller));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userService.updateSellerStats(sellerId, revenueDelta, itemsSoldDelta);

        // Assert
        assertEquals(15, seller.getItemsSold());
        assertEquals(new BigDecimal("350.0"), seller.getRevenue());
        verify(sellerRepository, times(1)).save(seller);
    }

    @Test
    void testUpdateSellerStats_SellerNotFound() {
        // Arrange
        Integer sellerId = 999;
        when(sellerRepository.findById(sellerId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.updateSellerStats(sellerId, 250.0, 10);
        });
        verify(sellerRepository, never()).save(any());
    }

    @Test
    void testUpdateSellerStats_NullInitialValues() {
        // Arrange
        Integer sellerId = 10;
        double revenueDelta = 250.0;
        int itemsSoldDelta = 10;

        Seller seller = new Seller();
        seller.setId(sellerId);
        seller.setItemsSold(null);
        seller.setRevenue(null);

        when(sellerRepository.findById(sellerId)).thenReturn(Optional.of(seller));
        when(sellerRepository.save(any(Seller.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userService.updateSellerStats(sellerId, revenueDelta, itemsSoldDelta);

        // Assert
        assertEquals(10, seller.getItemsSold());
        assertEquals(new BigDecimal("250.0"), seller.getRevenue());
        verify(sellerRepository, times(1)).save(seller);
    }
}
