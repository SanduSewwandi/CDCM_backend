package com.example.demo.repository;

import com.example.demo.model.LabTest;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface LabTestRepository extends MongoRepository<LabTest, String> {

    // Get all tests by hospital
    List<LabTest> findByHospitalId(String hospitalId);

    // Get tests by status
    List<LabTest> findByStatus(String status);

    // Get tests by hospital + status (very useful for dashboard)
    List<LabTest> findByHospitalIdAndStatus(String hospitalId, String status);

    // Get tests by patient
    List<LabTest> findByPatientId(String patientId);

    // Search by test type (case-insensitive)
    List<LabTest> findByTestTypeContainingIgnoreCase(String testType);
}