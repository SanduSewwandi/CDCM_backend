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
import com.example.demo.model.Admin;
import com.example.demo.service.AdminService;
import com.example.demo.security.JwtService;



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
        this.jwtService = jwtService;   // 🔥 VERY IMPORTANT
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestBody java.util.Map<String, String> request) {

        String email = request.get("email");
        String code = request.get("code");
        String role = request.get("role");

        // 1. Basic Validation
        if (email == null || code == null || role == null) {
            return ResponseEntity.badRequest()
                    .body(java.util.Collections.singletonMap("message", "Email, code and role are required"));
        }

        String result;

        // 2. Wrap the service calls in a try-catch block
        try {
            if ("PATIENT".equalsIgnoreCase(role)) {
                result = patientService.verifyOtp(email, code);
            }
            else if ("DOCTOR".equalsIgnoreCase(role)) {
                result = doctorService.verifyOtp(email, code);
            }
            else {
                return ResponseEntity.badRequest()
                        .body(java.util.Collections.singletonMap("message", "Invalid role"));
            }

            // 3. Handle the success/failure result from the service
            if ("Email verified successfully".equals(result)) {
                return ResponseEntity.ok(
                        java.util.Collections.singletonMap("message", result)
                );
            }

            return ResponseEntity.badRequest()
                    .body(java.util.Collections.singletonMap("message", result));

        } catch (Exception e) {
            // This will print the EXACT error in your IntelliJ/IDE console
            e.printStackTrace();

            // This stops the generic "Server Error" on the frontend and gives you a hint
            return ResponseEntity.status(500)
                    .body(java.util.Collections.singletonMap("message", "Backend Error: " + e.getMessage()));
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

    @GetMapping("/patient/{id}")
    public ResponseEntity<Patient> getPatientProfile(@PathVariable String id) {
        return ResponseEntity.ok(patientService.getPatientById(id));
    }

    @PutMapping("/patient/{id}")
    public ResponseEntity<Patient> updatePatientProfile(@PathVariable String id, @RequestBody Patient patient) {
        return ResponseEntity.ok(patientService.updatePatient(id, patient));
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

    @GetMapping("/doctors/{id}")
    public ResponseEntity<Doctor> getDoctorProfile(@PathVariable String id) {
        return ResponseEntity.ok(doctorService.getDoctorById(id));
    }

    @PutMapping("/doctors/{id}")
    public ResponseEntity<Doctor> updateDoctorProfile(@PathVariable String id, @RequestBody Doctor doctor) {
        return ResponseEntity.ok(doctorService.updateDoctor(id, doctor));
    }

    // =========================
    // LOGIN (ALL ROLES)
    // =========================


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

            String token = jwtService.generateToken(
                    admin.getEmail(),
                    "ADMIN"
            );

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

            String token = jwtService.generateToken(
                    doctor.getEmail(),
                    "DOCTOR"
            );

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

            String token = jwtService.generateToken(
                    patient.getEmail(),
                    "PATIENT"
            );

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

        // ❌ INVALID LOGIN
        return ResponseEntity.status(401)
                .body(new LoginResponse(
                        "Invalid Email or Password",
                        null,
                        null,
                        null,
                        null
                ));
    }
}
