package com.example.demo.controller;

import com.example.demo.dto.AppointmentHistoryDTO;
import com.example.demo.model.Appointment;
import com.example.demo.model.Doctor;
import com.example.demo.model.Patient;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.DoctorRepository;
import com.example.demo.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.demo.dto.PatientWithAppointmentDTO;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/hospital/patients")
@CrossOrigin(origins = "http://localhost:5173")
public class HospitalPatientController {

    private static final Logger logger = LoggerFactory.getLogger(HospitalPatientController.class);

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    // Get patients with appointments for a specific hospital
    @GetMapping("/hospital/{hospitalId}")
    @PreAuthorize("hasAuthority('HOSPITAL')")
    public ResponseEntity<List<PatientWithAppointmentDTO>> getPatientsByHospital(
            @PathVariable String hospitalId) {
        try {
            logger.info("Fetching patients with appointments for hospital ID: {}", hospitalId);

            // Get all doctors in this hospital
            List<Doctor> hospitalDoctors = doctorRepository.findAll().stream()
                    .filter(d -> d.getHospitals() != null && d.getHospitals().contains(hospitalId))
                    .collect(Collectors.toList());

            List<String> doctorIds = hospitalDoctors.stream()
                    .map(Doctor::getId)
                    .collect(Collectors.toList());

            // Get all appointments for these doctors
            List<Appointment> appointments = new ArrayList<>();
            for (String doctorId : doctorIds) {
                List<Appointment> doctorAppointments = appointmentRepository.findAll().stream()
                        .filter(a -> doctorId.equals(a.getDoctorId()))
                        .collect(Collectors.toList());
                appointments.addAll(doctorAppointments);
            }

            // Group appointments by patient
            Map<String, List<Appointment>> patientAppointments = appointments.stream()
                    .collect(Collectors.groupingBy(Appointment::getPatientId));

            // Create response DTOs
            List<PatientWithAppointmentDTO> result = new ArrayList<>();

            for (Map.Entry<String, List<Appointment>> entry : patientAppointments.entrySet()) {
                String patientId = entry.getKey();
                List<Appointment> patientApps = entry.getValue();

                Optional<Patient> patientOpt = patientRepository.findById(patientId);
                if (patientOpt.isPresent()) {
                    Patient patient = patientOpt.get();

                    PatientWithAppointmentDTO dto = new PatientWithAppointmentDTO();
                    dto.setPatientId(generatePatientId(patient.getId()));
                    dto.setId(patient.getId());
                    dto.setPatientName(patient.getTitle() + " " + patient.getFirstName() + " " + patient.getLastName());
                    dto.setEmail(patient.getEmail());
                    dto.setContactNumber(patient.getContactNumber());
                    dto.setGender(extractGender(patient.getNicOrPassport()));
                    dto.setDateOfBirth(patient.getDateOfBirth() != null ? patient.getDateOfBirth().toString() : "");
                    dto.setVerified(patient.isVerified());

                    // Get appointment history
                    List<AppointmentHistoryDTO> appointmentHistory = new ArrayList<>();
                    for (Appointment apt : patientApps) {
                        AppointmentHistoryDTO aptDTO = new AppointmentHistoryDTO();
                        aptDTO.setId(apt.getId());
                        aptDTO.setAppointmentNumber(apt.getAppointmentNumber());
                        aptDTO.setDate(apt.getDate());
                        aptDTO.setTime(apt.getTime());
                        aptDTO.setStatus(apt.getStatus());

                        // Get doctor details
                        doctorRepository.findById(apt.getDoctorId()).ifPresent(doctor -> {
                            aptDTO.setDoctorName(doctor.getTitle() + " " + doctor.getFirstName() + " " + doctor.getLastName());
                        });

                        // You can set hospitalName if needed
                        // aptDTO.setHospitalName(hospitalName);

                        appointmentHistory.add(aptDTO);
                    }
                    dto.setAppointments(appointmentHistory);
                    result.add(dto);
                }
            }

            logger.info("Found {} patients with appointments", result.size());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Error fetching patients: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    // Get patient with their appointments
    @GetMapping("/{id}/appointments")
    @PreAuthorize("hasAuthority('HOSPITAL')")
    public ResponseEntity<PatientWithAppointmentDTO> getPatientWithAppointments(@PathVariable String id) {
        try {
            logger.info("Fetching patient with appointments for ID: {}", id);

            Optional<Patient> patientOpt = patientRepository.findById(id);
            if (patientOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Patient patient = patientOpt.get();

            // Get all appointments for this patient
            List<Appointment> appointments = appointmentRepository.findAll().stream()
                    .filter(a -> id.equals(a.getPatientId()))
                    .collect(Collectors.toList());

            PatientWithAppointmentDTO dto = new PatientWithAppointmentDTO();
            dto.setPatientId(generatePatientId(patient.getId()));
            dto.setPatientName(patient.getTitle() + " " + patient.getFirstName() + " " + patient.getLastName());
            dto.setEmail(patient.getEmail());
            dto.setContactNumber(patient.getContactNumber());
            dto.setGender(extractGender(patient.getNicOrPassport()));
            dto.setDateOfBirth(patient.getDateOfBirth() != null ? patient.getDateOfBirth().toString() : "");
            dto.setVerified(patient.isVerified());

            // Get appointment history
            List<AppointmentHistoryDTO> appointmentHistory = new ArrayList<>();
            for (Appointment apt : appointments) {
                AppointmentHistoryDTO aptDTO = new AppointmentHistoryDTO();
                aptDTO.setId(apt.getId());
                aptDTO.setAppointmentNumber(apt.getAppointmentNumber());
                aptDTO.setDate(apt.getDate());
                aptDTO.setTime(apt.getTime());
                aptDTO.setStatus(apt.getStatus());

                // Get doctor details
                doctorRepository.findById(apt.getDoctorId()).ifPresent(doctor -> {
                    aptDTO.setDoctorName(doctor.getTitle() + " " + doctor.getFirstName() + " " + doctor.getLastName());
                });

                appointmentHistory.add(aptDTO);
            }
            dto.setAppointments(appointmentHistory);

            return ResponseEntity.ok(dto);

        } catch (Exception e) {
            logger.error("Error fetching patient appointments: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    // Get all patients (simplified)
    @GetMapping
    @PreAuthorize("hasAuthority('HOSPITAL')")
    public ResponseEntity<List<Map<String, Object>>> getAllPatients() {
        try {
            logger.info("Fetching all patients for hospital dashboard");
            List<Patient> patients = patientRepository.findAll();

            List<Map<String, Object>> patientDTOs = patients.stream()
                    .map(patient -> {
                        Map<String, Object> dto = new HashMap<>();
                        dto.put("id", patient.getId());
                        dto.put("patientId", generatePatientId(patient.getId()));
                        dto.put("name", patient.getTitle() + " " + patient.getFirstName() + " " + patient.getLastName());
                        dto.put("email", patient.getEmail());
                        dto.put("contactNumber", patient.getContactNumber());
                        dto.put("gender", extractGender(patient.getNicOrPassport()));
                        dto.put("dateOfBirth", patient.getDateOfBirth() != null ? patient.getDateOfBirth().toString() : "");
                        dto.put("verified", patient.isVerified());
                        return dto;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(patientDTOs);
        } catch (Exception e) {
            logger.error("Error fetching patients: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    // Helper method to generate patient ID like P-00001
    private String generatePatientId(String id) {
        try {
            String numeric = id.replaceAll("[^0-9]", "");
            if (numeric.length() >= 5) {
                numeric = numeric.substring(0, 5);
            } else if (numeric.length() > 0) {
                numeric = String.format("%05d", numeric.hashCode() % 100000);
            } else {
                numeric = String.format("%05d", Math.abs(id.hashCode()) % 100000);
            }
            return "P-" + numeric;
        } catch (Exception e) {
            return "P-" + id.substring(Math.max(0, id.length() - 5));
        }
    }

    // Helper method to extract gender from NIC
    private String extractGender(String nicOrPassport) {
        if (nicOrPassport != null && !nicOrPassport.isEmpty()) {
            String lastChar = nicOrPassport.substring(nicOrPassport.length() - 1);
            if (lastChar.matches("[vVxX]")) {
                String digits = nicOrPassport.replaceAll("[^0-9]", "");
                if (digits.length() >= 5) {
                    int lastDigit = Integer.parseInt(digits.substring(digits.length() - 1));
                    return lastDigit % 2 == 0 ? "Female" : "Male";
                }
            } else if (lastChar.matches("\\d")) {
                int digit = Integer.parseInt(lastChar);
                return digit % 2 == 0 ? "Female" : "Male";
            }
        }
        return "Not Specified";
    }
}