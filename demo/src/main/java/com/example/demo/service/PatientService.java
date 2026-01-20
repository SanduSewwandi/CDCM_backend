package com.example.demo.service;

<<<<<<< HEAD
import com.example.demo.dto.PatientRegisterRequest;
import com.example.demo.model.Patient;
import com.example.demo.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    // Signup
    public Patient registerPatient(PatientRegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        Optional<Patient> existing = patientRepository.findByEmail(request.getEmail());
        if (existing.isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        Patient patient = new Patient();
        patient.setTitle(request.getTitle());
        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setNicOrPassport(request.getNicOrPassport());
        patient.setContactNumber(request.getContactNumber());
        patient.setResidentialAddress(request.getResidentialAddress());
        patient.setEmail(request.getEmail());
        patient.setPassword(request.getPassword()); // Later encrypt

        return patientRepository.save(patient);
    }

    // Login
    public Patient loginPatient(String email, String password) {
        Patient patient = patientRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!patient.getPassword().equals(password)) {
            throw new RuntimeException("Invalid email or password");
        }

        return patient;
    }
=======
public class PatientService {
>>>>>>> 89e3041278f620dff59a1d43465db79ea34a6c27
}
