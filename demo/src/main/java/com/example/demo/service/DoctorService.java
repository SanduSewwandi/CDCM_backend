package com.example.demo.service;


import com.example.demo.dto.DoctorRegisterRequest;
import com.example.demo.model.Doctor;
import com.example.demo.repository.DoctorRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import com.example.demo.dto.DoctorAccountDTO;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Optional;

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

    // =========================
    // REGISTER DOCTOR
    // =========================
    public Doctor registerDoctor(DoctorRegisterRequest request) {
        // 1. Check if the email already exists in the database
        Optional<Doctor> existingDoctor = doctorRepository.findByEmail(request.getEmail());

        Doctor doctor;

        if (existingDoctor.isPresent()) {
            doctor = existingDoctor.get();
            // If already verified, don't let them register again
            if (doctor.isVerified()) {
                throw new RuntimeException("Email is already registered and verified.");
            }
            // If NOT verified, we will just update this existing object with new data
        } else {
            // If it's a new email, create a new object
            doctor = new Doctor();
        }

        // 2. Map fields (Same as your old code)
        doctor.setTitle(request.getTitle());
        doctor.setFirstName(request.getFirstName());
        doctor.setLastName(request.getLastName());
        doctor.setPhone(request.getPhone());
        doctor.setSpecialization(request.getSpecialization());
        doctor.setMedicalLicenseNumber(request.getMedicalLicenseNumber());
        doctor.setEmail(request.getEmail());

        doctor.setPassword(passwordEncoder.encode(request.getPassword()));

        // 3. Generate and set OTP
        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);
        doctor.setVerified(false);
        doctor.setVerificationCode(otp);
        doctor.setVerificationExpiry(new Date(System.currentTimeMillis() + 5 * 60 * 1000));

        // 4. Send Email
        emailService.sendOtp(doctor.getEmail(), otp);

        // 5. Save (This will now "Update" if it existed or "Insert" if it's new)
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

            if (!doctor.isVerified()) {
                throw new RuntimeException("Please verify your email first.");
            }

            if (passwordEncoder.matches(password,
                    doctor.getPassword())) {
                return doctor;
            }
        }

        return null;
    }

    // =========================
    // VERIFY OTP
    // =========================
    public String verifyOtp(String email, String otp) {

        Optional<Doctor> optional =
                doctorRepository.findByEmail(email);

        if (optional.isEmpty()) {
            return "Doctor not found";
        }

        Doctor doctor = optional.get();

        if (doctor.isVerified()) {
            return "Already verified";
        }

        if (doctor.getVerificationCode() == null ||
                !doctor.getVerificationCode().equals(otp)) {

            return "Invalid OTP";
        }

        if (doctor.getVerificationExpiry() == null ||
                doctor.getVerificationExpiry().before(new Date())) {

            return "OTP expired";
        }

        doctor.setVerified(true);
        doctor.setVerificationCode(null);
        doctor.setVerificationExpiry(null);

        doctorRepository.save(doctor);

        return "Email verified successfully";
    }

    // Add these methods inside PatientService class

    public Doctor getDoctorById(String id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
    }

    public Doctor updateDoctor(String id, Doctor updatedData) {
        Doctor existing = getDoctorById(id);

        // Update fields
        existing.setTitle(updatedData.getTitle());
        existing.setFirstName(updatedData.getFirstName());
        existing.setLastName(updatedData.getLastName());
        existing.setPhone(updatedData.getPhone());
        existing.setSpecialization(updatedData.getSpecialization());
        existing.setMedicalLicenseNumber(updatedData.getMedicalLicenseNumber());
        existing.setProfileImage(updatedData.getProfileImage());

        return doctorRepository.save(existing);
    }

    // =========================
// GET DOCTOR ACCOUNT DETAILS
// =========================
    public DoctorAccountDTO getDoctorAccount(String id) {
        Doctor doctor = getDoctorById(id);

        DoctorAccountDTO dto = new DoctorAccountDTO();
        dto.setSpecialization(doctor.getSpecialization());
        dto.setExperience(doctor.getExperience());
        dto.setQualifications(doctor.getQualifications());
        dto.setHospitals(doctor.getHospitals());

        return dto;
    }

    // =========================
// UPDATE DOCTOR ACCOUNT DETAILS
// =========================
    @Transactional
    public DoctorAccountDTO updateDoctorAccount(String id, DoctorAccountDTO dto) {
        Doctor doctor = getDoctorById(id);

        // update specialization (specialty)
        doctor.setSpecialization(dto.getSpecialization());

        // update experience
        doctor.setExperience(dto.getExperience());

        // update qualifications list safely
        if (doctor.getQualifications() == null) doctor.setQualifications(new ArrayList<>());
        doctor.getQualifications().clear();
        if (dto.getQualifications() != null) doctor.getQualifications().addAll(dto.getQualifications());

        // update hospitals list safely
        if (doctor.getHospitals() == null) doctor.setHospitals(new ArrayList<>());
        doctor.getHospitals().clear();
        if (dto.getHospitals() != null) doctor.getHospitals().addAll(dto.getHospitals());

        doctorRepository.save(doctor);

        return getDoctorAccount(id);
    }

}

