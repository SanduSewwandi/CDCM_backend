
package com.example.demo.integration;

import com.example.demo.config.SecurityConfig;
import com.example.demo.controller.PaymentController;
import com.example.demo.security.JwtAuthenticationFilter;
import com.example.demo.security.JwtService;
import com.example.demo.service.PaymentService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@TestPropertySource(properties = {
        "payhere.merchant.id=testMerchant",
        "payhere.merchant.secret=testSecret"
})
class PaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void getPatientPaymentHistoryShouldReturn200() throws Exception {

        String patientId = "patient123";

        when(paymentService.getPatientPaymentHistory(patientId))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(
                        get("/api/payments/patient/" + patientId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(paymentService)
                .getPatientPaymentHistory(patientId);
    }


    @Test
    void getPatientPaymentHistoryServiceErrorShouldReturn500() throws Exception {

        String patientId = "patient123";

        when(paymentService.getPatientPaymentHistory(patientId))
                .thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(
                        get("/api/payments/patient/" + patientId)
                )
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(
                        "Error fetching payment history: Database connection failed"
                ));

        verify(paymentService)
                .getPatientPaymentHistory(patientId);
    }

    @Test
    void paymentSuccessConfirmationShouldReturn200() throws Exception {

        String orderId = "order123";
        String payhereId = "PAY123456";
        Double amount = 2500.00;

        com.example.demo.model.Appointment updatedAppointment =
                new com.example.demo.model.Appointment();

        when(paymentService.confirmPaymentSuccess(
                orderId,
                payhereId,
                amount
        )).thenReturn(updatedAppointment);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .post("/api/payments/payment-success/" + orderId)
                                .contentType(
                                        org.springframework.http.MediaType.APPLICATION_JSON
                                )
                                .content("""
                                    {
                                        "payhereId": "PAY123456",
                                        "amount": 2500.00
                                    }
                                    """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Payment confirmed successfully"))
                .andExpect(jsonPath("$.appointment").exists());

        verify(paymentService)
                .confirmPaymentSuccess(
                        orderId,
                        payhereId,
                        amount
                );
    }

    @Test
    void paymentConfirmationRejectedShouldReturn400() throws Exception {

        String orderId = "order123";
        String payhereId = "PAY123456";
        Double amount = 2500.00;

        when(paymentService.confirmPaymentSuccess(
                orderId,
                payhereId,
                amount
        )).thenThrow(
                new IllegalStateException("Payment already confirmed")
        );

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .post("/api/payments/payment-success/" + orderId)
                                .contentType(
                                        org.springframework.http.MediaType.APPLICATION_JSON
                                )
                                .content("""
                                    {
                                        "payhereId": "PAY123456",
                                        "amount": 2500.00
                                    }
                                    """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Payment already confirmed"));

        verify(paymentService)
                .confirmPaymentSuccess(
                        orderId,
                        payhereId,
                        amount
                );
    }

    @Test
    void paymentConfirmationServerErrorShouldReturn500() throws Exception {

        String orderId = "order123";
        String payhereId = "PAY123456";
        Double amount = 2500.00;

        when(paymentService.confirmPaymentSuccess(
                orderId,
                payhereId,
                amount
        )).thenThrow(
                new RuntimeException("Database connection failed")
        );

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .post("/api/payments/payment-success/" + orderId)
                                .contentType(
                                        org.springframework.http.MediaType.APPLICATION_JSON
                                )
                                .content("""
                                    {
                                        "payhereId": "PAY123456",
                                        "amount": 2500.00
                                    }
                                    """)
                )
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Error confirming payment: Database connection failed"));

        verify(paymentService)
                .confirmPaymentSuccess(
                        orderId,
                        payhereId,
                        amount
                );
    }

    @Test
    void payHereNotificationShouldReturn200() throws Exception {

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .post("/api/payments/notify")
                                .contentType(
                                        org.springframework.http.MediaType
                                                .APPLICATION_FORM_URLENCODED
                                )
                                .param("merchant_id", "123456")
                                .param("order_id", "order123")
                                .param("payment_id", "PAY123456")
                                .param("payhere_amount", "2500.00")
                                .param("payhere_currency", "LKR")
                                .param("status_code", "2")
                                .param("md5sig", "testSignature")
                )
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(paymentService)
                .processNotification(
                        org.mockito.ArgumentMatchers.any(
                                com.example.demo.dto.PaymentNotificationDTO.class
                        )
                );
    }

