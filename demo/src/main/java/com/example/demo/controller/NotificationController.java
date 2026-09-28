package com.example.demo.controller;

import com.example.demo.model.Notification;
import com.example.demo.repository.NotificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationRepository repo;

    public NotificationController(NotificationRepository repo) {
        this.repo = repo;
    }

    // Get notifications for patient / doctor / hospital / user
    @GetMapping({ "/{userId}", "/patient/{userId}", "/doctor/{userId}" })
    public ResponseEntity<?> getNotifications(@PathVariable String userId) {
        try {
            List<Notification> byUser = repo.findByUserIdOrderByCreatedAtDesc(userId);
            List<Notification> byHospital = repo.findByHospitalIdOrderByCreatedAtDesc(userId);
            List<Notification> byDoctor = repo.findByDoctorIdOrderByCreatedAtDesc(userId);

            Set<String> seenIds = new HashSet<>();
            List<Notification> combined = new ArrayList<>();

            for (Notification n : byUser) {
                if (n.getId() != null && seenIds.add(n.getId())) {
                    combined.add(n);
                }
            }
            // Fallback for legacy notifications where userId was not explicitly populated:
            for (Notification n : byHospital) {
                if (n.getId() != null && n.getUserId() == null && seenIds.add(n.getId())) {
                    combined.add(n);
                }
            }
            for (Notification n : byDoctor) {
                if (n.getId() != null && n.getUserId() == null && seenIds.add(n.getId())) {
                    combined.add(n);
                }
            }

            // Sort descending by createdAt
            combined.sort((a, b) -> {
                if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                if (a.getCreatedAt() == null) return 1;
                if (b.getCreatedAt() == null) return -1;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            });

            return ResponseEntity.ok(combined);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error fetching notifications: " + e.getMessage()));
        }
    }

    // Get notifications for hospital
    @GetMapping("/hospital/{hospitalId}")
    public ResponseEntity<?> getHospitalNotifications(@PathVariable String hospitalId) {
        return getNotifications(hospitalId);
    }

    // Get unread notification count
    @GetMapping("/{userId}/unread-count")
    public ResponseEntity<?> getUnreadCount(@PathVariable String userId) {
        try {
            Set<String> unreadIds = new HashSet<>();
            for (Notification n : repo.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)) {
                if (n.getId() != null) unreadIds.add(n.getId());
            }
            for (Notification n : repo.findByHospitalIdAndReadFalseOrderByCreatedAtDesc(userId)) {
                if (n.getId() != null && n.getUserId() == null) {
                    unreadIds.add(n.getId());
                }
            }
            for (Notification n : repo.findByDoctorIdAndReadFalseOrderByCreatedAtDesc(userId)) {
                if (n.getId() != null && n.getUserId() == null) {
                    unreadIds.add(n.getId());
                }
            }

            long totalUnread = unreadIds.size();
            return ResponseEntity.ok(Map.of("unreadCount", totalUnread));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error counting unread notifications: " + e.getMessage()));
        }
    }

    // Mark single notification as read
    @PutMapping("/read/{id}")
    public ResponseEntity<?> markAsRead(@PathVariable String id) {
        try {
            Notification n = repo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Notification not found with ID: " + id));
            n.setRead(true);
            return ResponseEntity.ok(repo.save(n));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error marking notification as read: " + e.getMessage()));
        }
    }

    // Mark all notifications as read for a user (patient, doctor, or hospital)
    @PutMapping("/{userId}/read-all")
    public ResponseEntity<?> markAllAsRead(@PathVariable String userId) {
        try {
            List<Notification> toUpdate = new ArrayList<>();
            toUpdate.addAll(repo.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId));
            for (Notification n : repo.findByHospitalIdAndReadFalseOrderByCreatedAtDesc(userId)) {
                if (n.getUserId() == null) {
                    toUpdate.add(n);
                }
            }
            for (Notification n : repo.findByDoctorIdAndReadFalseOrderByCreatedAtDesc(userId)) {
                if (n.getUserId() == null) {
                    toUpdate.add(n);
                }
            }

            Set<String> updatedIds = new HashSet<>();
            List<Notification> finalBatch = new ArrayList<>();
            for (Notification n : toUpdate) {
                if (n.getId() != null && updatedIds.add(n.getId())) {
                    n.setRead(true);
                    finalBatch.add(n);
                }
            }

            if (!finalBatch.isEmpty()) {
                repo.saveAll(finalBatch);
            }

            return ResponseEntity.ok(Map.of("message", "All notifications marked as read", "success", true, "count", finalBatch.size()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error marking all as read: " + e.getMessage()));
        }
    }
}
