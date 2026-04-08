package com.example.demo.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "patient_medical_records")
public class PatientMedicalRecord {
    @Id
    private String id;
    private String patientId;
    private String doctorId;
    private String doctorName;      // Auto-filled
    private String hospitalName;    // Auto-filled
    private String dateOfVisit;     // Auto-filled (Today)
    private String conditions;      // Diagnosis
    private String treatment;       // Procedure
    private String medications;     // Prescribed medicine
    private String allergies;
    private String followUp;        // Required or not
    private String requiredLabTests;
    private String notes;
}