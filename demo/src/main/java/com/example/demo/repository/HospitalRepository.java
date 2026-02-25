package com.example.demo.repository;

import com.example.demo.model.Hospital;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface HospitalRepository extends MongoRepository<Hospital, String> {
    Optional<Hospital> findByEmail(String email);
}