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
}
