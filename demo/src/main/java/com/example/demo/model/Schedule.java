package com.example.demo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "schedules")
public class Schedule {

    @Id
    private String id;

    private String doctorId;
    private String hospitalId;

    private String date;
    private String startTime;
    private String endTime;

    private String status; // PENDING, ACCEPTED, REJECTED

    // ✨ Additional fields for frontend display
    private String doctorName;
    private String specialty;

    // Default Constructor
    public Schedule() {
    }

    // Parameterized Constructor
    public Schedule(String doctorId, String hospitalId, String date, String startTime, String endTime, String status) {
        this.doctorId = doctorId;
        this.hospitalId = hospitalId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    // --- Existing Getters and Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // --- New Getters and Setters for Doctor Info ---

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }
}