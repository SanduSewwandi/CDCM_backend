package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class BankDetailsRequest {
    private String accountHolderName;
    private String bankName;

    @JsonAlias({"bankBranchName", "branch"})
    private String branchName;

    private String accountNumber;

    public void setBankBranchName(String bankBranchName) {
        if (this.branchName == null || this.branchName.trim().isEmpty()) {
            this.branchName = bankBranchName;
        }
    }

    public String getBankBranchName() {
        return this.branchName;
    }
}

