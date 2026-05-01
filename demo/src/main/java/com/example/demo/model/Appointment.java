package com.example.demo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "appointments")
public class Appointment {

    @Id
    private String id;
    private String appointmentNumber;

    private String patientId;
    private String doctorId;
    private String hospitalId;
    private String scheduleId;

    private String date;
    private String time;
    private String status; // e.g., "CONFIRMED", "CANCELLED", "COMPLETED"
    private String patientName;

    // --- PAYMENT FIELDS ---
    private String paymentStatus = "PENDING"; // Tracks lifecycle (PENDING/PAID)
    private String payhereId;                 // Stores gateway transaction ID
    private double amount;                    // Stores the fee for hash verification[cite: 2]
    private boolean isPaid = false;           // Boolean flag for quick checks[cite: 2]
    private LocalDateTime paidAt;             // Timestamp of successful payment[cite: 2]

    public Appointment() {
        this.status = "PENDING";
        this.paymentStatus = "PENDING";
        this.isPaid = false;
    }

    // --- GETTERS AND SETTERS ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAppointmentNumber() { return appointmentNumber; }
    public void setAppointmentNumber(String appointmentNumber) { this.appointmentNumber = appointmentNumber; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getScheduleId() { return scheduleId; }
    public void setScheduleId(String scheduleId) { this.scheduleId = scheduleId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    // --- PAYMENT GETTERS AND SETTERS ---

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getPayhereId() { return payhereId; }
    public void setPayhereId(String payhereId) { this.payhereId = payhereId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public boolean isPaid() { return isPaid; }
    public void setPaid(boolean paid) { this.isPaid = paid; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
}