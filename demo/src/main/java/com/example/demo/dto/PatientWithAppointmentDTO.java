package com.example.demo.dto;

import java.util.List;

public class PatientWithAppointmentDTO {
    private String id;  // Actual MongoDB ID
    private String patientId;
    private String patientName;
    private String email;
    private String contactNumber;
    private String gender;
    private String dateOfBirth;
    private boolean verified;
    private List<AppointmentHistoryDTO> appointments;

    public PatientWithAppointmentDTO() {}

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public List<AppointmentHistoryDTO> getAppointments() {
        return appointments;
    }

    public void setAppointments(List<AppointmentHistoryDTO> appointments) {
        this.appointments = appointments;
    }
}