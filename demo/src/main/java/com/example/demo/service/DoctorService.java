package com.example.demo.service;

import com.example.demo.dto.DoctorRegisterRequest;
import com.example.demo.model.Doctor;
import com.example.demo.repository.DoctorRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.example.demo.dto.DoctorAccountDTO;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Optional;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;

    public DoctorService(DoctorRepository doctorRepository,
                         PasswordEncoder passwordEncoder) {
        this.doctorRepository = doctorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // =========================
    // REGISTER DOCTOR
    // =========================
    public Doctor registerDoctor(DoctorRegisterRequest request) {

        Doctor doctor = new Doctor();

        doctor.setTitle(request.getTitle());
        doctor.setFirstName(request.getFirstName());
        doctor.setLastName(request.getLastName());
        doctor.setPhone(request.getPhone());
        doctor.setSpecialization(request.getSpecialization());
        doctor.setMedicalLicenseNumber(request.getMedicalLicenseNumber());
        doctor.setEmail(request.getEmail());

        // 🔐 Encrypt password
        doctor.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

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

            if (passwordEncoder.matches(password,
                    doctor.getPassword())) {
                return doctor;
            }
        }

        return null;
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
