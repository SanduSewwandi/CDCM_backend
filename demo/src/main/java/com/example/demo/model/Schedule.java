package com.example.demo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "schedules")
public class Schedule {

    @Id
    private String id;

    // PHYSICAL or VIDEO
    private String type;

    private String doctorId;
    private String hospitalId;

    private String hospitalName;
    private String hospitalLocation;

    private String date;
    private String startTime;
    private String endTime;

    // Used only for VIDEO schedules
    private String meetingLink;

    // Used only for PHYSICAL schedules
    private String roomNumber;

    // Maximum number of patients that can book this schedule
    private int maximumPatients;

    /*
     * Schedule approval status:
     * PENDING, ACCEPTED, REJECTED, CANCELLED
     */
    private String status;

    private String doctorName;
    private String specialty;

    /*
     * Calculated from appointments.
     * These are NOT stored permanently in MongoDB.
     */
    @Transient
    private long bookedPatientCount;

    @Transient
    private long availableSlots;

    public Schedule() {
    }

    public Schedule(
            String doctorId,
            String hospitalId,
            String date,
            String startTime,
            String endTime,
            String type,
            String meetingLink,
            String roomNumber,
            int maximumPatients,
            String status
    ) {
        this.doctorId = doctorId;
        this.hospitalId = hospitalId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.type = type;
        this.meetingLink = meetingLink;
        this.roomNumber = roomNumber;
        this.maximumPatients = maximumPatients;
        this.status = status;
    }

    // =========================
    // ID
    // =========================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // =========================
    // TYPE
    // =========================

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    // =========================
    // DOCTOR
    // =========================

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

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    // =========================
    // HOSPITAL
    // =========================

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

    public String getHospitalLocation() {
        return hospitalLocation;
    }

    public void setHospitalLocation(String hospitalLocation) {
        this.hospitalLocation = hospitalLocation;
    }

    // =========================
    // DATE & TIME
    // =========================

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    // =========================
    // VIDEO
    // =========================

    public String getMeetingLink() {
        return meetingLink;
    }

    public void setMeetingLink(String meetingLink) {
        this.meetingLink = meetingLink;
    }

    // =========================
    // PHYSICAL
    // =========================

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    // =========================
    // PATIENT CAPACITY
    // =========================

    public int getMaximumPatients() {
        return maximumPatients;
    }

    public void setMaximumPatients(int maximumPatients) {
        this.maximumPatients = maximumPatients;
    }

    // =========================
    // STATUS
    // =========================

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // =========================
    // BOOKED PATIENTS
    // =========================

    public long getBookedPatientCount() {
        return bookedPatientCount;
    }

    public void setBookedPatientCount(long bookedPatientCount) {
        this.bookedPatientCount = bookedPatientCount;
    }

    // =========================
    // AVAILABLE SLOTS
    // =========================

    public long getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(long availableSlots) {
        this.availableSlots = availableSlots;
    }
}