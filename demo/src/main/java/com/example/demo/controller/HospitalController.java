package com.example.demo.controller;

import com.example.demo.dto.HospitalLoginRequest;
import com.example.demo.model.Hospital;
import com.example.demo.service.HospitalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hospital")
@CrossOrigin(origins = "*")
public class HospitalController {

    private final HospitalService hospitalService;

    public HospitalController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginHospital(
            @Valid @RequestBody HospitalLoginRequest request) {

        try {
            Hospital hospital = hospitalService
                    .loginHospital(request.getEmail(), request.getPassword());
            return ResponseEntity.ok(hospital);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Server error: " + e.getMessage());
        }
    }
}
