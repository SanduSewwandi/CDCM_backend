package com.example.demo.dto;

public class LoginResponse {
    private String message;
    private String doctorId;

    public LoginResponse(String message, String doctorId) {
        this.message = message;
        this.doctorId = doctorId;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
}
