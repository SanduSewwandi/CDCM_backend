package com.example.demo.controller;

import com.example.demo.model.PatientMedicalRecord;
import com.example.demo.service.PatientMedicalRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medical-records")
@CrossOrigin(origins = "http://localhost:5173")
public class PatientMedicalRecordController {

    @Autowired
    private PatientMedicalRecordService service;

    @PostMapping("/add")
    public PatientMedicalRecord addRecord(@RequestBody PatientMedicalRecord record) {
        return service.saveMedicalRecord(record);
    }

    @GetMapping("/patient/{patientId}")
    public List<PatientMedicalRecord> getHistory(@PathVariable String patientId) {
        return service.getHistoryByPatient(patientId);
    }
}