package com.example.demo.controller;

import com.example.demo.dto.DoctorSearchResponse;
import com.example.demo.dto.PaginatedResponse;
import com.example.demo.model.Doctor;
import com.example.demo.service.DoctorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/doctors")
@CrossOrigin(origins = "http://localhost:3000")
public class DoctorController {

    @Autowired
    private DoctorService doctorService;

    @GetMapping("/search")
    public ResponseEntity<PaginatedResponse<DoctorSearchResponse>> searchDoctors(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String specialty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size) {

        // For MongoDB, we'll implement pagination manually
        List<Doctor> doctors;

        if (search != null && !search.isEmpty() && specialty != null && !specialty.isEmpty()) {
            doctors = doctorService.searchByTermAndSpecialty(search, specialty);
        } else if (search != null && !search.isEmpty()) {
            doctors = doctorService.searchByTerm(search);
        } else if (specialty != null && !specialty.isEmpty()) {
            doctors = doctorService.findBySpecialization(specialty);
        } else {
            doctors = doctorService.getAllDoctors();
        }

        // Filter only verified doctors
        doctors = doctors.stream()
                .filter(Doctor::isVerified)
                .collect(Collectors.toList());

        // Manual pagination
        int start = page * size;
        int end = Math.min(start + size, doctors.size());

        List<DoctorSearchResponse> paginatedContent = doctors.subList(start, end)
                .stream()
                .map(DoctorSearchResponse::new)
                .collect(Collectors.toList());

        PaginatedResponse<DoctorSearchResponse> response = new PaginatedResponse<>();
        response.setContent(paginatedContent);
        response.setPage(page);
        response.setSize(size);
        response.setTotalElements(doctors.size());
        response.setTotalPages((int) Math.ceil((double) doctors.size() / size));
        response.setLast(end >= doctors.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DoctorSearchResponse> getDoctorById(@PathVariable String id) {
        Doctor doctor = doctorService.getDoctorById(id);
        return ResponseEntity.ok(new DoctorSearchResponse(doctor));
    }

    @GetMapping("/all")
    public ResponseEntity<List<DoctorSearchResponse>> getAllDoctors() {
        List<DoctorSearchResponse> doctors = doctorService.getAllDoctors()
                .stream()
                .filter(Doctor::isVerified)
                .map(DoctorSearchResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(doctors);
    }
}