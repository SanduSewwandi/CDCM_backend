package com.example.demo.controller;

import com.example.demo.dto.DoctorRegisterRequest;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.PatientRegisterRequest;
import com.example.demo.model.Doctor;
import com.example.demo.model.Hospital;
import com.example.demo.model.Patient;
import com.example.demo.service.DoctorService;
import com.example.demo.service.HospitalService;
import com.example.demo.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final PatientService patientService;
    private final DoctorService doctorService;
    private final HospitalService hospitalService;

    // Temporary admin (will replace with DB later)
    private final String ADMIN_EMAIL = "admin@gmail.com";
    private final String ADMIN_PASSWORD = "admin123";

    public AuthController(PatientService patientService,
                          DoctorService doctorService,
                          HospitalService hospitalService) {
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.hospitalService = hospitalService;
    }

    // =========================
    // REGISTER PATIENT
    // =========================
    @PostMapping("/register/patient")
    public ResponseEntity<?> registerPatient(
            @Valid @RequestBody PatientRegisterRequest request) {

        Patient patient = patientService.registerPatient(request);
        return ResponseEntity.ok(patient);
    }

    // =========================
    // REGISTER DOCTOR
    // =========================
    @PostMapping("/register/doctor")
    public ResponseEntity<?> registerDoctor(
            @Valid @RequestBody DoctorRegisterRequest request) {

        Doctor doctor = doctorService.registerDoctor(request);
        return ResponseEntity.ok(doctor);
    }

    // =========================
    // LOGIN (ALL ROLES)
    // =========================
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        // 1️⃣ ADMIN
        if (ADMIN_EMAIL.equals(request.getEmail()) &&
                ADMIN_PASSWORD.equals(request.getPassword())) {

            return ResponseEntity.ok(
                    new LoginResponse(
                            "Login Successful",
                            "ADMIN",
                            "ADMIN-001",
                            "Administrator"
                    )
            );
        }

        // 2️⃣ HOSPITAL
        Hospital hospital = hospitalService
                .loginHospital(request.getEmail(), request.getPassword());

        if (hospital != null) {
            return ResponseEntity.ok(
                    new LoginResponse(
                            "Login Successful",
                            "HOSPITAL",
                            hospital.getId(),
                            hospital.getName()
                    )
            );
        }

        // 3️⃣ DOCTOR
        Doctor doctor = doctorService
                .loginDoctor(request.getEmail(), request.getPassword());

        if (doctor != null) {
            return ResponseEntity.ok(
                    new LoginResponse(
                            "Login Successful",
                            "DOCTOR",
                            doctor.getId(),
                            doctor.getTitle() + " " +
                                    doctor.getFirstName() + " " +
                                    doctor.getLastName()
                    )
            );
        }

        // 4️⃣ PATIENT
        Patient patient = patientService
                .loginPatient(request.getEmail(), request.getPassword());

        if (patient != null) {
            return ResponseEntity.ok(
                    new LoginResponse(
                            "Login Successful",
                            "PATIENT",
                            patient.getId(),
                            patient.getFirstName() + " " +
                                    patient.getLastName()
                    )
            );
        }

        // ❌ INVALID LOGIN
        return ResponseEntity.status(401)
                .body(new LoginResponse(
                        "Invalid Email or Password",
                        null,
                        null,
                        null
                ));
    }
}
