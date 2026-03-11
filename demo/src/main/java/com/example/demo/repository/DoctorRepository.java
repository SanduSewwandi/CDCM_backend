package com.example.demo.repository;

import com.example.demo.model.Doctor;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;
import java.util.List;

public interface DoctorRepository extends MongoRepository<Doctor, String> {
    Optional<Doctor> findByEmail(String email);
    Optional<Doctor> findByResetToken(String resetToken);
    List<Doctor> findBySpecializationContainingIgnoreCase(String specialization);
    List<Doctor> findByFirstNameContainingIgnoreCase(String firstName);
}
