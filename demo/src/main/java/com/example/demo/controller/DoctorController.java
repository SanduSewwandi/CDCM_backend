package com.example.demo.controller;

import com.example.demo.dto.DoctorAccountDTO;
import com.example.demo.model.Doctor;
import com.example.demo.service.DoctorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/doctors")
@CrossOrigin(origins = "http://localhost:5173")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    // =========================
    // DOCTOR PROFILE (Profile.jsx)
    // =========================
    @GetMapping("/{id}")
    public ResponseEntity<Doctor> getDoctorProfile(@PathVariable String id) {
        return ResponseEntity.ok(doctorService.getDoctorById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Doctor> updateDoctorProfile(
            @PathVariable String id,
            @RequestBody Doctor doctor
    ) {
        return ResponseEntity.ok(doctorService.updateDoctor(id, doctor));
    }

    // =========================
    // DOCTOR ACCOUNT (Account.jsx)
    // =========================
    @GetMapping("/{id}/account")
    public ResponseEntity<DoctorAccountDTO> getDoctorAccount(@PathVariable String id) {
        return ResponseEntity.ok(doctorService.getDoctorAccount(id));
    }

    @PutMapping("/{id}/account")
    public ResponseEntity<DoctorAccountDTO> updateDoctorAccount(
            @PathVariable String id,
            @RequestBody DoctorAccountDTO dto
    ) {
        return ResponseEntity.ok(doctorService.updateDoctorAccount(id, dto));
    }
}