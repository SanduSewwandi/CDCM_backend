package com.example.demo.controller;

import com.example.demo.model.Hospital;
import com.example.demo.service.HospitalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import com.example.demo.dto.HospitalPasswordChangeRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
import com.example.demo.dto.HospitalProfileDTO;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/hospitals")
@CrossOrigin(origins = "http://localhost:5173")
public class HospitalController {

    private final HospitalService hospitalService;

    public HospitalController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    // Get all hospitals
    @GetMapping("/all")
    public ResponseEntity<List<Hospital>> getAllHospitals() {
        try {
            List<Hospital> hospitals = hospitalService.getAllHospitals();
            return ResponseEntity.ok(hospitals);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    // Get single hospital by ID
    @GetMapping("/{id}")
    public ResponseEntity<Hospital> getHospitalById(@PathVariable String id) {
        try {
            Hospital hospital = hospitalService.getHospitalById(id);
            if (hospital == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(hospital);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @PutMapping("/first-login-password")
    public ResponseEntity<?> changeFirstLoginPassword(
            Principal principal,
            @Valid @RequestBody HospitalPasswordChangeRequest request) {

        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Authentication required"));
        }

        hospitalService.changeFirstLoginPassword(
                principal.getName(),
                request.getNewPassword()
        );

        return ResponseEntity.ok(
                Map.of("message", "Password changed successfully")
        );
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(Map.of(
                            "message",
                            "Authentication required"
                    ));
        }

        HospitalProfileDTO profile =
                hospitalService.getMyProfile(
                        principal.getName()
                );

        return ResponseEntity.ok(profile);
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMyProfile(
            Principal principal,
            @RequestBody HospitalProfileDTO request) {

        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(Map.of(
                            "message",
                            "Authentication required"
                    ));
        }

        HospitalProfileDTO updated =
                hospitalService.updateMyProfile(
                        principal.getName(),
                        request
                );

        return ResponseEntity.ok(updated);
    }
}