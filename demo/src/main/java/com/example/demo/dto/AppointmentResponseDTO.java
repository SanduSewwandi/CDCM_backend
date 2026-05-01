package com.example.demo.dto;

import lombok.Data;

@Data
public class AppointmentResponseDTO {
    private String id;
    private String patientId;
    private String patientName;
    private String profileImage;
    private String appointmentNumber;
    private String date;
    private String time;
    private String status;
    private String hospitalName;

    // get patient name
    private String paymentStatus;
    private String doctorId;
    private boolean isPaid;

}