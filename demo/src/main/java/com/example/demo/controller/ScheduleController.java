package com.example.demo.controller;

import com.example.demo.dto.ScheduleRequest;
import com.example.demo.model.Schedule;
import com.example.demo.service.ScheduleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedules")
@CrossOrigin(
        origins = {
                "http://localhost:5173",
                "https://cdcm-frontend.vercel.app"
        }
)
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    // ----------------- CREATE SCHEDULE -----------------

    @PostMapping
    public Schedule createSchedule(@RequestBody ScheduleRequest request) {
        return scheduleService.createSchedule(request);
    }

    // ----------------- HOSPITAL SCHEDULES -----------------

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

    @GetMapping("/doctor/{doctorId}")
    public List<Schedule> getDoctorSchedules(
            @PathVariable String doctorId
    ) {
        return scheduleService.getDoctorSchedules(doctorId);
    }

    // ----------------- ACCEPT / REJECT SCHEDULE -----------------

    @PutMapping("/accept/{id}")
    public ResponseEntity<?> acceptSchedule(@PathVariable String id, Principal principal) {
        try {
            String doctorEmail = principal != null ? principal.getName() : null;
            Schedule accepted = scheduleService.acceptScheduleByDoctor(id, doctorEmail);
            return ResponseEntity.ok(accepted);
        } catch (SecurityException se) {
            return ResponseEntity.status(403).body(Map.of("message", se.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException iae) {
            return ResponseEntity.badRequest().body(Map.of("message", iae.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/reject/{id}")
    public ResponseEntity<?> rejectSchedule(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            Principal principal) {
        try {
            String reason = body != null ? body.get("reason") : null;
            String doctorEmail = principal != null ? principal.getName() : null;
            Schedule rejected = scheduleService.rejectScheduleByDoctor(id, doctorEmail, reason);
            return ResponseEntity.ok(rejected);
        } catch (SecurityException se) {
            return ResponseEntity.status(403).body(Map.of("message", se.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException iae) {
            return ResponseEntity.badRequest().body(Map.of("message", iae.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    // ----------------- DOCTOR CANCEL SCHEDULE -----------------

    @PutMapping("/cancel/{id}")
    public ResponseEntity<?> cancelSchedule(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            Principal principal) {
        try {
            String reason = body != null ? body.get("reason") : null;
            String doctorEmail = principal != null ? principal.getName() : null;
            Schedule cancelled = scheduleService.cancelScheduleByDoctor(id, doctorEmail, reason);
            return ResponseEntity.ok(cancelled);
        } catch (SecurityException se) {
            return ResponseEntity.status(403).body(Map.of("message", se.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException iae) {
            return ResponseEntity.badRequest().body(Map.of("message", iae.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    // ----------------- HOSPITAL CANCEL SCHEDULE -----------------

    @PutMapping("/hospital-cancel/{id}")
    public ResponseEntity<?> hospitalCancelSchedule(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Authentication required"));
        }
        try {
            String reason = body != null ? body.get("reason") : null;
            Schedule cancelled = scheduleService.cancelScheduleByHospital(id, principal.getName(), reason);
            return ResponseEntity.ok(cancelled);
        } catch (SecurityException se) {
            return ResponseEntity.status(403).body(Map.of("message", se.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException iae) {
            return ResponseEntity.badRequest().body(Map.of("message", iae.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }
}