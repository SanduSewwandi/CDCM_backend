package com.example.demo.repository;

import com.example.demo.model.Feedback;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface FeedbackRepository extends MongoRepository<Feedback, String> {
    // This custom method automatically finds all feedback for a specific doctor!
    List<Feedback> findByDoctorId(String doctorId);
}