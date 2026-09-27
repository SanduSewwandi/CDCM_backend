package com.example.demo.controller;

import com.example.demo.dto.PaymentNotificationDTO;
import com.example.demo.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin("*")
public class PaymentController {

    private static final Logger logger = Logger.getLogger(PaymentController.class.getName());

    @Autowired
    private PaymentService paymentService;

    @Value("${payhere.merchant.id}")
    private String merchantId;

    @Value("${payhere.merchant.secret}")
    private String merchantSecret;

    @GetMapping("/generate-hash/{orderId}/{amount}")
    public Map<String, String> getPaymentHash(@PathVariable String orderId, @PathVariable double amount) {
        String currency = "LKR";

        String amountFormatted = String.format("%.2f", amount);

        try {

            String secretHash = md5(merchantSecret).toUpperCase();
            String mainString = merchantId + orderId + amountFormatted + currency + secretHash;
            String finalHash = md5(mainString).toUpperCase();

            // LOGGING: Check your console to verify these values match your dashboard
            logger.info("--- HASH GENERATION DEBUG ---");
            logger.info("Merchant ID: " + merchantId);
            logger.info("Order ID: " + orderId);
            logger.info("Formatted Amount: " + amountFormatted);
            logger.info("Secret Hash: " + secretHash);
            logger.info("Full String to Hash: " + mainString);
            logger.info("Final Hash: " + finalHash);

            Map<String, String> response = new HashMap<>();
            response.put("merchantId", merchantId);
            response.put("orderId", orderId);
            response.put("amount", amountFormatted);
            response.put("currency", currency);
            response.put("hash", finalHash);

            return response;
        } catch (Exception e) {
            logger.severe("Hash generation failed: " + e.getMessage());
            throw new RuntimeException("Error generating payment hash", e);
        }
    }

    @PostMapping("/notify")
    public void handlePayHereNotify(@ModelAttribute PaymentNotificationDTO dto) {
        paymentService.processNotification(dto);
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<?> getPatientPaymentHistory(@PathVariable String patientId) {
        try {
            return ResponseEntity.ok(paymentService.getPatientPaymentHistory(patientId));
        } catch (Exception e) {
            logger.severe("Error fetching patient payment history: " + e.getMessage());
            return ResponseEntity.status(500).body("Error fetching payment history: " + e.getMessage());
        }
    }

    @PostMapping("/payment-success/{orderId}")
    public ResponseEntity<?> paymentSuccess(
            @PathVariable String orderId,
            @RequestBody(required = false) Map<String, Object> req) {
        try {
            String payhereId = null;
            Double amount = null;
            if (req != null) {
                if (req.containsKey("payhereId") && req.get("payhereId") != null) {
                    payhereId = String.valueOf(req.get("payhereId"));
                }
                if (req.containsKey("amount") && req.get("amount") != null) {
                    try {
                        amount = Double.parseDouble(String.valueOf(req.get("amount")));
                    } catch (Exception ignored) {}
                }
            }

            com.example.demo.model.Appointment updated = paymentService.confirmPaymentSuccess(orderId, payhereId, amount);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Payment confirmed successfully");
            response.put("appointment", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.severe("Error confirming payment success: " + e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error confirming payment: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    private String md5(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] messageDigest = md.digest(input.getBytes());
        BigInteger no = new BigInteger(1, messageDigest);
        StringBuilder hashtext = new StringBuilder(no.toString(16));
        while (hashtext.length() < 32) {
            hashtext.insert(0, "0");
        }
        return hashtext.toString();
    }
}