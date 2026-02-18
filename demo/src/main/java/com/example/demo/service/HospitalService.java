package com.example.demo.service;

import com.example.demo.dto.HospitalRegisterRequest;
import com.example.demo.model.Hospital;
import com.example.demo.repository.HospitalRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class HospitalService {

    private final HospitalRepository hospitalRepository;
    private final PasswordEncoder passwordEncoder;

    public HospitalService(HospitalRepository hospitalRepository,
                           PasswordEncoder passwordEncoder) {
        this.hospitalRepository = hospitalRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // =========================
    // REGISTER HOSPITAL (Admin)
    // =========================
    public Hospital registerHospital(HospitalRegisterRequest request) {

        Hospital hospital = new Hospital();

        hospital.setName(request.getName());
        hospital.setEmail(request.getEmail());
        hospital.setContactNumber(request.getContactNumber());
        hospital.setAddress(request.getAddress());
        hospital.setLicenseNumber(request.getLicenseNumber());
        hospital.setManagerName(request.getManagerName());

        // 🔐 Encrypt password
        hospital.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        return hospitalRepository.save(hospital);
    }

    // =========================
    // LOGIN HOSPITAL
    // =========================
    public Hospital loginHospital(String email, String password) {

        Optional<Hospital> optional =
                hospitalRepository.findByEmail(email);

        if (optional.isPresent()) {
            Hospital hospital = optional.get();

            if (passwordEncoder.matches(password,
                    hospital.getPassword())) {
                return hospital;
            }
        }

        return null;
    }
}
