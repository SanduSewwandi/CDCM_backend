package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class VerifyEmailRequest {
    @NotBlank @Email
    private String email;

    @NotBlank
    private String code;

    private String role; // "PATIENT" or "DOCTOR"

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}