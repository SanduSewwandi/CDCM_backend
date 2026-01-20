package com.example.demo.service;

import com.example.demo.dto.HospitalRegisterRequest;
import com.example.demo.model.Hospital;
import com.example.demo.repository.HospitalRepository;
import org.springframework.stereotype.Service;

@Service
public class HospitalService {

    private final HospitalRepository hospitalRepository;

    public HospitalService(HospitalRepository hospitalRepository) {
        this.hospitalRepository = hospitalRepository;
    }

    public Hospital registerHospital(HospitalRegisterRequest request) {

        if (hospitalRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        Hospital hospital = new Hospital();
        hospital.setName(request.getName());
        hospital.setEmail(request.getEmail());
        hospital.setPassword(request.getPassword()); // plaintext for now
        hospital.setContactNumber(request.getContactNumber());
        hospital.setAddress(request.getAddress());
        hospital.setLicenseNumber(request.getLicenseNumber());
        hospital.setManagerName(request.getManagerName());

        return hospitalRepository.save(hospital);
    }

    public Hospital loginHospital(String email, String password) {

        return hospitalRepository.findByEmail(email)
                .filter(h -> h.getPassword().equals(password))
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
    }
}
