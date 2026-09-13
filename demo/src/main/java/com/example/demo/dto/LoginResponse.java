package com.example.demo.dto;

public class LoginResponse {

    private String message;
    private String role;
    private String userId;
    private String name;
    private String token;

    private String email;
    private boolean verified;
    private boolean mustChangePassword;
    private String profileImage;


    public LoginResponse() {
    }

    public LoginResponse(String message, String role, String userId, String name, String token) {
        this.message = message;
        this.role = role;
        this.userId = userId;
        this.name = name;
        this.token = token;
    }

    // ====================
    // Getters & Setters
    // ====================

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getToken() {
        return token;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public void setToken(String token) {
        this.token = token;
    }
}


