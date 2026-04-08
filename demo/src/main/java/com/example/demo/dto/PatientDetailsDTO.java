package com.example.demo.dto;

import com.example.demo.model.Patient;
import java.util.List;

public class PatientDetailsDTO extends PatientDTO {
    private String residentialAddress;
    private String nicOrPassport;
    private List<AppointmentHistoryDTO> appointmentHistory;
    private List<MedicalRecordDTO> medicalRecords;

    public PatientDetailsDTO() {}

    public PatientDetailsDTO(Patient patient) {
        super(patient);
        this.residentialAddress = patient.getResidentialAddress();
        this.nicOrPassport = patient.getNicOrPassport();
    }

    // Getters and Setters
    public String getResidentialAddress() { return residentialAddress; }
    public void setResidentialAddress(String residentialAddress) { this.residentialAddress = residentialAddress; }

    public String getNicOrPassport() { return nicOrPassport; }
    public void setNicOrPassport(String nicOrPassport) { this.nicOrPassport = nicOrPassport; }

    public List<AppointmentHistoryDTO> getAppointmentHistory() { return appointmentHistory; }
    public void setAppointmentHistory(List<AppointmentHistoryDTO> appointmentHistory) { this.appointmentHistory = appointmentHistory; }

    public List<MedicalRecordDTO> getMedicalRecords() { return medicalRecords; }
    public void setMedicalRecords(List<MedicalRecordDTO> medicalRecords) { this.medicalRecords = medicalRecords; }
}