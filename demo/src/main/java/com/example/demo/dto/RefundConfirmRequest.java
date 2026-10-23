package com.example.demo.dto;

import lombok.Data;

@Data
public class RefundConfirmRequest {
    private String transactionRef;
    private String notes;
}
