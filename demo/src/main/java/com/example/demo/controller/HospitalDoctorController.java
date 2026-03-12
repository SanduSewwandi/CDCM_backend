package com.example.demo.controller;

import com.example.demo.model.Doctor;
import com.example.demo.model.Hospital;
import com.example.demo.service.DoctorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.demo.service.HospitalService;

import java.util.List;

@RestController
@RequestMapping("/api/hospital/doctors")
@CrossOrigin(origins = "http://localhost:5173")
public class HospitalDoctorController {

    private final DoctorService doctorService;
    private final HospitalService hospitalService;

    public HospitalDoctorController(DoctorService doctorService,HospitalService hospitalService) {
        this.doctorService = doctorService;
        this.hospitalService=hospitalService;
    }

    // 🔎 Search all registered doctors
    @GetMapping("/search")
    public List<Doctor> searchDoctors(
            @RequestParam(required = false) String keyword
    ) {
        return doctorService.searchDoctors(keyword);
    }

    // ➕ Assign doctor to hospital
    @PutMapping("/{doctorId}/assign/{hospitalId}")
    public Doctor assignDoctor(
            @PathVariable String doctorId,
            @PathVariable String hospitalId
    ) {
        return doctorService.assignDoctorToHospital(doctorId, hospitalId);
    }

    // 🏥 Get doctors of hospital
    @GetMapping("/hospital/{hospitalId}")
    public List<Doctor> getHospitalDoctors(
            @PathVariable String hospitalId
    ) {
        return doctorService.getDoctorsByHospital(hospitalId);
    }
    // ⭐ ADD THIS METHOD
    @DeleteMapping("/{doctorId}/remove/{hospitalId}")
    public Doctor removeDoctorFromHospital(
            @PathVariable String doctorId,
            @PathVariable String hospitalId
    ) {
        return doctorService.removeDoctorFromHospital(doctorId, hospitalId);
    }

    // File: sandusewwandi/cdcm_backend/.../controller/HospitalDoctorController.java
    @GetMapping("/assigned-all")
    public ResponseEntity<List<Doctor>> getAllAssigned() {
        List<Doctor> doctors = doctorService.getAllAssignedDoctors();
        return ResponseEntity.ok(doctors); // Ensures a proper JSON response
    }

    @GetMapping("/specializations")
    public ResponseEntity<List<String>> getSpecializations() {
        return ResponseEntity.ok(doctorService.getUniqueSpecializations());
    }

    @GetMapping("/all-hospitals")
    public ResponseEntity<List<Hospital>> getAllHospitals() {
        return ResponseEntity.ok(hospitalService.getAllHospitals());
    }

}