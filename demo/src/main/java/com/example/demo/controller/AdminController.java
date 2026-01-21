package com.example.demo.controller;

import com.example.demo.dto.AdminLoginRequest;
import com.example.demo.dto.HospitalRegisterRequest;
import com.example.demo.model.Hospital;
import com.example.demo.service.HospitalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173") // FIX: Allow Frontend Port
public class AdminController {

    private final HospitalService hospitalService;

    private final String ADMIN_EMAIL = "admin@gmail.com";
    private final String ADMIN_PASSWORD = "admin123";

    public AdminController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> adminLogin(@RequestBody AdminLoginRequest request) {

        // FIX: Use .getEmail()
        if (ADMIN_EMAIL.equals(request.getEmail()) && ADMIN_PASSWORD.equals(request.getPassword())) {

            // FIX: Return JSON Map so frontend doesn't crash
            Map<String, String> response = new HashMap<>();
            response.put("message", "Login Successful");
            response.put("role", "ADMIN");
            response.put("userId", "ADMIN-001"); // Mock ID for context

            return ResponseEntity.ok(response);
        }

        // FIX: Return JSON error
        return ResponseEntity.status(401)
                .body(Collections.singletonMap("message", "Invalid admin credentials"));
    }

    @PostMapping("/register-hospital")
    public ResponseEntity<?> registerHospital(@Valid @RequestBody HospitalRegisterRequest request) {
        try {
            Hospital hospital = hospitalService.registerHospital(request);
            return ResponseEntity.ok(hospital);
        } catch (IllegalArgumentException e) {
            // FIX: Return JSON error
            return ResponseEntity.badRequest().body(Collections.singletonMap("message", e.getMessage()));
        } catch (Exception e) {
            // FIX: Return JSON error
            return ResponseEntity.status(500).body(Collections.singletonMap("message", "Server error: " + e.getMessage()));
        }
    }
}