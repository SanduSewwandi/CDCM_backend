package com.example.demo.dto;

public class AppointmentCancelRequest {

    private String reason;

    public AppointmentCancelRequest() {}

    public AppointmentCancelRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
