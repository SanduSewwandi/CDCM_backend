package com.example.demo.controller;

import com.example.demo.dto.ScheduleRequest;
import com.example.demo.model.Schedule;
import com.example.demo.service.ScheduleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@CrossOrigin(origins = "http://localhost:5173")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    // ----------------- CREATE SCHEDULE -----------------
    /**
     * Hospital creates a new schedule
     */
    @PostMapping
    public Schedule createSchedule(@RequestBody ScheduleRequest request) {
        return scheduleService.createSchedule(request);
    }

    // ----------------- HOSPITAL SCHEDULES -----------------
    /**
     * Get schedules for a hospital
     * Optional query param 'date' to filter by date
     */
    @GetMapping("/hospital/{hospitalId}")
    public List<Schedule> getHospitalSchedules(
            @PathVariable String hospitalId,
            @RequestParam(required = false) String date
    ) {
        if (date != null && !date.isEmpty()) {
            return scheduleService.getHospitalSchedulesByDate(hospitalId, date);
        } else {
            return scheduleService.getHospitalSchedules(hospitalId);
        }
    }

    // ----------------- DOCTOR SCHEDULES -----------------
    /**
     * Get all schedules assigned to a doctor
     */
    @GetMapping("/doctor/{doctorId}")
    public List<Schedule> getDoctorSchedules(@PathVariable String doctorId) {
        // ✅ Calls service that populates doctorName and hospitalName
        return scheduleService.getDoctorSchedules(doctorId);
    }

    // ----------------- ACCEPT / REJECT SCHEDULE -----------------
    /**
     * Accept a schedule
     */
    @PutMapping("/accept/{id}")
    public Schedule acceptSchedule(@PathVariable String id) {
        return scheduleService.acceptSchedule(id);
    }

    /**
     * Reject a schedule
     */
    @PutMapping("/reject/{id}")
    public Schedule rejectSchedule(@PathVariable String id) {
        return scheduleService.rejectSchedule(id);
    }
}