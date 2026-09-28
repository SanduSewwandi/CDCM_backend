package com.example.demo.controller;

import com.example.demo.model.Notification;
import com.example.demo.repository.NotificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationRepository repo;
    private final com.example.demo.service.PatientService patientService;
    private final com.example.demo.service.DoctorService doctorService;
    private final com.example.demo.service.HospitalService hospitalService;

    public NotificationController(NotificationRepository repo,
                                  com.example.demo.service.PatientService patientService,
                                  com.example.demo.service.DoctorService doctorService,
                                  com.example.demo.service.HospitalService hospitalService) {
        this.repo = repo;
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.hospitalService = hospitalService;
    }

    // Get notifications for patient/user
    @GetMapping({ "/{userId}", "/patient/{userId}" })
    public ResponseEntity<?> getNotifications(@PathVariable String userId) {
        try {
            List<Notification> notifications = repo.findByUserIdOrderByCreatedAtDesc(userId);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching notifications: " + e.getMessage());
        }
    }

    // Get notifications for hospital
    @GetMapping("/hospital/{hospitalId}")
    public ResponseEntity<?> getHospitalNotifications(@PathVariable String hospitalId) {
        try {
            List<Notification> notifications = repo.findByUserIdOrderByCreatedAtDesc(hospitalId);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching hospital notifications: " + e.getMessage());
        }
    }

    // Mark as read
    @PutMapping("/read/{id}")
    public ResponseEntity<?> markAsRead(@PathVariable String id) {
        try {
            Notification n = repo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Notification not found with ID: " + id));
            n.setRead(true);
            return ResponseEntity.ok(repo.save(n));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error marking notification as read: " + e.getMessage());
        }
    }

    // ================= NOTIFICATION PREFERENCES =================
    @GetMapping("/preferences")
    public ResponseEntity<?> getPreferences(java.security.Principal principal) {
        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(401).body(java.util.Map.of("message", "Authentication required"));
        }

        String email = principal.getName();
        try {
            try {
                return ResponseEntity.ok(patientService.getNotificationPreference(email));
            } catch (Exception ignored) {}

            try {
                return ResponseEntity.ok(doctorService.getNotificationPreference(email));
            } catch (Exception ignored) {}

            try {
                return ResponseEntity.ok(hospitalService.getNotificationPreference(email));
            } catch (Exception ignored) {}

            return ResponseEntity.ok(new com.example.demo.model.NotificationPreference());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(java.util.Map.of("message", "Error fetching preferences: " + e.getMessage()));
        }
    }

    @PutMapping("/preferences")
    public ResponseEntity<?> updatePreferences(java.security.Principal principal,
                                               @RequestBody com.example.demo.model.NotificationPreference preference) {
        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(401).body(java.util.Map.of("message", "Authentication required"));
        }

        String email = principal.getName();
        try {
            try {
                return ResponseEntity.ok(patientService.updateNotificationPreference(email, preference));
            } catch (Exception ignored) {}

            try {
                return ResponseEntity.ok(doctorService.updateNotificationPreference(email, preference));
            } catch (Exception ignored) {}

            try {
                return ResponseEntity.ok(hospitalService.updateNotificationPreference(email, preference));
            } catch (Exception ignored) {}

            return ResponseEntity.ok(preference);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(java.util.Map.of("message", "Error updating preferences: " + e.getMessage()));
        }
    }
}
