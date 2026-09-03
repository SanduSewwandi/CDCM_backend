package com.example.demo.service;

import com.example.demo.dto.HospitalProfileDTO;
import com.example.demo.dto.HospitalRegisterRequest;
import com.example.demo.model.Hospital;
import com.example.demo.repository.HospitalRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Date;

@Service
public class HospitalService {

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private final HospitalRepository hospitalRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public HospitalService(HospitalRepository hospitalRepository,
                           PasswordEncoder passwordEncoder,EmailService emailService) {
        this.hospitalRepository = hospitalRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    private String generateVerificationCode() {

        int number =
                100000 +
                        SECURE_RANDOM.nextInt(900000);

        return String.valueOf(number);
    }

    // =========================
    // =========================
    // REGISTER HOSPITAL (Admin)
    public Hospital registerHospital(
            HospitalRegisterRequest request) {

        String email =
                request.getEmail()
                        .trim()
                        .toLowerCase();

        if (hospitalRepository
                .findByEmail(email)
                .isPresent()) {

            throw new RuntimeException(
                    "Hospital email is already registered"
            );
        }

        Hospital hospital = new Hospital();

        hospital.setName(
                request.getName()
        );

        hospital.setEmail(email);

        hospital.setContactNumber(
                request.getContactNumber()
        );

        hospital.setAddress(
                request.getAddress()
        );

        hospital.setLicenseNumber(
                request.getLicenseNumber()
        );

        hospital.setManagerName(
                request.getManagerName()
        );

        // Encrypt the temporary password
        hospital.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        // Initial onboarding state
        hospital.setVerified(false);

        hospital.setMustChangePassword(true);

        // Do not create OTP during registration
        hospital.setVerificationCode(null);
        hospital.setVerificationExpiry(null);
        hospital.setVerificationLastSentAt(null);

        Hospital savedHospital =
                hospitalRepository.save(hospital);

        // Email contains login information only
        emailService.sendHospitalWelcomeEmail(
                hospital.getEmail(),
                hospital.getName(),
                request.getPassword()
        );

        return savedHospital;
    }


    // =========================
    // LOGIN HOSPITAL
    // =========================
    public Hospital loginHospital(
            String email,
            String password) {

        if (email == null ||
                password == null) {
            return null;
        }

        Optional<Hospital> optional =
                hospitalRepository.findByEmail(
                        email.trim()
                                .toLowerCase()
                );

        if (optional.isEmpty()) {
            return null;
        }

        Hospital hospital = optional.get();

        if (passwordEncoder.matches(
                password,
                hospital.getPassword())) {

            return hospital;
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

    public String verifyOtp(
            String email,
            String otp) {

        Optional<Hospital> optional =
                hospitalRepository.findByEmail(
                        email.trim()
                                .toLowerCase()
                );

        if (optional.isEmpty()) {
            return "Hospital not found";
        }

        Hospital hospital = optional.get();

        if (hospital.isVerified()) {
            return "Email is already verified";
        }

        if (hospital.getVerificationCode()
                == null) {

            return "Please request a verification code first";
        }

        if (!hospital.getVerificationCode()
                .equals(otp)) {

            return "Invalid verification code";
        }

        if (hospital.getVerificationExpiry()
                == null ||
                hospital.getVerificationExpiry()
                        .before(new Date())) {

            return "Verification code expired";
        }

        hospital.setVerified(true);
        hospital.setVerificationCode(null);
        hospital.setVerificationExpiry(null);
        hospital.setVerificationLastSentAt(null);

        hospitalRepository.save(hospital);

        return "Email verified successfully";
    }

    public Hospital changeFirstLoginPassword(
            String email,
            String newPassword) {

        Hospital hospital =
                hospitalRepository
                        .findByEmail(
                                email.trim()
                                        .toLowerCase()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Hospital not found"
                                )
                        );

        if (!hospital.isVerified()) {
            throw new RuntimeException(
                    "Please verify your email first"
            );
        }

        if (!hospital.isMustChangePassword()) {
            throw new RuntimeException(
                    "Temporary password has already been changed"
            );
        }

        hospital.setPassword(
                passwordEncoder.encode(
                        newPassword
                )
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

    public String requestVerificationCode(
            String email) {

        if (email == null || email.isBlank()) {
            return "Authentication required";
        }

        Hospital hospital =
                hospitalRepository
                        .findByEmail(
                                email.trim()
                                        .toLowerCase()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Hospital not found"
                                )
                        );

        if (hospital.isVerified()) {
            return "Email is already verified";
        }

        Date now = new Date();

        Date lastSent =
                hospital
                        .getVerificationLastSentAt();

        // Only allow one request per minute
        if (lastSent != null) {

            long elapsedMilliseconds =
                    now.getTime()
                            - lastSent.getTime();

            long remainingMilliseconds =
                    60_000 -
                            elapsedMilliseconds;

            if (remainingMilliseconds > 0) {

                long remainingSeconds =
                        (remainingMilliseconds + 999)
                                / 1000;

                return "Please wait "
                        + remainingSeconds
                        + " seconds before requesting another code";
            }
        }

        String otp =
                generateVerificationCode();

        hospital.setVerificationCode(otp);

        hospital.setVerificationExpiry(
                new Date(
                        System.currentTimeMillis()
                                + 5 * 60 * 1000
                )
        );

        hospital.setVerificationLastSentAt(now);

        hospitalRepository.save(hospital);

        emailService.sendOtp(
                hospital.getEmail(),
                otp
        );

        return "Verification code sent successfully";
    }
    public List<Hospital> getAllHospitals() {
        return hospitalRepository.findAll();
    }

    public Hospital getHospitalById(String id) {
        return hospitalRepository.findById(id).orElse(null);
    }
}
