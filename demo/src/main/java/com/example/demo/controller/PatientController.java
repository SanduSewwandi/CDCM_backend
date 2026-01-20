package com.example.demo.controller;

<<<<<<< HEAD
import com.example.demo.dto.PatientLoginRequest;
import com.example.demo.dto.PatientRegisterRequest;
import com.example.demo.model.Patient;
import com.example.demo.service.PatientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patient")
@CrossOrigin(origins = "*")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    // Signup
    @PostMapping("/signup")
    public ResponseEntity<?> registerPatient(@RequestBody PatientRegisterRequest request) {
        try {
            Patient patient = patientService.registerPatient(request);
            return ResponseEntity.ok(patient);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Login
    @PostMapping("/login")
    public ResponseEntity<?> loginPatient(@RequestBody PatientLoginRequest request) {
        try {
            Patient patient = patientService.loginPatient(request.getEmail(), request.getPassword());
            return ResponseEntity.ok(patient);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
=======
public class PatientController {
>>>>>>> 89e3041278f620dff59a1d43465db79ea34a6c27
}
