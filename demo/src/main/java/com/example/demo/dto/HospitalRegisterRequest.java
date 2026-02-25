package com.example.demo.dto;

import jakarta.validation.constraints.*;

public class HospitalRegisterRequest {

    @NotBlank(message = "Hospital name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$",
            message = "Password must contain uppercase, lowercase, number, and special character"
    )
    private String password;

    @NotBlank(message = "Contact number is required")
    @Pattern(regexp = "07\\d{8}", message = "Contact number must start with 07 and be 10 digits")
    private String contactNumber;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "License number is required")
    private String licenseNumber;

    @NotBlank(message = "Manager name is required")
    private String managerName;

    public HospitalRegisterRequest() {}

    // ========================
    // Getters and Setters
    // ========================

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }
}