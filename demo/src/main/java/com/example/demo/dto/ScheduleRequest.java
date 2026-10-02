package com.example.demo.dto;

public class ScheduleRequest {

    private String doctorId;
    private String hospitalId;

    private String date;
    private String startTime;
    private String endTime;

    // PHYSICAL / VIDEO
    private String type;

    // For VIDEO schedules only
    private String meetingLink;

    // For PHYSICAL schedules only
    private String roomNumber;

    // Maximum number of patients allowed
    private int maximumPatients;


    // =========================
    // Default Constructor
    // =========================

    public ScheduleRequest() {
    }


    // =========================
    // Parameterized Constructor
    // =========================

    public ScheduleRequest(
            String doctorId,
            String hospitalId,
            String date,
            String startTime,
            String endTime,
            String type,
            String meetingLink,
            String roomNumber,
            int maximumPatients) {

        this.doctorId = doctorId;
        this.hospitalId = hospitalId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.type = type;
        this.meetingLink = meetingLink;
        this.roomNumber = roomNumber;
        this.maximumPatients = maximumPatients;
    }


    // =========================
    // Doctor ID
    // =========================

    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }


    // =========================
    // Hospital ID
    // =========================

    public String getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(String hospitalId) {
        this.hospitalId = hospitalId;
    }


    // =========================
    // Date
    // =========================

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }


    // =========================
    // Start Time
    // =========================

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }


    // =========================
    // End Time
    // =========================

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }


    // =========================
    // Schedule Type
    // =========================

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    // =========================
    // Meeting Link
    // =========================

    public String getMeetingLink() {
        return meetingLink;
    }

    public void setMeetingLink(String meetingLink) {
        this.meetingLink = meetingLink;
    }


    // =========================
    // Room Number
    // =========================

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }


    // =========================
    // Maximum Patients
    // =========================

    public int getMaximumPatients() {
        return maximumPatients;
    }

    public void setMaximumPatients(int maximumPatients) {
        this.maximumPatients = maximumPatients;
    }
}