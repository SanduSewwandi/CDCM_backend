package com.example.demo.controller;

import com.example.demo.dto.DoctorRegisterRequest;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.PatientRegisterRequest;
import com.example.demo.model.Doctor;
import com.example.demo.model.Hospital;
import com.example.demo.model.Patient;
import com.example.demo.model.Admin;
import com.example.demo.service.DoctorService;
import com.example.demo.service.HospitalService;
import com.example.demo.service.PatientService;
import com.example.demo.service.AdminService;
import com.example.demo.security.JwtService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final PatientService patientService;
    private final DoctorService doctorService;
    private final HospitalService hospitalService;
    private final AdminService adminService;
    private final JwtService jwtService;


    public AuthController(PatientService patientService,
                          DoctorService doctorService,
                          HospitalService hospitalService,
                          AdminService adminService,
                          JwtService jwtService) {

        this.patientService = patientService;
        this.doctorService = doctorService;
        this.hospitalService = hospitalService;
        this.adminService = adminService;
        this.jwtService = jwtService;
    }

    // =========================
    // VERIFY EMAIL (OTP)
    // =========================
    @PostMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestBody java.util.Map<String, String> request) {

        String email = request.get("email");
        String code = request.get("code");
        String role = request.get("role");

        if (email == null || code == null || role == null) {
            return ResponseEntity.badRequest()
                    .body(java.util.Collections.singletonMap(
                            "message",
                            "Email, code and role are required"
                    ));
        }

        try {
            String result;

            if ("PATIENT".equalsIgnoreCase(role)) {
                result = patientService.verifyOtp(email, code);
            } else if ("DOCTOR".equalsIgnoreCase(role)) {
                result = doctorService.verifyOtp(email, code);
            } else {
                return ResponseEntity.badRequest()
                        .body(java.util.Collections.singletonMap(
                                "message",
                                "Invalid role"
                        ));
            }

            if ("Email verified successfully".equals(result)) {
                return ResponseEntity.ok(
                        java.util.Collections.singletonMap("message", result)
                );
            }

            return ResponseEntity.badRequest()
                    .body(java.util.Collections.singletonMap("message", result));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(java.util.Collections.singletonMap(
                            "message",
                            "Backend Error: " + e.getMessage()
                    ));
        }
    }

    // =========================
    // REGISTER PATIENT
    // =========================
    @PostMapping("/register/patient")
    public ResponseEntity<?> registerPatient(
            @Valid @RequestBody PatientRegisterRequest request) {

        patientService.registerPatient(request);

        return ResponseEntity.ok(
                java.util.Collections.singletonMap(
                        "message",
                        "Registration successful. Please verify your email."
                )
        );
    }

    // =========================
    // REGISTER DOCTOR
    // =========================
    @PostMapping("/register/doctor")
    public ResponseEntity<?> registerDoctor(
            @Valid @RequestBody DoctorRegisterRequest request) {

        doctorService.registerDoctor(request);

        return ResponseEntity.ok(
                java.util.Collections.singletonMap(
                        "message",
                        "Registration successful. Please verify your email."
                )
        );
    }

    // =========================
    // LOGIN (ALL ROLES)
    // =========================
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        // ADMIN
        Admin admin = adminService.loginAdmin(
                request.getEmail(),
                request.getPassword()
        );

        if (admin != null) {
            String token = jwtService.generateToken(admin.getEmail(), "ADMIN");

            return ResponseEntity.ok(
                    new LoginResponse(
                            "Login Successful",
                            "ADMIN",
                            admin.getId(),
                            admin.getName(),
                            token
                    )
            );
        }

        // HOSPITAL
        Hospital hospital = hospitalService.loginHospital(
                request.getEmail(),
                request.getPassword()
        );

        if (hospital != null) {
            String token = jwtService.generateToken(hospital.getEmail(), "HOSPITAL");

            return ResponseEntity.ok(
                    new LoginResponse(
                            "Login Successful",
                            "HOSPITAL",
                            hospital.getId(),
                            hospital.getName(),
                            token
                    )
            );
        }

        // DOCTOR
        Doctor doctor = doctorService.loginDoctor(
                request.getEmail(),
                request.getPassword()
        );

        if (doctor != null) {

            if (!doctor.isVerified()) {
                return ResponseEntity.status(403)
                        .body(new LoginResponse(
                                "Please verify your email first",
                                null,
                                null,
                                null,
                                null
                        ));
            }

            String token = jwtService.generateToken(doctor.getEmail(), "DOCTOR");

            return ResponseEntity.ok(
                    new LoginResponse(
                            "Login Successful",
                            "DOCTOR",
                            doctor.getId(),
                            doctor.getTitle() + " " +
                                    doctor.getFirstName() + " " +
                                    doctor.getLastName(),
                            token
                    )
            );
        }

        // PATIENT
        Patient patient = patientService.loginPatient(
                request.getEmail(),
                request.getPassword()
        );

        if (patient != null) {

            if (!patient.isVerified()) {
                return ResponseEntity.status(403)
                        .body(new LoginResponse(
                                "Please verify your email first",
                                null,
                                null,
                                null,
                                null
                        ));
            }

            String token = jwtService.generateToken(patient.getEmail(), "PATIENT");

            return ResponseEntity.ok(
                    new LoginResponse(
                            "Login Successful",
                            "PATIENT",
                            patient.getId(),
                            patient.getFirstName() + " " +
                                    patient.getLastName(),
                            token
                    )
            );
        }

        // INVALID LOGIN
        return ResponseEntity.status(401)
                .body(new LoginResponse(
                        "Invalid Email or Password",
                        null,
                        null,
                        null,
                        null
                ));
    }

    // =========================
    // FORGOT PASSWORD
    // =========================
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {
        try {
            boolean sent = patientService.sendPasswordResetEmail(email);

            if (sent) {
                return ResponseEntity.ok(
                        java.util.Collections.singletonMap(
                                "message",
                                "Password reset email sent"
                        )
                );
            } else {
                return ResponseEntity.badRequest()
                        .body(java.util.Collections.singletonMap(
                                "message",
                                "Email not found"
                        ));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(java.util.Collections.singletonMap(
                            "message",
                            "Backend Error: " + e.getMessage()
                    ));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody java.util.Map<String, String> request) {

        String token = request.get("token");
        String newPassword = request.get("newPassword");

        if (token == null || newPassword == null) {
            return ResponseEntity.badRequest().body("Token and password required");
        }

        boolean success = patientService.resetPassword(token.trim(), newPassword);

        if (success) {
            return ResponseEntity.ok("Password reset successful");
        }

        return ResponseEntity.badRequest().body("Invalid or expired token");
    }

}