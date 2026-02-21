package com.example.demo.service;

import com.example.demo.dto.PatientRegisterRequest;
import com.example.demo.model.Patient;
import com.example.demo.repository.PatientRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;

    public PatientService(PatientRepository patientRepository,
                          PasswordEncoder passwordEncoder) {
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Patient registerPatient(PatientRegisterRequest request) {

        Patient patient = new Patient();

        patient.setTitle(request.getTitle());
        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setNicOrPassport(request.getNicOrPassport());
        patient.setContactNumber(request.getContactNumber());
        patient.setResidentialAddress(request.getResidentialAddress());
        patient.setEmail(request.getEmail());

        // 🔐 Encrypt password
        patient.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        return patientRepository.save(patient);
    }

    // Add these methods inside PatientService class

    public Patient getPatientById(String id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found"));
    }

    public Patient updatePatient(String id, Patient updatedData) {
        Patient existing = getPatientById(id);

        // Update fields
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

    // Login
    public Patient loginPatient(String email, String password) {

        Optional<Patient> optional =
                patientRepository.findByEmail(email);

        if (optional.isPresent()) {
            Patient patient = optional.get();

            if (passwordEncoder.matches(password,
                    patient.getPassword())) {
                return patient;
            }
        }

        return null;
    }
}
