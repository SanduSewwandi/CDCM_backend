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

    // VERIFY EMAIL (OTP)

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
            } else if ("HOSPITAL".equalsIgnoreCase(role)) {
                result = hospitalService.verifyOtp(email, code);
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


    // REGISTER PATIENT

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

    // REGISTER DOCTOR

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

    // LOGIN (ALL ROLES)

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
            String token = jwtService.generateToken(
                    hospital.getEmail(),
                    "HOSPITAL"
            );

            LoginResponse response = new LoginResponse(
                    "Login Successful",
                    "HOSPITAL",
                    hospital.getId(),
                    hospital.getName(),
                    token
            );

            response.setEmail(hospital.getEmail());
            response.setVerified(hospital.isVerified());
            response.setMustChangePassword(
                    hospital.isMustChangePassword()
            );
            response.setProfileImage(hospital.getProfileImage());

            return ResponseEntity.ok(response);
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

            LoginResponse response = new LoginResponse(
                    "Login Successful",
                    "DOCTOR",
                    doctor.getId(),
                    doctor.getTitle() + " " +
                            doctor.getFirstName() + " " +
                            doctor.getLastName(),
                    token
            );
            response.setEmail(doctor.getEmail());
            response.setVerified(doctor.isVerified());
            response.setProfileImage(doctor.getProfileImage());

            return ResponseEntity.ok(response);
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

            LoginResponse response = new LoginResponse(
                    "Login Successful",
                    "PATIENT",
                    patient.getId(),
                    patient.getFirstName() + " " +
                            patient.getLastName(),
                    token
            );
            response.setEmail(patient.getEmail());
            response.setVerified(patient.isVerified());
            response.setProfileImage(patient.getProfileImage());

            return ResponseEntity.ok(response);
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

    // FORGOT PASSWORD

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @RequestParam String email,
            @RequestParam String role) {

        try {

            boolean sent = false;

            switch (role.toUpperCase()) {

                case "PATIENT":
                    sent = patientService.sendPasswordResetEmail(email);
                    break;

                case "DOCTOR":
                    sent = doctorService.sendPasswordResetEmail(email);
                    break;

                case "HOSPITAL":
                    sent = hospitalService.sendPasswordResetEmail(email);
                    break;

                default:
                    return ResponseEntity.badRequest()
                            .body(java.util.Collections.singletonMap(
                                    "message",
                                    "Invalid role"
                            ));
            }

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
        String role = request.get("role");

        if (token == null || newPassword == null || role == null) {
            return ResponseEntity.badRequest().body("Token, password and role required");
        }

        boolean success = false;

        switch (role.toUpperCase()) {

            case "PATIENT":
                success = patientService.resetPassword(token.trim(), newPassword);
                break;

            case "DOCTOR":
                success = doctorService.resetPassword(token.trim(), newPassword);
                break;

            case "HOSPITAL":
                success = hospitalService.resetPassword(token.trim(), newPassword);
                break;

            default:
                return ResponseEntity.badRequest().body("Invalid role");
        }

        if (success) {
            return ResponseEntity.ok("Password reset successful");
        }

        return ResponseEntity.badRequest().body("Invalid or expired token");
    }

    // AUTHENTICATED CHANGE PASSWORD (ALL ROLES)
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            java.security.Principal principal,
            @Valid @RequestBody com.example.demo.dto.ChangePasswordRequest request) {

        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(401)
                    .body(java.util.Map.of("message", "Authentication required"));
        }

        String email = principal.getName();
        String currentPassword = request.getCurrentPassword();
        String newPassword = request.getNewPassword();

        if (currentPassword == null || currentPassword.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("message", "Current password is required"));
        }

        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("message", "New password must be at least 6 characters long"));
        }

        if (request.getConfirmPassword() != null && !request.getConfirmPassword().isBlank()) {
            if (!newPassword.equals(request.getConfirmPassword())) {
                return ResponseEntity.badRequest()
                        .body(java.util.Map.of("message", "New password and confirmation do not match"));
            }
        }

        try {
            // Check PATIENT
            try {
                patientService.changePassword(email, currentPassword, newPassword);
                return ResponseEntity.ok(java.util.Map.of("message", "Password changed successfully"));
            } catch (RuntimeException e) {
                if ("Current password does not match".equals(e.getMessage())) {
                    return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
                }
            }

            // Check DOCTOR
            try {
                doctorService.changePassword(email, currentPassword, newPassword);
                return ResponseEntity.ok(java.util.Map.of("message", "Password changed successfully"));
            } catch (RuntimeException e) {
                if ("Current password does not match".equals(e.getMessage())) {
                    return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
                }
            }

            // Check HOSPITAL
            try {
                hospitalService.changePassword(email, currentPassword, newPassword);
                return ResponseEntity.ok(java.util.Map.of("message", "Password changed successfully"));
            } catch (RuntimeException e) {
                if ("Current password does not match".equals(e.getMessage())) {
                    return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
                }
            }

            return ResponseEntity.status(404).body(java.util.Map.of("message", "User account not found"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(java.util.Map.of("message", "Error changing password: " + e.getMessage()));
        }
    }

}