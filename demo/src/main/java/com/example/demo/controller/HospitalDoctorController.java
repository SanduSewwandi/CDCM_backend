package com.example.demo.controller;

import com.example.demo.model.Doctor;
import com.example.demo.service.DoctorService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hospital/doctors")
@CrossOrigin(origins = "http://localhost:5173")
public class HospitalDoctorController {

    private final DoctorService doctorService;

    public HospitalDoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
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
}