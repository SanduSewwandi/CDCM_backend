package com.example.demo.service;

import com.example.demo.model.PatientMedicalRecord;
import com.example.demo.repository.PatientMedicalRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PatientMedicalRecordService {

    @Autowired
    private PatientMedicalRecordRepository repository;

    public PatientMedicalRecord saveMedicalRecord(PatientMedicalRecord record) {
        // Automatically set the date of visit to today if empty
        if (record.getDateOfVisit() == null || record.getDateOfVisit().isEmpty()) {
            record.setDateOfVisit(LocalDate.now().toString());
        }
        return repository.save(record);
    }

    public List<PatientMedicalRecord> getHistoryByPatient(String patientId) {
        //  Use the new method name here
        return repository.findByPatientIdOrderByDateOfVisitDesc(patientId);
    }
}