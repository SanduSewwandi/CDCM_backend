package com.example.demo.service;

import com.example.demo.model.Patient;
import com.example.demo.model.Doctor;
import com.example.demo.model.Hospital;
import com.example.demo.repository.PatientRepository;
import com.example.demo.repository.DoctorRepository;
import com.example.demo.repository.HospitalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    @Autowired
    private PatientRepository patientRepo;

    @Autowired
    private DoctorRepository doctorRepo;

    @Autowired
    private HospitalRepository hospitalRepo;

    @Autowired
    private EmailService emailService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // =========================================================
    // GENERATE RESET TOKEN + SEND EMAIL
    // =========================================================
    public String generateResetToken(String email) {

        String token = UUID.randomUUID().toString();

        // ---------- Patient ----------
        Optional<Patient> optPatient = patientRepo.findByEmail(email);
        if (optPatient.isPresent()) {
            Patient p = optPatient.get();
            p.setResetToken(token);
            p.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
            patientRepo.save(p);

            emailService.sendPasswordResetEmail(email, token);
            return token;
        }

        // ---------- Doctor ----------
        Optional<Doctor> optDoctor = doctorRepo.findByEmail(email);
        if (optDoctor.isPresent()) {
            Doctor d = optDoctor.get();
            d.setResetToken(token);
            d.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
            doctorRepo.save(d);

            emailService.sendPasswordResetEmail(email, token);
            return token;
        }

        // ---------- Hospital ----------
        Optional<Hospital> optHospital = hospitalRepo.findByEmail(email);
        if (optHospital.isPresent()) {
            Hospital h = optHospital.get();
            h.setResetToken(token);
            h.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
            hospitalRepo.save(h);

            emailService.sendPasswordResetEmail(email, token);
            return token;
        }

        return null;
    }

    // =========================================================
    // RESET PASSWORD (NO EMAIL HERE)
    // =========================================================
    public boolean resetPassword(String token, String newPassword) {

        LocalDateTime now = LocalDateTime.now();

        // ---------- Patient ----------
        Optional<Patient> optPatient = patientRepo.findByResetToken(token);
        if (optPatient.isPresent()) {
            Patient p = optPatient.get();

            if (p.getResetTokenExpiry().isAfter(now)) {
                p.setPassword(passwordEncoder.encode(newPassword));
                p.setResetToken(null);
                p.setResetTokenExpiry(null);
                patientRepo.save(p);
                return true;
            }
            return false;
        }

        // ---------- Doctor ----------
        Optional<Doctor> optDoctor = doctorRepo.findByResetToken(token);
        if (optDoctor.isPresent()) {
            Doctor d = optDoctor.get();

            if (d.getResetTokenExpiry().isAfter(now)) {
                d.setPassword(passwordEncoder.encode(newPassword));
                d.setResetToken(null);
                d.setResetTokenExpiry(null);
                doctorRepo.save(d);
                return true;
            }
            return false;
        }

        // ---------- Hospital ----------
        Optional<Hospital> optHospital = hospitalRepo.findByResetToken(token);
        if (optHospital.isPresent()) {
            Hospital h = optHospital.get();

            if (h.getResetTokenExpiry().isAfter(now)) {
                h.setPassword(passwordEncoder.encode(newPassword));
                h.setResetToken(null);
                h.setResetTokenExpiry(null);
                hospitalRepo.save(h);
                return true;
            }
            return false;
        }

        return false;
    }
}