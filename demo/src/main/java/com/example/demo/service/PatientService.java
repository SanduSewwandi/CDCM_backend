package com.example.demo.service;

import com.example.demo.dto.PatientRegisterRequest;
import com.example.demo.model.Patient;
import com.example.demo.repository.PatientRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public PatientService(PatientRepository patientRepository,
                          PasswordEncoder passwordEncoder,
                          EmailService emailService) {
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    // ================= REGISTER =================
    public Patient registerPatient(PatientRegisterRequest request) {

        Optional<Patient> existingPatient = patientRepository.findByEmail(request.getEmail());

        Patient patient;

        if (existingPatient.isPresent()) {
            patient = existingPatient.get();
            if (patient.isVerified()) {
                throw new RuntimeException("Email is already registered and verified.");
            }
        } else {
            patient = new Patient();
        }

        patient.setTitle(request.getTitle());
        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setNicOrPassport(request.getNicOrPassport());
        patient.setContactNumber(request.getContactNumber());
        patient.setResidentialAddress(request.getResidentialAddress());
        patient.setEmail(request.getEmail());
        patient.setPassword(passwordEncoder.encode(request.getPassword()));

        // Generate OTP
        String otp = String.valueOf((int)(Math.random() * 900000) + 100000);
        patient.setVerified(false);
        patient.setVerificationCode(otp);
        patient.setVerificationExpiry(new Date(System.currentTimeMillis() + 5 * 60 * 1000));

        patientRepository.save(patient);

        // Send OTP email
        emailService.sendOtp(patient.getEmail(), otp);

        return patient;
    }

    // ================= LOGIN =================
    public Patient loginPatient(String email, String password) {

        Optional<Patient> optional = patientRepository.findByEmail(email);
        if (optional.isPresent()) {
            Patient patient = optional.get();
            if (!patient.isVerified()) {
                throw new RuntimeException("Please verify your email first.");
            }
            if (passwordEncoder.matches(password, patient.getPassword())) {
                return patient;
            }
        }
        return null;
    }

    // ================= VERIFY OTP =================
    public String verifyOtp(String email, String otp) {

        Optional<Patient> optional = patientRepository.findByEmail(email);
        if (optional.isEmpty()) return "Patient not found";

        Patient patient = optional.get();

        if (patient.isVerified()) return "Already verified";
        if (patient.getVerificationCode() == null || !patient.getVerificationCode().equals(otp))
            return "Invalid OTP";
        if (patient.getVerificationExpiry() == null || patient.getVerificationExpiry().before(new Date()))
            return "OTP expired";

        patient.setVerified(true);
        patient.setVerificationCode(null);
        patient.setVerificationExpiry(null);

        patientRepository.save(patient);

        return "Email verified successfully";
    }

    // ================= FORGOT PASSWORD =================
    public boolean sendPasswordResetEmail(String email) {

        Optional<Patient> optional = patientRepository.findByEmail(email);
        if (optional.isEmpty()) return false;

        Patient patient = optional.get();

        // Generate secure reset token
        String token = UUID.randomUUID().toString();
        patient.setResetToken(token);
        patient.setResetTokenExpiry(LocalDateTime.now().plusMinutes(30)); // use LocalDateTime

        patientRepository.save(patient);

        // Send reset email
        emailService.sendPasswordResetEmail(patient.getEmail(), token);

        return true;
    }

    // ================= RESET PASSWORD =================
    public boolean resetPassword(String token, String newPassword) {

        Optional<Patient> optional = patientRepository.findByResetToken(token);
        if (optional.isEmpty()) return false;

        Patient patient = optional.get();

        if (patient.getResetTokenExpiry() == null ||
                patient.getResetTokenExpiry().isBefore(LocalDateTime.now()))
            return false;

        patient.setPassword(passwordEncoder.encode(newPassword));
        patient.setResetToken(null);
        patient.setResetTokenExpiry(null);

        patientRepository.save(patient);

        return true;
    }

    // ================= GET PROFILE =================
    public Patient getPatientById(String id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
    }

    // ================= UPDATE PROFILE =================
    public Patient updatePatient(String id, Patient updatedData) {

        Patient existing = getPatientById(id);

        existing.setTitle(updatedData.getTitle());
        existing.setFirstName(updatedData.getFirstName());
        existing.setLastName(updatedData.getLastName());
        existing.setDateOfBirth(updatedData.getDateOfBirth());
        existing.setNicOrPassport(updatedData.getNicOrPassport());
        existing.setContactNumber(updatedData.getContactNumber());
        existing.setResidentialAddress(updatedData.getResidentialAddress());
        existing.setProfileImage(updatedData.getProfileImage());

        return patientRepository.save(existing);
    }
}