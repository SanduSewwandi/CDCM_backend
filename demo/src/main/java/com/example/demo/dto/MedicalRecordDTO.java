package com.example.demo.dto;

public class MedicalRecordDTO {
    private String id;
    private String recordType; // "PRESCRIPTION", "REPORT", "LAB_RESULT"
    private String title;
    private String description;
    private String doctorName;
    private String hospitalName;
    private String date;
    private String fileUrl;

    public MedicalRecordDTO() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getHospitalName() { return hospitalName; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
}