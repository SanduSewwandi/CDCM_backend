package com.example.demo.controller;

import com.example.demo.model.Patient;
import com.example.demo.service.PatientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/patients")
@CrossOrigin(origins = "http://localhost:5173")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    // =========================
    // PATIENT PROFILE (Profile.jsx)
    // =========================

    @GetMapping("/{id}")
    public ResponseEntity<Patient> getPatientProfile(@PathVariable String id) {
        return ResponseEntity.ok(patientService.getPatientById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Patient> updatePatientProfile(
            @PathVariable String id,
            @RequestBody Patient patient
    ) {
        return ResponseEntity.ok(patientService.updatePatient(id, patient));
    }

}