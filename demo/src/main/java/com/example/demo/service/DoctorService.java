package com.example.demo.service;

import com.example.demo.dto.DoctorRegisterRequest;
import com.example.demo.dto.DoctorAccountDTO;
import com.example.demo.model.Doctor;
import com.example.demo.repository.DoctorRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public DoctorService(DoctorRepository doctorRepository,
                         PasswordEncoder passwordEncoder,
                         EmailService emailService) {
        this.doctorRepository = doctorRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    // ================= REGISTER =================
    public Doctor registerDoctor(DoctorRegisterRequest request) {

        Optional<Doctor> existingDoctor = doctorRepository.findByEmail(request.getEmail());

        Doctor doctor;

        if (existingDoctor.isPresent()) {
            doctor = existingDoctor.get();
            if (doctor.isVerified()) {
                throw new RuntimeException("Email is already registered and verified.");
            }
        } else {
            doctor = new Doctor();
        }

        doctor.setTitle(request.getTitle());
        doctor.setFirstName(request.getFirstName());
        doctor.setLastName(request.getLastName());
        doctor.setPhone(request.getPhone());
        doctor.setSpecialization(request.getSpecialization());
        doctor.setMedicalLicenseNumber(request.getMedicalLicenseNumber());
        doctor.setEmail(request.getEmail());
        doctor.setPassword(passwordEncoder.encode(request.getPassword()));

        // Generate OTP
        String otp = String.valueOf((int)(Math.random() * 900000) + 100000);
        doctor.setVerified(false);
        doctor.setVerificationCode(otp);
        doctor.setVerificationExpiry(new Date(System.currentTimeMillis() + 5 * 60 * 1000));

        doctorRepository.save(doctor);

        // Send OTP email
        emailService.sendOtp(doctor.getEmail(), otp);

        return doctor;
    }

    // ================= LOGIN =================
    public Doctor loginDoctor(String email, String password) {

        Optional<Doctor> optional = doctorRepository.findByEmail(email);
        if (optional.isPresent()) {
            Doctor doctor = optional.get();
            if (!doctor.isVerified()) {
                throw new RuntimeException("Please verify your email first.");
            }
            if (passwordEncoder.matches(password, doctor.getPassword())) {
                return doctor;
            }
        }
        return null;
    }

    // ================= VERIFY OTP =================
    public String verifyOtp(String email, String otp) {

        Optional<Doctor> optional = doctorRepository.findByEmail(email);
        if (optional.isEmpty()) return "Doctor not found";

        Doctor doctor = optional.get();

        if (doctor.isVerified()) return "Already verified";
        if (doctor.getVerificationCode() == null || !doctor.getVerificationCode().equals(otp))
            return "Invalid OTP";
        if (doctor.getVerificationExpiry() == null || doctor.getVerificationExpiry().before(new Date()))
            return "OTP expired";

        doctor.setVerified(true);
        doctor.setVerificationCode(null);
        doctor.setVerificationExpiry(null);

        doctorRepository.save(doctor);

        return "Email verified successfully";
    }

    // ================= FORGOT PASSWORD =================
    public boolean sendPasswordResetEmail(String email) {

        Optional<Doctor> optional = doctorRepository.findByEmail(email);
        if (optional.isEmpty()) return false;

        Doctor doctor = optional.get();

        String token = UUID.randomUUID().toString();
        doctor.setResetToken(token);
        doctor.setResetTokenExpiry(LocalDateTime.now().plusMinutes(30));

        doctorRepository.save(doctor);

        emailService.sendPasswordResetEmail(doctor.getEmail(), token);

        return true;
    }

    // ================= RESET PASSWORD =================
    public boolean resetPassword(String token, String newPassword) {

        Optional<Doctor> optional = doctorRepository.findByResetToken(token);
        if (optional.isEmpty()) return false;

        Doctor doctor = optional.get();

        if (doctor.getResetTokenExpiry() == null || doctor.getResetTokenExpiry().isBefore(LocalDateTime.now()))
            return false;

        doctor.setPassword(passwordEncoder.encode(newPassword));
        doctor.setResetToken(null);
        doctor.setResetTokenExpiry(null);

        doctorRepository.save(doctor);

        return true;
    }

    // ================= GET PROFILE =================
    public Doctor getDoctorById(String id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
    }

    // ================= UPDATE PROFILE =================
    public Doctor updateDoctor(String id, Doctor updatedData) {

        Doctor existing = getDoctorById(id);

        existing.setTitle(updatedData.getTitle());
        existing.setFirstName(updatedData.getFirstName());
        existing.setLastName(updatedData.getLastName());
        existing.setPhone(updatedData.getPhone());
        existing.setSpecialization(updatedData.getSpecialization());
        existing.setMedicalLicenseNumber(updatedData.getMedicalLicenseNumber());
        existing.setProfileImage(updatedData.getProfileImage());

        return doctorRepository.save(existing);
    }

    // ================= GET ACCOUNT DETAILS =================
    public DoctorAccountDTO getDoctorAccount(String id) {
        Doctor doctor = getDoctorById(id);

        DoctorAccountDTO dto = new DoctorAccountDTO();
        dto.setSpecialization(doctor.getSpecialization());
        dto.setExperience(doctor.getExperience());
        dto.setQualifications(doctor.getQualifications());
        dto.setHospitals(doctor.getHospitals());

        return dto;
    }

    // ================= UPDATE ACCOUNT DETAILS =================
    @Transactional
    public DoctorAccountDTO updateDoctorAccount(String id, DoctorAccountDTO dto) {
        Doctor doctor = getDoctorById(id);

        doctor.setSpecialization(dto.getSpecialization());
        doctor.setExperience(dto.getExperience());

        if (doctor.getQualifications() == null) doctor.setQualifications(new ArrayList<>());
        doctor.getQualifications().clear();
        if (dto.getQualifications() != null) doctor.getQualifications().addAll(dto.getQualifications());

        if (doctor.getHospitals() == null) doctor.setHospitals(new ArrayList<>());
        doctor.getHospitals().clear();
        if (dto.getHospitals() != null) doctor.getHospitals().addAll(dto.getHospitals());

        doctorRepository.save(doctor);

        return getDoctorAccount(id);
    }
}