package com.example.demo.repository;

import com.example.demo.model.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository
        extends MongoRepository<Conversation, String> {

    Optional<Conversation> findByAppointmentId(String appointmentId);

    List<Conversation> findByPatientId(String patientId);

    List<Conversation> findByDoctorId(String doctorId);
}