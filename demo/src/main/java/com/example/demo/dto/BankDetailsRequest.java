package com.example.demo.dto;

import lombok.Data;

@Data
public class BankDetailsRequest {
    private String accountHolderName;
    private String bankName;
    private String branchName;
    private String accountNumber;
}
