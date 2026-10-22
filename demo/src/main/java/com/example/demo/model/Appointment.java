package com.example.demo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "appointments")
public class Appointment {

    @Id
    private String id;
    private String appointmentNumber;
    private String consultationType;
    private String meetingLink;

    private String patientId;
    private String doctorId;
    private String hospitalId;
    private String scheduleId;

    private String date;
    private String time;
    private String status; // e.g., "CONFIRMED", "CANCELLED", "COMPLETED"
    private String patientName;

    // --- PAYMENT FIELDS ---
    private String paymentStatus = "PENDING";
    private String payhereId;
    private double amount;
    private boolean isPaid = false;
    private LocalDateTime paidAt;

    // --- REFUND AUDIT FIELDS ---
    private Double refundPercentage;
    private Double refundAmount;
    private String refundStatus; // e.g. "REFUNDED"
    private String cancelledBy;  // "PATIENT", "DOCTOR", "HOSPITAL", "SYSTEM"
    private String refundReason;
    private LocalDateTime refundedAt;

    // --- MEDICAL HISTORY OTP ACCESS FIELDS ---
    private String medicalHistoryOtpHash;
    private LocalDateTime medicalHistoryOtpExpiresAt;
    private LocalDateTime medicalHistoryAccessGrantedAt;
    private LocalDateTime medicalHistoryAccessExpiresAt;
    private Integer medicalHistoryOtpFailedAttempts = 0;

    // --- APPOINTMENT CREATION / HOLD TIMESTAMP ---
    private LocalDateTime createdAt = LocalDateTime.now();

    public Appointment() {
        this.status = "PENDING";
        this.paymentStatus = "PENDING";
        this.isPaid = false;
        this.createdAt = LocalDateTime.now();
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

    public String getConsultationType() {
        return consultationType;
    }

    public void setConsultationType(String consultationType) {
        this.consultationType = consultationType;
    }

    public String getMeetingLink() {
        return meetingLink;
    }

    public void setMeetingLink(String meetingLink) {
        this.meetingLink = meetingLink;
    }

    // --- MEDICAL HISTORY OTP ACCESS GETTERS AND SETTERS ---

    public String getMedicalHistoryOtpHash() {
        return medicalHistoryOtpHash;
    }

    public void setMedicalHistoryOtpHash(String medicalHistoryOtpHash) {
        this.medicalHistoryOtpHash = medicalHistoryOtpHash;
    }

    public LocalDateTime getMedicalHistoryOtpExpiresAt() {
        return medicalHistoryOtpExpiresAt;
    }

    public void setMedicalHistoryOtpExpiresAt(LocalDateTime medicalHistoryOtpExpiresAt) {
        this.medicalHistoryOtpExpiresAt = medicalHistoryOtpExpiresAt;
    }

    public LocalDateTime getMedicalHistoryAccessGrantedAt() {
        return medicalHistoryAccessGrantedAt;
    }

    public void setMedicalHistoryAccessGrantedAt(LocalDateTime medicalHistoryAccessGrantedAt) {
        this.medicalHistoryAccessGrantedAt = medicalHistoryAccessGrantedAt;
    }

    public LocalDateTime getMedicalHistoryAccessExpiresAt() {
        return medicalHistoryAccessExpiresAt;
    }

    public void setMedicalHistoryAccessExpiresAt(LocalDateTime medicalHistoryAccessExpiresAt) {
        this.medicalHistoryAccessExpiresAt = medicalHistoryAccessExpiresAt;
    }

    public Integer getMedicalHistoryOtpFailedAttempts() {
        return medicalHistoryOtpFailedAttempts != null ? medicalHistoryOtpFailedAttempts : 0;
    }

    public void setMedicalHistoryOtpFailedAttempts(Integer medicalHistoryOtpFailedAttempts) {
        this.medicalHistoryOtpFailedAttempts = medicalHistoryOtpFailedAttempts;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // --- REFUND AUDIT GETTERS AND SETTERS ---

    public Double getRefundPercentage() {
        return refundPercentage;
    }

    public void setRefundPercentage(Double refundPercentage) {
        this.refundPercentage = refundPercentage;
    }

    public Double getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(Double refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getRefundStatus() {
        return refundStatus;
    }

    public void setRefundStatus(String refundStatus) {
        this.refundStatus = refundStatus;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(String cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }
}