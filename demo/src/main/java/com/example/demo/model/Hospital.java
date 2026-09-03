package com.example.demo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Date;

@Document(collection = "hospitals")
public class Hospital {

    @Id
    private String id;

    private String name;
    private String email;
    private String password;
    private String contactNumber;
    private String address;
    private String licenseNumber;
    private String managerName;
    private String location;
    private String profileImage;

    // Email verification
    private boolean verified = false;
    private String verificationCode;
    private Date verificationExpiry;
    private Date verificationLastSentAt;

    // First login
    private boolean mustChangePassword = true;

    // Password reset
    private String resetToken;
    private LocalDateTime resetTokenExpiry;

    public Hospital() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(
            String password) {
        this.password = password;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(
            String contactNumber) {
        this.contactNumber =
                contactNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(
            String address) {
        this.address = address;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(
            String licenseNumber) {
        this.licenseNumber =
                licenseNumber;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(
            String managerName) {
        this.managerName =
                managerName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(
            String location) {
        this.location = location;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(
            String profileImage) {
        this.profileImage =
                profileImage;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(
            boolean verified) {
        this.verified = verified;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(
            String verificationCode) {
        this.verificationCode =
                verificationCode;
    }

    public Date getVerificationExpiry() {
        return verificationExpiry;
    }

    public void setVerificationExpiry(
            Date verificationExpiry) {
        this.verificationExpiry =
                verificationExpiry;
    }

    public Date getVerificationLastSentAt() {
        return verificationLastSentAt;
    }

    public void setVerificationLastSentAt(
            Date verificationLastSentAt) {
        this.verificationLastSentAt =
                verificationLastSentAt;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(
            boolean mustChangePassword) {
        this.mustChangePassword =
                mustChangePassword;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(
            String resetToken) {
        this.resetToken = resetToken;
    }

    public LocalDateTime getResetTokenExpiry() {
        return resetTokenExpiry;
    }

    public void setResetTokenExpiry(
            LocalDateTime resetTokenExpiry) {
        this.resetTokenExpiry =
                resetTokenExpiry;
    }
}