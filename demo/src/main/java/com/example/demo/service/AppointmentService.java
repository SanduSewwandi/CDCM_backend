package com.example.demo.service;

import com.example.demo.dto.AppointmentResponseDTO;
import com.example.demo.model.Appointment;
import com.example.demo.model.Doctor;
import com.example.demo.model.Hospital;
import com.example.demo.model.Notification;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.DoctorRepository;
import com.example.demo.repository.HospitalRepository;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private HospitalRepository hospitalRepository; 

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    public Appointment bookAppointment(Appointment appointment) {
        if (appointmentRepository.existsByPatientIdAndDoctorIdAndScheduleId(
                appointment.getPatientId(),
                appointment.getDoctorId(),
                appointment.getScheduleId())) {
            throw new IllegalArgumentException("You have already booked this doctor's schedule.");
        }

        int chosenNumber = Integer.parseInt(appointment.getAppointmentNumber());
        String formattedApptNumber = String.format("APT-%03d", chosenNumber);

        appointment.setAppointmentNumber(formattedApptNumber);
        appointment.setStatus("CONFIRMED");

        Appointment savedAppointment = appointmentRepository.save(appointment);

        createAppointmentNotification(savedAppointment);

        return savedAppointment;
    }

    private void createAppointmentNotification(Appointment appointment) {
        try {
            String doctorName = "Doctor";
            if (appointment.getDoctorId() != null && doctorRepository != null) {
                Optional<Doctor> doctorOpt = doctorRepository.findById(appointment.getDoctorId());
                if (doctorOpt.isPresent()) {
                    Doctor doc = doctorOpt.get();
                    String title = (doc.getTitle() != null && !doc.getTitle().isEmpty()) ? doc.getTitle() : "Dr.";
                    String firstName = doc.getFirstName() != null ? doc.getFirstName() : "";
                    String lastName = doc.getLastName() != null ? doc.getLastName() : "";
                    String fullName = (title + " " + firstName + " " + lastName).trim();
                    if (!fullName.isEmpty()) {
                        doctorName = fullName;
                    }
                }
            }

            Notification notification = new Notification();
            notification.setUserId(appointment.getPatientId());
            notification.setTitle("Appointment Booked Successfully");
            notification.setDoctorId(appointment.getDoctorId());
            notification.setDoctorName(doctorName);
            notification.setScheduleId(appointment.getScheduleId());
            notification.setScheduleType("PHYSICAL");
            notification.setDate(appointment.getDate());
            notification.setTime(appointment.getTime());
            notification.setHospitalId(appointment.getHospitalId());
            notification.setRead(false);
            notification.setCreatedAt(LocalDateTime.now());

            String dateStr = appointment.getDate() != null ? appointment.getDate() : "";
            String timeStr = appointment.getTime() != null ? appointment.getTime() : "";
            String message = "Your physical appointment with " + doctorName + " has been successfully booked for " + dateStr + (timeStr.isEmpty() ? "" : " at " + timeStr) + ".";
            notification.setMessage(message);

            if (notificationRepository != null) {
                notificationRepository.save(notification);
            }
        } catch (Exception e) {
            System.err.println("Failed to create appointment notification: " + e.getMessage());
        }
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

    public List<Appointment> autoAssignNumbers(String hospitalId, String date) {

        List<Appointment> appointments = appointmentRepository.findByHospitalId(hospitalId);

        List<Appointment> filtered = appointments.stream()
                .filter(a -> date.equals(a.getDate()))
                .filter(a -> !"CANCELLED".equalsIgnoreCase(a.getStatus()))
                .sorted((a, b) -> {
                    if (a.getDoctorId() == null) return 1;
                    if (b.getDoctorId() == null) return -1;

                    int doctorCompare = a.getDoctorId().compareTo(b.getDoctorId());
                    if (doctorCompare != 0) return doctorCompare;

                    if (a.getTime() == null) return 1;
                    if (b.getTime() == null) return -1;

                    return a.getTime().compareTo(b.getTime());
                })
                .toList();

        String currentDoctor = "";
        int number = 1;

        for (Appointment appointment : filtered) {
            if (!appointment.getDoctorId().equals(currentDoctor)) {
                currentDoctor = appointment.getDoctorId();
                number = 1;
            }

            appointment.setAppointmentNumber("APT-" + String.format("%03d", number));
            number++;
        }

        return appointmentRepository.saveAll(filtered);
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


                    //  Fetch hospital name using the hospitalId from the appointment
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