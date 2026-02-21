package com.example.demo.controller;

import com.example.demo.dto.HospitalRegisterRequest;
import com.example.demo.model.Hospital;
import com.example.demo.service.HospitalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final HospitalService hospitalService;

    public AdminController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @PostMapping("/register-hospital")
    public ResponseEntity<?> registerHospital(
            @RequestBody HospitalRegisterRequest request) {

        Hospital hospital =
                hospitalService.registerHospital(request);

        return ResponseEntity.ok(hospital);
    }
}
