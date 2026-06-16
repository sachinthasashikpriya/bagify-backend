package com.mycompany.app.order.controller;

import com.mycompany.app.order.service.OrderService;
import com.mycompany.app.order.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.mycompany.app.order.config.SecurityConfig;
import com.mycompany.app.order.config.JwtFilter;
import org.springframework.context.annotation.Import;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, JwtFilter.class})
class OrderWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void testWebhookEndpoint_PublicAccessAndSuccess() throws Exception {
        doNothing().when(orderService).processPaymentNotification(anyMap());

        mockMvc.perform(post("/api/v1/orders/payment/notify")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("merchant_id", "1235988")
                        .param("order_id", "45")
                        .param("payhere_amount", "250.00")
                        .param("payhere_currency", "LKR")
                        .param("status_code", "2")
                        .param("md5sig", "VALIDSIGNATURE"))
                .andExpect(status().isOk())
                .andExpect(content().string("Notification Processed Successfully"));

        verify(orderService, times(1)).processPaymentNotification(anyMap());
    }

    @Test
    void testWebhookEndpoint_MismatchedSignatureBadRequest() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid signature verification failed"))
                .when(orderService).processPaymentNotification(anyMap());

        mockMvc.perform(post("/api/v1/orders/payment/notify")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("merchant_id", "1235988")
                        .param("order_id", "45")
                        .param("payhere_amount", "250.00")
                        .param("payhere_currency", "LKR")
                        .param("status_code", "2")
                        .param("md5sig", "INVALID"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testWebhookEndpoint_MissingParametersBadRequest() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required PayHere parameters"))
                .when(orderService).processPaymentNotification(anyMap());

        mockMvc.perform(post("/api/v1/orders/payment/notify")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("merchant_id", "1235988"))
                .andExpect(status().isBadRequest());
    }
}
