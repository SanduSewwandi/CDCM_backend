package com.example.demo.service;

import com.example.demo.model.Doctor;
import com.example.demo.repository.DoctorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DoctorService {

    @Autowired
    private DoctorRepository doctorRepository;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // Sign-up
    public Doctor registerDoctor(Doctor doctor) {
        doctor.setPassword(passwordEncoder.encode(doctor.getPassword()));
        return doctorRepository.save(doctor);
    }

    // Login
    public Doctor loginDoctor(String email, String password) {
        Optional<Doctor> optionalDoctor = doctorRepository.findByEmail(email);
        if(optionalDoctor.isPresent()) {
            Doctor doctor = optionalDoctor.get();
            if(passwordEncoder.matches(password, doctor.getPassword())) {
                return doctor;
            }
        }
        return null; // invalid credentials
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
