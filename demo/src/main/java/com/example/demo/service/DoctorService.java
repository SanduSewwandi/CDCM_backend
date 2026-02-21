package com.example.demo.service;

import com.example.demo.dto.DoctorRegisterRequest;
import com.example.demo.model.Doctor;
import com.example.demo.repository.DoctorRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;

    public DoctorService(DoctorRepository doctorRepository,
                         PasswordEncoder passwordEncoder) {
        this.doctorRepository = doctorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // =========================
    // REGISTER DOCTOR
    // =========================
    public Doctor registerDoctor(DoctorRegisterRequest request) {

        Doctor doctor = new Doctor();

        doctor.setTitle(request.getTitle());
        doctor.setFirstName(request.getFirstName());
        doctor.setLastName(request.getLastName());
        doctor.setPhone(request.getPhone());
        doctor.setSpecialization(request.getSpecialization());
        doctor.setMedicalLicenseNumber(request.getMedicalLicenseNumber());
        doctor.setEmail(request.getEmail());

        // 🔐 Encrypt password
        doctor.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        return doctorRepository.save(doctor);
    }

    // =========================
    // LOGIN DOCTOR
    // =========================
    public Doctor loginDoctor(String email, String password) {

        Optional<Doctor> optional =
                doctorRepository.findByEmail(email);

        if (optional.isPresent()) {
            Doctor doctor = optional.get();

            if (passwordEncoder.matches(password,
                    doctor.getPassword())) {
                return doctor;
            }
        }

        return null;
    }

    // Add these methods inside PatientService class

    public Doctor getDoctorById(String id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
    }

    public Doctor updateDoctor(String id, Doctor updatedData) {
        Doctor existing = getDoctorById(id);

        // Update fields
        existing.setFirstName(updatedData.getFirstName());
        existing.setLastName(updatedData.getLastName());
        // ... update other fields as needed ...

        // Update the Profile Image
        existing.setProfileImage(updatedData.getProfileImage());

        return doctorRepository.save(existing);
    }
}
