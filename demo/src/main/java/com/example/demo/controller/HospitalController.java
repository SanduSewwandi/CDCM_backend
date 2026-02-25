package com.example.demo.controller;

import com.example.demo.dto.DoctorDTO;
import com.example.demo.model.Hospital;
import com.example.demo.service.HospitalDoctorService;
import com.example.demo.service.HospitalService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hospitals")
@CrossOrigin(origins = "http://localhost:5173")
public class HospitalController {

    private final HospitalService hospitalService;
    private final HospitalDoctorService hospitalDoctorService;

    public HospitalController(HospitalService hospitalService,
                              HospitalDoctorService hospitalDoctorService) {
        this.hospitalService = hospitalService;
        this.hospitalDoctorService = hospitalDoctorService;
    }

    // Get hospital profile
    @GetMapping("/{hospitalId}")
    public ResponseEntity<Hospital> getHospitalById(@PathVariable String hospitalId) {
        Hospital hospital = hospitalService.getHospitalById(hospitalId);
        return ResponseEntity.ok(hospital);
    }

    // Get all doctors in a hospital
    @GetMapping("/{hospitalId}/doctors")
    @PreAuthorize("hasAuthority('HOSPITAL') or hasAuthority('ADMIN')")
    public ResponseEntity<List<DoctorDTO>> getHospitalDoctors(@PathVariable String hospitalId) {
        List<DoctorDTO> doctors = hospitalDoctorService.getDoctorsByHospital(hospitalId);
        return ResponseEntity.ok(doctors);
    }

    // Add doctor to hospital
    @PostMapping("/{hospitalId}/doctors/{doctorId}")
    @PreAuthorize("hasAuthority('HOSPITAL') or hasAuthority('ADMIN')")
    public ResponseEntity<?> addDoctorToHospital(
            @PathVariable String hospitalId,
            @PathVariable String doctorId) {

        hospitalDoctorService.addDoctorToHospital(hospitalId, doctorId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Doctor added to hospital successfully");
        return ResponseEntity.ok(response);
    }

    // Remove doctor from hospital
    @DeleteMapping("/{hospitalId}/doctors/{doctorId}")
    @PreAuthorize("hasAuthority('HOSPITAL') or hasAuthority('ADMIN')")
    public ResponseEntity<?> removeDoctorFromHospital(
            @PathVariable String hospitalId,
            @PathVariable String doctorId) {

        hospitalDoctorService.removeDoctorFromHospital(hospitalId, doctorId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Doctor removed from hospital successfully");
        return ResponseEntity.ok(response);
    }
}