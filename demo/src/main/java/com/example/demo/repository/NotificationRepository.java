package com.example.demo.repository;

import com.example.demo.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Notification> findByHospitalIdOrderByCreatedAtDesc(String hospitalId);

    List<Notification> findByDoctorIdOrderByCreatedAtDesc(String doctorId);

    long countByUserIdAndReadFalse(String userId);

    long countByHospitalIdAndReadFalse(String hospitalId);

    long countByDoctorIdAndReadFalse(String doctorId);

    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(String userId);

    List<Notification> findByHospitalIdAndReadFalseOrderByCreatedAtDesc(String hospitalId);

    List<Notification> findByDoctorIdAndReadFalseOrderByCreatedAtDesc(String doctorId);
}