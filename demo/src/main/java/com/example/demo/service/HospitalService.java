package com.example.demo.service;

import com.example.demo.dto.HospitalRegisterRequest;
import com.example.demo.model.Hospital;
import com.example.demo.model.Patient;
import com.example.demo.repository.HospitalRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.example.demo.service.EmailService;
import java.util.Date;
import com.example.demo.dto.HospitalProfileDTO;

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

        if (hospitalRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Hospital email is already registered");
        }

        Hospital hospital = new Hospital();

        hospital.setName(request.getName());
        hospital.setEmail(request.getEmail().trim().toLowerCase());
        hospital.setContactNumber(request.getContactNumber());
        hospital.setAddress(request.getAddress());
        hospital.setLicenseNumber(request.getLicenseNumber());
        hospital.setManagerName(request.getManagerName());

        hospital.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        String otp = String.valueOf(
                (int) (Math.random() * 900000) + 100000
        );

        hospital.setVerified(false);
        hospital.setVerificationCode(otp);
        hospital.setVerificationExpiry(
                new Date(System.currentTimeMillis() + 5 * 60 * 1000)
        );
        hospital.setMustChangePassword(true);

        Hospital savedHospital = hospitalRepository.save(hospital);

        emailService.sendHospitalWelcomeEmail(
                hospital.getEmail(),
                hospital.getName(),
                request.getPassword(),
                otp
        );

        return savedHospital;
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

    public String verifyOtp(String email, String otp) {

        Optional<Hospital> optional =
                hospitalRepository.findByEmail(email.trim().toLowerCase());

        if (optional.isEmpty()) {
            return "Hospital not found";
        }

        Hospital hospital = optional.get();

        if (hospital.isVerified()) {
            return "Already verified";
        }

        if (hospital.getVerificationCode() == null ||
                !hospital.getVerificationCode().equals(otp)) {
            return "Invalid OTP";
        }

        if (hospital.getVerificationExpiry() == null ||
                hospital.getVerificationExpiry().before(new Date())) {
            return "OTP expired";
        }

        hospital.setVerified(true);
        hospital.setVerificationCode(null);
        hospital.setVerificationExpiry(null);

        hospitalRepository.save(hospital);

        return "Email verified successfully";
    }

    public Hospital changeFirstLoginPassword(
            String email,
            String newPassword) {

        Hospital hospital = hospitalRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Hospital not found")
                );

        if (!hospital.isVerified()) {
            throw new RuntimeException(
                    "Please verify the hospital email first"
            );
        }

        if (!hospital.isMustChangePassword()) {
            throw new RuntimeException(
                    "Temporary password has already been changed"
            );
        }

        hospital.setPassword(
                passwordEncoder.encode(newPassword)
        );

        hospital.setMustChangePassword(false);

        return hospitalRepository.save(hospital);
    }

    private HospitalProfileDTO convertToProfileDTO(Hospital hospital) {

        return new HospitalProfileDTO(
                hospital.getId(),
                hospital.getName(),
                hospital.getEmail(),
                hospital.getContactNumber(),
                hospital.getAddress(),
                hospital.getLicenseNumber(),
                hospital.getManagerName(),
                hospital.getLocation(),
                hospital.getProfileImage(),
                hospital.isVerified(),
                hospital.isMustChangePassword()
        );
    }

    public HospitalProfileDTO getMyProfile(String email) {

        Hospital hospital = hospitalRepository
                .findByEmail(email.trim().toLowerCase())
                .orElseThrow(() ->
                        new RuntimeException("Hospital not found")
                );

        return convertToProfileDTO(hospital);
    }

    public HospitalProfileDTO updateMyProfile(
            String email,
            HospitalProfileDTO request) {

        Hospital hospital = hospitalRepository
                .findByEmail(email.trim().toLowerCase())
                .orElseThrow(() ->
                        new RuntimeException("Hospital not found")
                );

        if (!hospital.isVerified()) {
            throw new RuntimeException(
                    "Please verify your email first"
            );
        }

        if (hospital.isMustChangePassword()) {
            throw new RuntimeException(
                    "Please change your temporary password first"
            );
        }

        hospital.setName(request.getName());
        hospital.setContactNumber(request.getContactNumber());
        hospital.setAddress(request.getAddress());
        hospital.setManagerName(request.getManagerName());
        hospital.setLocation(request.getLocation());
        hospital.setProfileImage(request.getProfileImage());

        Hospital updatedHospital =
                hospitalRepository.save(hospital);

        return convertToProfileDTO(updatedHospital);
    }

    public List<Hospital> getAllHospitals() {
        return hospitalRepository.findAll();
    }

    public Hospital getHospitalById(String id) {
        return hospitalRepository.findById(id).orElse(null);
    }
}
