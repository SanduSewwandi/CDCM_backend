package com.example.demo.service;

import com.example.demo.dto.AppointmentResponseDTO;
import com.example.demo.model.Appointment;
import com.example.demo.model.Hospital;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.HospitalRepository;
import com.example.demo.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private HospitalRepository hospitalRepository; // ✅ Added to fetch hospital names

    public Appointment bookAppointment(Appointment appointment) {
        int chosenNumber = Integer.parseInt(appointment.getAppointmentNumber());
        String formattedApptNumber = String.format("APT-%03d", chosenNumber);

        appointment.setAppointmentNumber(formattedApptNumber);
        appointment.setStatus("CONFIRMED");

        return appointmentRepository.save(appointment);
    }

    public List<Appointment> getAppointmentsForPatient(String patientId) {
        return appointmentRepository.findByPatientId(patientId);
    }

    public List<Appointment> getAppointmentsBySchedule(String scheduleId) {
        return appointmentRepository.findByScheduleId(scheduleId);
    }

    //getAppointmentsByDoctor
    public List<AppointmentResponseDTO> getAppointmentsByHospital(String hospitalId) {
        List<Appointment> appointments = appointmentRepository.findByHospitalId(hospitalId);

        return appointments.stream().map(appt -> {
                    AppointmentResponseDTO dto = new AppointmentResponseDTO();
                    dto.setId(appt.getId());
                    dto.setPatientId(appt.getPatientId());
                    dto.setAppointmentNumber(appt.getAppointmentNumber());
                    dto.setDate(appt.getDate());
                    dto.setTime(appt.getTime());
                    dto.setStatus(appt.getStatus());
                    dto.setPaymentStatus(appt.getPaymentStatus());
                    dto.setDoctorId(appt.getDoctorId());
                    dto.setPaid(appt.isPaid());

                    // Fetch hospital name
                    if (appt.getHospitalId() != null) {
                        hospitalRepository.findById(appt.getHospitalId()).ifPresent(h -> {
                            dto.setHospitalName(h.getName());
                        });
                    }

                    // Fetch patient name from Patient collection
                    patientRepository.findById(appt.getPatientId()).ifPresent(p -> {
                        dto.setPatientName(p.getFirstName() + " " + p.getLastName());
                        dto.setProfileImage(p.getProfileImage());
                    });

                    return dto;
                })
                .sorted(Comparator.comparing(AppointmentResponseDTO::getDate).reversed()
                        .thenComparing(AppointmentResponseDTO::getAppointmentNumber))
                .collect(Collectors.toList());
    }

    /**
     * Fetches appointments for a specific doctor, enriches them with patient
     * details and hospital names, and sorts them.
     */
    public List<AppointmentResponseDTO> getAppointmentsByDoctor(String doctorId) {
        List<Appointment> appointments = appointmentRepository.findByDoctorId(doctorId);

        return appointments.stream().map(appt -> {
                    AppointmentResponseDTO dto = new AppointmentResponseDTO();
                    dto.setId(appt.getId());
                    dto.setPatientId(appt.getPatientId());
                    dto.setAppointmentNumber(appt.getAppointmentNumber());
                    dto.setDate(appt.getDate());
                    dto.setTime(appt.getTime());
                    dto.setStatus(appt.getStatus());

                    dto.setPaymentStatus(appt.getPaymentStatus());
                    dto.setDoctorId(appt.getDoctorId());
                    dto.setPaid(appt.isPaid());


                    // ✅ FIX: Fetch hospital name using the hospitalId from the appointment
                    if (appt.getHospitalId() != null) {
                        hospitalRepository.findById(appt.getHospitalId()).ifPresent(h -> {
                            dto.setHospitalName(h.getName());
                        });
                    } else {
                        dto.setHospitalName("General Clinic"); // Fallback
                    }

                    // Fetch patient details from the Patient collection
                    patientRepository.findById(appt.getPatientId()).ifPresent(p -> {
                        dto.setPatientName(p.getFirstName() + " " + p.getLastName());
                        dto.setProfileImage(p.getProfileImage());
                    });

                    return dto;
                })
                // Sort by Date first (Newest first), then by Appointment Number
                .sorted(Comparator.comparing(AppointmentResponseDTO::getDate).reversed()
                        .thenComparing(AppointmentResponseDTO::getAppointmentNumber))
                .collect(Collectors.toList());
    }
}