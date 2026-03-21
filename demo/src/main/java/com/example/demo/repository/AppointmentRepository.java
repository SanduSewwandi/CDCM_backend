package com.example.demo.repository;

import com.example.demo.model.Appointment;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface AppointmentRepository extends MongoRepository<Appointment, String> {
    List<Appointment> findByPatientId(String patientId);

    // This helps us auto-generate the appointment number
    long countByScheduleId(String scheduleId);
    List<Appointment> findByScheduleId(String scheduleId);
}