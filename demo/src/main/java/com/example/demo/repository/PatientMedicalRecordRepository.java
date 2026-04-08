package com.example.demo.repository;

import com.example.demo.model.PatientMedicalRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface PatientMedicalRecordRepository extends MongoRepository<PatientMedicalRecord, String> {
    // ✅ CORRECT: Matches 'dateOfVisit' field name
    List<PatientMedicalRecord> findByPatientIdOrderByDateOfVisitDesc(String patientId);
}