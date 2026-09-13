package com.example.demo.service;

import com.example.demo.model.Feedback;
import com.example.demo.model.Notification;
import com.example.demo.repository.FeedbackRepository;
import com.example.demo.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FeedbackService {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    public Feedback saveFeedback(Feedback feedback) {
        // Automatically set the current time when saving
        feedback.setCreatedAt(LocalDateTime.now());
        Feedback saved = feedbackRepository.save(feedback);

        // Notify Doctor: New Patient Feedback
        try {
            if (saved.getDoctorId() != null && notificationRepository != null) {
                Notification note = new Notification();
                note.setUserId(saved.getDoctorId());
                note.setDoctorId(saved.getDoctorId());
                note.setTitle("New Patient Feedback");
                String commentSnippet = (saved.getComment() != null && !saved.getComment().trim().isEmpty())
                        ? ": \"" + saved.getComment().trim() + "\""
                        : ".";
                note.setMessage("You received a new " + saved.getRating() + "-star feedback rating from a patient" + commentSnippet);
                note.setRead(false);
                note.setCreatedAt(saved.getCreatedAt() != null ? saved.getCreatedAt() : LocalDateTime.now());

                notificationRepository.save(note);
            }
        } catch (Exception e) {
            System.err.println("Failed to create doctor feedback notification: " + e.getMessage());
        }

        return saved;
    }

    public List<Feedback> getFeedbackForDoctor(String doctorId) {
        return feedbackRepository.findByDoctorId(doctorId);
    }
}