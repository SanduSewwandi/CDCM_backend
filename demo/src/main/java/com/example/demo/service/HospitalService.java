package com.example.demo.service;

import com.example.demo.dto.HospitalRegisterRequest;
import com.example.demo.model.Hospital;
import com.example.demo.model.Patient;
import com.example.demo.repository.HospitalRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import com.example.demo.service.EmailService;

@Service
public class HospitalService {

    private final HospitalRepository hospitalRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public HospitalService(HospitalRepository hospitalRepository,
                           PasswordEncoder passwordEncoder,EmailService emailService) {
        this.hospitalRepository = hospitalRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
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

    // ================= FORGOT PASSWORD =================
    public boolean sendPasswordResetEmail(String email) {

        Optional<Hospital> optional = hospitalRepository.findByEmail(email);
        if (optional.isEmpty()) return false;

        Hospital hospital = optional.get();

        String token = UUID.randomUUID().toString();
        hospital.setResetToken(token);
        hospital.setResetTokenExpiry(LocalDateTime.now().plusMinutes(30));

        hospitalRepository.save(hospital);

        emailService.sendPasswordResetEmail(hospital.getEmail(), token);

        return true;
    }


    // ================= RESET PASSWORD =================
    public boolean resetPassword(String token, String newPassword) {

        Optional<Hospital> optional = hospitalRepository.findByResetToken(token);
        if (optional.isEmpty()) return false;

        Hospital hospital = optional.get();

        if (hospital.getResetTokenExpiry() == null ||
                hospital.getResetTokenExpiry().isBefore(LocalDateTime.now()))
            return false;

        hospital.setPassword(passwordEncoder.encode(newPassword));
        hospital.setResetToken(null);
        hospital.setResetTokenExpiry(null);

        hospitalRepository.save(hospital);

        return true;
    }
}
