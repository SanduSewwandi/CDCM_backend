package com.example.demo.controller;

import com.example.demo.dto.AdminLoginRequest;
import com.example.demo.dto.HospitalRegisterRequest;
import com.example.demo.model.Hospital;
import com.example.demo.service.HospitalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final HospitalService hospitalService;

    private final String ADMIN_USERNAME = "admin";
    private final String ADMIN_PASSWORD = "admin123";

    public AdminController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> adminLogin(@RequestBody AdminLoginRequest request) {

        if (ADMIN_USERNAME.equals(request.getUsername())
                && ADMIN_PASSWORD.equals(request.getPassword())) {
            return ResponseEntity.ok("Admin logged in successfully");
        }

        return ResponseEntity.status(401).body("Invalid admin credentials");
    }

    @PostMapping("/register-hospital")
    public ResponseEntity<?> registerHospital(
            @Valid @RequestBody HospitalRegisterRequest request) {

        try {
            Hospital hospital = hospitalService.registerHospital(request);
            return ResponseEntity.ok(hospital);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Server error: " + e.getMessage());
        }
    }
}
