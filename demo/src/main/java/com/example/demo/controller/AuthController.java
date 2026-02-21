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
