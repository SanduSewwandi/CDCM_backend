package com.example.demo.service;

import com.example.demo.dto.DoctorDTO;
import com.example.demo.model.Doctor;
import com.example.demo.model.Hospital;
import com.example.demo.repository.DoctorRepository;
import com.example.demo.repository.HospitalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HospitalDoctorService {

    private static final Logger logger = LoggerFactory.getLogger(HospitalDoctorService.class);

    private final HospitalRepository hospitalRepository;
    private final DoctorRepository doctorRepository;

    public HospitalDoctorService(HospitalRepository hospitalRepository,
                                 DoctorRepository doctorRepository) {
        this.hospitalRepository = hospitalRepository;
        this.doctorRepository = doctorRepository;
    }

    public List<DoctorDTO> getDoctorsByHospital(String hospitalId) {
        logger.info("Fetching doctors for hospital ID: {}", hospitalId);

        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> {
                    logger.error("Hospital not found with id: {}", hospitalId);
                    return new RuntimeException("Hospital not found with id: " + hospitalId);
                });

        logger.info("Hospital found: {}, doctorIds: {}", hospital.getName(), hospital.getDoctorIds());

        // Check if doctorIds is null
        List<String> doctorIds = hospital.getDoctorIds();
        if (doctorIds == null) {
            logger.warn("doctorIds is null for hospital: {}", hospitalId);
            return new ArrayList<>();
        }

        // For MongoDB, we need to fetch doctors one by one if they're stored as references
        List<DoctorDTO> doctors = doctorIds.stream()
                .map(doctorId -> {
                    try {
                        logger.debug("Fetching doctor with ID: {}", doctorId);
                        return doctorRepository.findById(doctorId)
                                .map(doctor -> {
                                    logger.debug("Found doctor: {} {}", doctor.getFirstName(), doctor.getLastName());
                                    return new DoctorDTO(doctor);
                                })
                                .orElseGet(() -> {
                                    logger.warn("Doctor not found with id: {}", doctorId);
                                    return null;
                                });
                    } catch (Exception e) {
                        logger.error("Error fetching doctor {}: {}", doctorId, e.getMessage());
                        return null;
                    }
                })
                .filter(doctor -> doctor != null)
                .filter(DoctorDTO::isVerified) // Only return verified doctors
                .collect(Collectors.toList());

        logger.info("Returning {} doctors for hospital {}", doctors.size(), hospitalId);
        return doctors;
    }

    @Transactional
    public void addDoctorToHospital(String hospitalId, String doctorId) {
        logger.info("Adding doctor {} to hospital {}", doctorId, hospitalId);

        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new RuntimeException("Hospital not found with id: " + hospitalId));

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found with id: " + doctorId));

        // Check if doctor is verified
        if (!doctor.isVerified()) {
            throw new RuntimeException("Doctor is not verified yet");
        }

        // Initialize doctorIds if null
        if (hospital.getDoctorIds() == null) {
            hospital.setDoctorIds(new ArrayList<>());
        }

        // Check if doctor is already associated with this hospital
        if (hospital.getDoctorIds().contains(doctorId)) {
            throw new RuntimeException("Doctor is already associated with this hospital");
        }

        hospital.getDoctorIds().add(doctorId);
        hospitalRepository.save(hospital);
        logger.info("Doctor added successfully");
    }

    @Transactional
    public void removeDoctorFromHospital(String hospitalId, String doctorId) {
        logger.info("Removing doctor {} from hospital {}", doctorId, hospitalId);

        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new RuntimeException("Hospital not found with id: " + hospitalId));

        if (hospital.getDoctorIds() != null) {
            hospital.getDoctorIds().remove(doctorId);
            hospitalRepository.save(hospital);
        }
        logger.info("Doctor removed successfully");
    }

    public List<DoctorDTO> searchAvailableDoctors(String searchTerm, String specialty) {
        logger.info("Searching available doctors - searchTerm: {}, specialty: {}", searchTerm, specialty);

        List<Doctor> doctors;

        try {
            if (searchTerm != null && !searchTerm.isEmpty() && specialty != null && !specialty.isEmpty()) {
                doctors = doctorRepository.findBySearchTermAndSpecialty(searchTerm, specialty);
            } else if (searchTerm != null && !searchTerm.isEmpty()) {
                doctors = doctorRepository.findBySearchTerm(searchTerm);
            } else if (specialty != null && !specialty.isEmpty()) {
                doctors = doctorRepository.findBySpecialization(specialty);
            } else {
                doctors = doctorRepository.findAll();
            }
        } catch (Exception e) {
            logger.error("Error searching doctors: {}", e.getMessage());
            doctors = new ArrayList<>();
        }

        // Filter only verified doctors
        return doctors.stream()
                .filter(Doctor::isVerified)
                .map(DoctorDTO::new)
                .collect(Collectors.toList());
    }
}