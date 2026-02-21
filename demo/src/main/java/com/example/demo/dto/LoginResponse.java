package com.example.demo.dto;

public class LoginResponse {

    private String message;
    private String role;
    private String userId;
    private String name;
    private String token;


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
}
