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

    // Hospital add schedule
    @PostMapping
    public Schedule createSchedule(@RequestBody ScheduleRequest request) {
        return scheduleService.createSchedule(request);
    }

    // Hospital view schedules with optional date filter
    @GetMapping("/hospital/{hospitalId}")
    public List<Schedule> getHospitalSchedules(
            @PathVariable String hospitalId,
            @RequestParam(required = false) String date // optional date query param
    ) {
        if (date != null && !date.isEmpty()) {
            return scheduleService.getHospitalSchedulesByDate(hospitalId, date);
        } else {
            return scheduleService.getHospitalSchedules(hospitalId);
        }
    }

    // Doctor view schedules
    @GetMapping("/doctor/{doctorId}")
    public List<Schedule> getDoctorSchedules(@PathVariable String doctorId) {
        return scheduleService.getDoctorSchedules(doctorId);
    }

    // Accept schedule
    @PutMapping("/accept/{id}")
    public Schedule acceptSchedule(@PathVariable String id) {
        return scheduleService.acceptSchedule(id);
    }

    // Reject schedule
    @PutMapping("/reject/{id}")
    public Schedule rejectSchedule(@PathVariable String id) {
        return scheduleService.rejectSchedule(id);
    }
}