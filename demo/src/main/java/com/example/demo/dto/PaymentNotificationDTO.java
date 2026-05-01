package com.example.demo.dto;

import lombok.Data;

@Data
public class PaymentNotificationDTO {
    private String merchant_id;
    private String order_id;
    private String payment_id;
    private String payhere_amount;
    private String payhere_currency;
    private String status_code;
    private String md5sig;
    private String method;
    private String status_message;
}