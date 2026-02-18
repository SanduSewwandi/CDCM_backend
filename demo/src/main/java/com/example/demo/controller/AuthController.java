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


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final PatientService patientService;
    private final DoctorService doctorService;
    private final HospitalService hospitalService;
    private final AdminService adminService;




    public AuthController(PatientService patientService,
                          DoctorService doctorService,
                          HospitalService hospitalService,
                          AdminService adminService) {

        this.patientService = patientService;
        this.doctorService = doctorService;
        this.hospitalService = hospitalService;
        this.adminService = adminService;  // 🔥 IMPORTANT
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
        Admin admin = adminService.loginAdmin(request.getEmail(), request.getPassword());

        if (admin != null) {
            return ResponseEntity.ok(
                    new LoginResponse(
                            "Login Successful",
                            "ADMIN",
                            admin.getId(),
                            admin.getName()
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
