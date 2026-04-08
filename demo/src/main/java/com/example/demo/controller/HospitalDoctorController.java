package com.example.demo.controller;

import com.example.demo.model.Doctor;
import com.example.demo.model.Hospital;
import com.example.demo.service.DoctorService;
import com.example.demo.service.HospitalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hospital/doctors")
@CrossOrigin(origins = "http://localhost:5173")
public class HospitalDoctorController {

    private final DoctorService doctorService;
    private final HospitalService hospitalService;

    public HospitalDoctorController(DoctorService doctorService, HospitalService hospitalService) {
        this.doctorService = doctorService;
        this.hospitalService = hospitalService;
    }

    // 🔎 Search all registered doctors (optional keyword)
    @GetMapping("/search")
    public ResponseEntity<List<Doctor>> searchDoctors(@RequestParam(required = false) String keyword) {
        try {
            List<Doctor> doctors = doctorService.searchDoctors(keyword);
            return ResponseEntity.ok(doctors);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Collections.emptyList());
        }
    }

    // ➕ Assign doctor to hospital
    @PutMapping("/{doctorId}/assign/{hospitalId}")
    public ResponseEntity<?> assignDoctor(@PathVariable String doctorId, @PathVariable String hospitalId) {
        try {
            Doctor assignedDoctor = doctorService.assignDoctorToHospital(doctorId, hospitalId);
            if (assignedDoctor == null) {
                return ResponseEntity.status(404)
                        .body(Collections.singletonMap("message", "Doctor or Hospital not found"));
            }
            return ResponseEntity.ok(assignedDoctor);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Collections.singletonMap("message", "Backend error: " + e.getMessage()));
        }
    }

    // 🏥 Get doctors of a specific hospital
    @GetMapping("/hospital/{hospitalId}")
    public ResponseEntity<?> getHospitalDoctors(@PathVariable String hospitalId) {
        try {
            List<Doctor> doctors = doctorService.getDoctorsByHospital(hospitalId);
            return ResponseEntity.ok(doctors);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Collections.singletonMap("message", "Backend error: " + e.getMessage()));
        }
    }

    // ❌ Remove doctor from hospital
    @DeleteMapping("/{doctorId}/remove/{hospitalId}")
    public ResponseEntity<?> removeDoctorFromHospital(@PathVariable String doctorId, @PathVariable String hospitalId) {
        try {
            Doctor removedDoctor = doctorService.removeDoctorFromHospital(doctorId, hospitalId);
            if (removedDoctor == null) {
                return ResponseEntity.status(404)
                        .body(Collections.singletonMap("message", "Doctor or Hospital not found"));
            }
            return ResponseEntity.ok(removedDoctor);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Collections.singletonMap("message", "Backend error: " + e.getMessage()));
        }
    }

    // 📋 Get all assigned doctors
    @GetMapping("/assigned-all")
    public ResponseEntity<List<Doctor>> getAllAssigned() {
        try {
            List<Doctor> doctors = doctorService.getAllAssignedDoctors();
            return ResponseEntity.ok(doctors);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

    // 🩺 Get all unique specializations
    @GetMapping("/specializations")
    public ResponseEntity<List<String>> getSpecializations() {
        try {
            List<String> specializations = doctorService.getUniqueSpecializations();
            return ResponseEntity.ok(specializations);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

    // 🏥 Get all hospitals
    @GetMapping("/all-hospitals")
    public ResponseEntity<?> getAllHospitals() {
        try {
            List<Hospital> hospitals = hospitalService.getAllHospitals();
            return ResponseEntity.ok(hospitals);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Collections.singletonMap("message", "Backend error: " + e.getMessage()));
        }
    }
}