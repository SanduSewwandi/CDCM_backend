package com.example.demo.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PaymentHistoryDTO {
    private String id;
    private String transactionId;
    private String description;
    private String doctorName;
    private String hospitalName;
    private String appointmentNumber;
    private String date;
    private String time;
    private double amount;
    private String currency = "LKR";
    private String paymentStatus;
    private boolean isPaid;
    private LocalDateTime paidAt;
    private String type; // "APPOINTMENT", "LAB_TEST"
}
