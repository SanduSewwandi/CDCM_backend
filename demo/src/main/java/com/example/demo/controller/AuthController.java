package com.example.demo.controller;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.PatientRegisterRequest;
import com.example.demo.model.Doctor;
import com.example.demo.model.Hospital;
import com.example.demo.model.Patient;
import com.example.demo.service.DoctorService;
import com.example.demo.service.HospitalService;
import com.example.demo.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    @Autowired
    private PatientService patientService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private HospitalService hospitalService;

    private final String ADMIN_EMAIL = "admin@gmail.com";
    private final String ADMIN_PASSWORD = "admin123";

    // ... (Keep register methods the same) ...
    @PostMapping("/register/patient")
    public ResponseEntity<?> registerPatient(@RequestBody PatientRegisterRequest request) {
        try {
            Patient patient = patientService.registerPatient(request);
            return ResponseEntity.ok(patient);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Registration failed: " + e.getMessage()));
        }
    }

    @PostMapping("/register/doctor")
    public ResponseEntity<?> registerDoctor(@RequestBody Doctor doctor) {
        try {
            Doctor savedDoctor = doctorService.registerDoctor(doctor);
            return ResponseEntity.ok(savedDoctor);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", "Registration failed: " + e.getMessage()));
        }
    }

    // UPDATED LOGIN METHOD
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

        // 1. Admin
        if (ADMIN_EMAIL.equals(request.getEmail()) && ADMIN_PASSWORD.equals(request.getPassword())) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Login Successful");
            response.put("userId", "ADMIN-001");
            response.put("role", "ADMIN");
            response.put("name", "Administrator"); // Send Name
            return ResponseEntity.ok(response);
        }

        // 2. Hospital
        try {
            Hospital hospital = hospitalService.loginHospital(request.getEmail(), request.getPassword());
            if (hospital != null) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Login Successful");
                response.put("userId", hospital.getId());
                response.put("role", "HOSPITAL");
                response.put("name", hospital.getName()); // Send Name
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {}

        // 3. Doctor
        try {
            Doctor doctor = doctorService.loginDoctor(request.getEmail(), request.getPassword());
            if (doctor != null) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Login Successful");
                response.put("userId", doctor.getId());
                response.put("role", "DOCTOR");
                // Send Name (e.g., "Dr. John Doe")
                response.put("name", doctor.getTitle() + " " + doctor.getFirstName() + " " + doctor.getLastName());
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {}

        // 4. Patient
        try {
            Patient patient = patientService.loginPatient(request.getEmail(), request.getPassword());
            if (patient != null) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Login Successful");
                response.put("userId", patient.getId());
                response.put("role", "PATIENT");
                // Send Name
                response.put("name", patient.getFirstName() + " " + patient.getLastName());
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {}

        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", "Invalid Email or Password");
        return ResponseEntity.status(401).body(errorResponse);
    }
}