package com.example.demo.dto;

public class RefundPreviewDTO {

    private String appointmentId;
    private String appointmentNumber;
    private String patientId;
    private String patientName;
    private String doctorId;
    private String doctorName;
    private String hospitalId;
    private String hospitalName;
    private String date;
    private String time;
    private double originalPayment;
    private double refundPercentage;
    private double eligibleRefund;
    private double hoursRemaining;
    private boolean isPaid;
    private String status;
    private boolean eligibleForRefund;
    private String message;

    public RefundPreviewDTO() {}

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getAppointmentNumber() {
        return appointmentNumber;
    }

    public void setAppointmentNumber(String appointmentNumber) {
        this.appointmentNumber = appointmentNumber;
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

    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(String hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public double getOriginalPayment() {
        return originalPayment;
    }

    public void setOriginalPayment(double originalPayment) {
        this.originalPayment = originalPayment;
    }

    public double getRefundPercentage() {
        return refundPercentage;
    }

    public void setRefundPercentage(double refundPercentage) {
        this.refundPercentage = refundPercentage;
    }

    public double getEligibleRefund() {
        return eligibleRefund;
    }

    public void setEligibleRefund(double eligibleRefund) {
        this.eligibleRefund = eligibleRefund;
    }

    public double getHoursRemaining() {
        return hoursRemaining;
    }

    public void setHoursRemaining(double hoursRemaining) {
        this.hoursRemaining = hoursRemaining;
    }

    public boolean isPaid() {
        return isPaid;
    }

    public void setPaid(boolean paid) {
        isPaid = paid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isEligibleForRefund() {
        return eligibleForRefund;
    }

    public void setEligibleForRefund(boolean eligibleForRefund) {
        this.eligibleForRefund = eligibleForRefund;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
