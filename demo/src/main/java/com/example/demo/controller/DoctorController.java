package com.example.demo.controller;

import com.example.demo.model.Doctor;
import com.example.demo.service.DoctorService;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    @Autowired
    private DoctorService doctorService;

    // Add these inside PatientController class

    @GetMapping("/{id}")
    public ResponseEntity<?> getDoctor(@PathVariable String id) {
        return ResponseEntity.ok(doctorService.getDoctorById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDoctor(@PathVariable String id, @RequestBody Doctor patient) {
        return ResponseEntity.ok(doctorService.updateDoctor(id, patient));
    }

    // Doctor Sign-up
    @PostMapping("/signup")
    public Doctor signup(@RequestBody Doctor doctor) {
        return doctorService.registerDoctor(doctor);
    }

    // Doctor Login
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        Doctor doctor = doctorService.loginDoctor(request.getEmail(), request.getPassword());
        if(doctor != null) {
            return new LoginResponse("Login Successful", doctor.getId());
        } else {
            return new LoginResponse("Invalid Email or Password", null);
        }
    }

}
