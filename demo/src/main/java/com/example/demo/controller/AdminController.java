package com.example.demo.controller;

import com.example.demo.dto.HospitalRegisterRequest;
import com.example.demo.model.Doctor;
import com.example.demo.model.Hospital;
import com.example.demo.model.Patient;
import com.example.demo.repository.DoctorRepository;
import com.example.demo.repository.HospitalRepository;
import com.example.demo.repository.PatientRepository;
import com.example.demo.service.HospitalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final HospitalService hospitalService;
    private final HospitalRepository hospitalRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    public AdminController(HospitalService hospitalService,
                           HospitalRepository hospitalRepository,
                           DoctorRepository doctorRepository,
                           PatientRepository patientRepository) {
        this.hospitalService = hospitalService;
        this.hospitalRepository = hospitalRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    //hospital reg

    @PostMapping("/register-hospital")
    public ResponseEntity<?> registerHospital(
            @RequestBody HospitalRegisterRequest request) {

        Hospital hospital =
                hospitalService.registerHospital(request);

        return ResponseEntity.ok(hospital);
    }


    // DOCTORS CRUD

    @GetMapping("/doctors")
    public ResponseEntity<List<Doctor>> getAllDoctors() {
        return ResponseEntity.ok(doctorRepository.findAll());
    }

    @GetMapping("/doctors/{id}")
    public ResponseEntity<Doctor> getDoctor(@PathVariable String id) {
        Doctor d = doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        return ResponseEntity.ok(d);
    }

    @PutMapping("/doctors/{id}")
    public ResponseEntity<Doctor> updateDoctor(@PathVariable String id, @RequestBody Doctor updated) {
        Doctor existing = doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        existing.setTitle(updated.getTitle());
        existing.setFirstName(updated.getFirstName());
        existing.setLastName(updated.getLastName());
        existing.setPhone(updated.getPhone());
        existing.setSpecialization(updated.getSpecialization());
        existing.setMedicalLicenseNumber(updated.getMedicalLicenseNumber());
        existing.setProfileImage(updated.getProfileImage());
        existing.setVerified(updated.isVerified()); // optional

        return ResponseEntity.ok(doctorRepository.save(existing));
    }

    @DeleteMapping("/doctors/{id}")
    public ResponseEntity<?> deleteDoctor(@PathVariable String id) {
        doctorRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Doctor deleted"));
    }


    // PATIENTS CRUD

    @GetMapping("/patients")
    public ResponseEntity<List<Patient>> getAllPatients() {
        return ResponseEntity.ok(patientRepository.findAll());
    }

    @GetMapping("/patients/{id}")
    public ResponseEntity<Patient> getPatient(@PathVariable String id) {
        Patient p = patientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
        return ResponseEntity.ok(p);
    }

    @PutMapping("/patients/{id}")
    public ResponseEntity<Patient> updatePatient(@PathVariable String id, @RequestBody Patient updated) {
        Patient existing = patientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        existing.setTitle(updated.getTitle());
        existing.setFirstName(updated.getFirstName());
        existing.setLastName(updated.getLastName());
        existing.setContactNumber(updated.getContactNumber());
        existing.setNicOrPassport(updated.getNicOrPassport());
        existing.setResidentialAddress(updated.getResidentialAddress());
        existing.setDateOfBirth(updated.getDateOfBirth());
        existing.setProfileImage(updated.getProfileImage());
        existing.setVerified(updated.isVerified()); // optional

        return ResponseEntity.ok(patientRepository.save(existing));
    }

    @DeleteMapping("/patients/{id}")
    public ResponseEntity<?> deletePatient(@PathVariable String id) {
        patientRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Patient deleted"));
    }


    // HOSPITALS CRUD

    @GetMapping("/hospitals")
    public ResponseEntity<List<Hospital>> getAllHospitals() {
        return ResponseEntity.ok(hospitalRepository.findAll());
    }

    @GetMapping("/hospitals/{id}")
    public ResponseEntity<Hospital> getHospital(@PathVariable String id) {
        Hospital h = hospitalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hospital not found"));
        return ResponseEntity.ok(h);
    }

    @PutMapping("/hospitals/{id}")
    public ResponseEntity<Hospital> updateHospital(@PathVariable String id, @RequestBody Hospital updated) {
        Hospital existing = hospitalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Hospital not found"));

        existing.setName(updated.getName());
        existing.setEmail(updated.getEmail());
        existing.setContactNumber(updated.getContactNumber());
        existing.setAddress(updated.getAddress());
        existing.setLicenseNumber(updated.getLicenseNumber());
        existing.setManagerName(updated.getManagerName());

        return ResponseEntity.ok(hospitalRepository.save(existing));
    }

    @DeleteMapping("/hospitals/{id}")
    public ResponseEntity<?> deleteHospital(@PathVariable String id) {
        hospitalRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Hospital deleted"));
    }
}

