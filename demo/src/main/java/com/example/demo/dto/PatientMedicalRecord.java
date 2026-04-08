package com.example.demo.dto;

import lombok.Data;

@Data
public class PatientMedicalRecord {
    private String patientId;
    private String doctorId;
    private String doctorName;
    private String diagnosis;
    private String treatment;
    private String notes;
}