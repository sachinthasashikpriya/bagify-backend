package com.mycompany.app.order.controller;

import com.mycompany.app.order.config.JwtFilter;
import com.mycompany.app.order.config.SecurityConfig;
import com.mycompany.app.order.dto.OrderResponse;
import com.mycompany.app.order.entity.Order;
import com.mycompany.app.order.service.OrderService;
import com.mycompany.app.order.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, JwtFilter.class})
class OrderControllerAccessControlTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void testGetOrder_Success_ForOwner() throws Exception {
        // Setup authentication mock
        when(jwtUtil.extractEmail(anyString())).thenReturn("buyer@example.com");
        when(jwtUtil.extractUserId(anyString())).thenReturn(100);
        when(jwtUtil.extractRole(anyString())).thenReturn("BUYER");

        // Mock OrderResponse
        OrderResponse response = OrderResponse.builder()
                .id(42L)
                .buyerId(100)
                .status("PENDING")
                .totalAmount(150.0)
                .items(new ArrayList<>())
                .build();

        when(orderService.getOrderById(eq(42L), eq(100), eq(false))).thenReturn(response);

        mockMvc.perform(get("/api/v1/orders/42")
                        .header("Authorization", "Bearer mock-jwt-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.buyerId").value(100))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(orderService, times(1)).getOrderById(42L, 100, false);
    }

    @Test
    void testGetOrder_Forbidden_ForDifferentBuyer() throws Exception {
        // Setup authentication mock (Buyer ID is 101, but trying to access order belonging to someone else)
        when(jwtUtil.extractEmail(anyString())).thenReturn("anotherbuyer@example.com");
        when(jwtUtil.extractUserId(anyString())).thenReturn(101);
        when(jwtUtil.extractRole(anyString())).thenReturn("BUYER");

        // Service throws 403 Forbidden
        when(orderService.getOrderById(eq(42L), eq(101), eq(false)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));

        mockMvc.perform(get("/api/v1/orders/42")
                        .header("Authorization", "Bearer mock-jwt-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(orderService, times(1)).getOrderById(42L, 101, false);
    }

    @Test
    void testGetOrder_Success_ForAdmin() throws Exception {
        // Setup authentication mock (Role is ADMIN)
        when(jwtUtil.extractEmail(anyString())).thenReturn("admin@example.com");
        when(jwtUtil.extractUserId(anyString())).thenReturn(1);
        when(jwtUtil.extractRole(anyString())).thenReturn("ADMIN");

        OrderResponse response = OrderResponse.builder()
                .id(42L)
                .buyerId(100)
                .status("PENDING")
                .totalAmount(150.0)
                .items(new ArrayList<>())
                .build();

        when(orderService.getOrderById(eq(42L), eq(1), eq(true))).thenReturn(response);

        mockMvc.perform(get("/api/v1/orders/42")
                        .header("Authorization", "Bearer mock-jwt-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.buyerId").value(100));

        verify(orderService, times(1)).getOrderById(42L, 1, true);
    }

    @Test
    void testGetOrder_Unauthorized_WithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/orders/42")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(orderService);
    }
}
