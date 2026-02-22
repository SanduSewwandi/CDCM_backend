package com.example.demo.service;

import com.example.demo.dto.PatientRegisterRequest;
import com.example.demo.model.Patient;
import com.example.demo.repository.PatientRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

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
        // 1. Check for existing email to prevent duplicates
        Optional<Patient> existingPatient = patientRepository.findByEmail(request.getEmail());

        Patient patient;

        if (existingPatient.isPresent()) {
            patient = existingPatient.get();
            // If they are already verified, prevent a new registration
            if (patient.isVerified()) {
                throw new RuntimeException("Email is already registered and verified.");
            }
            // If not verified, the 'patient' variable now points to the existing record
        } else {
            // Only create a 'new' object if the email doesn't exist at all
            patient = new Patient();
        }

        // 2. Map fields (this will either populate the new object or update the existing one)
        patient.setTitle(request.getTitle());
        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setNicOrPassport(request.getNicOrPassport());
        patient.setContactNumber(request.getContactNumber());
        patient.setResidentialAddress(request.getResidentialAddress());
        patient.setEmail(request.getEmail());

        patient.setPassword(passwordEncoder.encode(request.getPassword()));

        // 3. Refresh OTP and Expiry
        String otp = String.valueOf((int)(Math.random() * 900000) + 100000);
        patient.setVerified(false);
        patient.setVerificationCode(otp);
        patient.setVerificationExpiry(new Date(System.currentTimeMillis() + 5 * 60 * 1000));

        // 4. Send Email
        emailService.sendOtp(patient.getEmail(), otp);

        // 5. Save (Update or Insert)
        return patientRepository.save(patient);
    }

    // ================= LOGIN =================
    public Patient loginPatient(String email, String password) {

        Optional<Patient> optional =
                patientRepository.findByEmail(email);

        if (optional.isPresent()) {
            Patient patient = optional.get();

            if (!patient.isVerified()) {
                throw new RuntimeException("Please verify your email first.");
            }

            if (passwordEncoder.matches(password,
                    patient.getPassword())) {
                return patient;
            }
        }

        return null;
    }

    // ================= VERIFY OTP =================
    public String verifyOtp(String email, String otp) {

        Optional<Patient> optional =
                patientRepository.findByEmail(email);

        if (optional.isEmpty()) {
            return "Patient not found";
        }

        Patient patient = optional.get();

        if (patient.isVerified()) {
            return "Already verified";
        }

        if (patient.getVerificationCode() == null ||
                !patient.getVerificationCode().equals(otp)) {

            return "Invalid OTP";
        }

        if (patient.getVerificationExpiry() == null ||
                patient.getVerificationExpiry().before(new Date())) {

            return "OTP expired";
        }

        patient.setVerified(true);
        patient.setVerificationCode(null);
        patient.setVerificationExpiry(null);

        patientRepository.save(patient);

        return "Email verified successfully";
    }
}