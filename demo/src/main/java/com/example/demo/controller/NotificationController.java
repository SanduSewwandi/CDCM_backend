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

    public NotificationController(NotificationRepository repo) {
        this.repo = repo;
    }

    // Get notifications for patient/user
    @GetMapping("/{userId}")
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
}