    @Test
    void generatePaymentHashShouldReturn200() throws Exception {

        String orderId = "order123";
        double amount = 2500.00;

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .get("/api/payments/generate-hash/"
                                        + orderId + "/" + amount)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantId").value("testMerchant"))
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.amount").value("2500.00"))
                .andExpect(jsonPath("$.currency").value("LKR"))
                .andExpect(jsonPath("$.hash").isString())
                .andExpect(jsonPath("$.hash")
                        .value(org.hamcrest.Matchers.matchesPattern(
                                "[A-F0-9]{32}"
                        )));
    }

    @Test
    void generatePaymentHashShouldMatchExpectedMd5() throws Exception {

        String orderId = "order123";
        String amount = "2500.00";
        String currency = "LKR";

        String merchantId = "testMerchant";
        String merchantSecret = "testSecret";

        java.security.MessageDigest md =
                java.security.MessageDigest.getInstance("MD5");

        byte[] secretDigest = md.digest(
                merchantSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        String secretHash = java.util.HexFormat.of()
                .withUpperCase()
                .formatHex(secretDigest);

        String hashInput =
                merchantId + orderId + amount + currency + secretHash;

        byte[] finalDigest = md.digest(
                hashInput.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        String expectedHash = java.util.HexFormat.of()
                .withUpperCase()
                .formatHex(finalDigest);

        mockMvc.perform(
                        get("/api/payments/generate-hash/"
                                + orderId + "/2500.0")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantId").value(merchantId))
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.amount").value(amount))
                .andExpect(jsonPath("$.currency").value(currency))
                .andExpect(jsonPath("$.hash").value(expectedHash));
    }

    @Test
    void paymentSuccessWithoutRequestBodyShouldReturn200() throws Exception {

        String orderId = "order123";

        com.example.demo.model.Appointment updatedAppointment =
                new com.example.demo.model.Appointment();

        when(paymentService.confirmPaymentSuccess(
                org.mockito.ArgumentMatchers.eq(orderId),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull()
        )).thenReturn(updatedAppointment);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .post("/api/payments/payment-success/" + orderId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Payment confirmed successfully"))
                .andExpect(jsonPath("$.appointment").exists());

        verify(paymentService)
                .confirmPaymentSuccess(
                        org.mockito.ArgumentMatchers.eq(orderId),
                        org.mockito.ArgumentMatchers.isNull(),
                        org.mockito.ArgumentMatchers.isNull()
                );
    }

    @Test
    void paymentSuccessWithInvalidAmountShouldHandleGracefully() throws Exception {

        String orderId = "order123";
        String payhereId = "PAY123456";

        com.example.demo.model.Appointment updatedAppointment =
                new com.example.demo.model.Appointment();

        when(paymentService.confirmPaymentSuccess(
                org.mockito.ArgumentMatchers.eq(orderId),
                org.mockito.ArgumentMatchers.eq(payhereId),
                org.mockito.ArgumentMatchers.isNull()
        )).thenReturn(updatedAppointment);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .post("/api/payments/payment-success/" + orderId)
                                .contentType(
                                        org.springframework.http.MediaType.APPLICATION_JSON
                                )
                                .content("""
                                    {
                                        "payhereId": "PAY123456",
                                        "amount": "invalid-amount"
                                    }
                                    """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Payment confirmed successfully"));

        verify(paymentService)
                .confirmPaymentSuccess(
                        org.mockito.ArgumentMatchers.eq(orderId),
                        org.mockito.ArgumentMatchers.eq(payhereId),
                        org.mockito.ArgumentMatchers.isNull()
                );
    }


}
