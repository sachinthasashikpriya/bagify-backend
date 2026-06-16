package com.mycompany.app.user.controller;

import com.mycompany.app.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Test
    void testUpdateSellerStats() {
        // Arrange
        Integer sellerId = 10;
        double revenueDelta = 250.0;
        int itemsSoldDelta = 10;

        doNothing().when(userService).updateSellerStats(sellerId, revenueDelta, itemsSoldDelta);

        // Act
        ResponseEntity<Void> response = userController.updateSellerStats(sellerId, revenueDelta, itemsSoldDelta);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userService, times(1)).updateSellerStats(sellerId, revenueDelta, itemsSoldDelta);
    }
}
