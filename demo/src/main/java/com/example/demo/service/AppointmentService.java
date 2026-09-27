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
import com.example.demo.model.Conversation;
import com.example.demo.service.ChatService;

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

    @Autowired
    private ChatService chatService;


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
        appointment.setStatus("PENDING");
        appointment.setPaymentStatus("PENDING");
        appointment.setPaid(false);
        if (appointment.getAmount() <= 0) {
            appointment.setAmount(1000.00);
        }

        Appointment savedAppointment = appointmentRepository.save(appointment);

// Create chat conversation for this appointment
        chatService.createConversation(
                savedAppointment.getId(),
                savedAppointment.getPatientId(),
                savedAppointment.getDoctorId()
        );

        // Send notifications across the system (Patient, Doctor, Hospital)
        createAppointmentNotification(savedAppointment);

        return savedAppointment;
    }

    private void createAppointmentNotification(Appointment appointment) {
        try {
            LocalDateTime now = LocalDateTime.now();
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

            String dateStr = appointment.getDate() != null ? appointment.getDate() : "";
            String timeStr = appointment.getTime() != null ? appointment.getTime() : "";
            String timeSuffix = timeStr.isEmpty() ? "" : " at " + timeStr;
            String apptNum = appointment.getAppointmentNumber() != null ? appointment.getAppointmentNumber() : "N/A";

            // 1. Notify Patient
            if (appointment.getPatientId() != null && notificationRepository != null) {
                Notification patientNote = new Notification();
                patientNote.setUserId(appointment.getPatientId());
                patientNote.setTitle("Appointment Booked Successfully");
                patientNote.setDoctorId(appointment.getDoctorId());
                patientNote.setDoctorName(doctorName);
                patientNote.setScheduleId(appointment.getScheduleId());
                patientNote.setScheduleType(appointment.getConsultationType() != null ? appointment.getConsultationType() : "PHYSICAL");
                patientNote.setDate(appointment.getDate());
                patientNote.setTime(appointment.getTime());
                patientNote.setHospitalId(appointment.getHospitalId());
                patientNote.setRead(false);
                patientNote.setCreatedAt(now);
                patientNote.setMessage("Your appointment (" + apptNum + ") with " + doctorName + " is booked for " + dateStr + timeSuffix + ". Please complete payment to confirm.");
                notificationRepository.save(patientNote);
            }

            // 2. Notify Doctor
            if (appointment.getDoctorId() != null && notificationRepository != null) {
                Notification docNote = new Notification();
                docNote.setUserId(appointment.getDoctorId());
                docNote.setDoctorId(appointment.getDoctorId());
                docNote.setDoctorName(doctorName);
                docNote.setScheduleId(appointment.getScheduleId());
                docNote.setScheduleType(appointment.getConsultationType() != null ? appointment.getConsultationType() : "PHYSICAL");
                docNote.setDate(appointment.getDate());
                docNote.setTime(appointment.getTime());
                docNote.setHospitalId(appointment.getHospitalId());
                docNote.setTitle("New Appointment Booking");
                docNote.setMessage("A patient has booked appointment (" + apptNum + ") with you for " + dateStr + timeSuffix + ".");
                docNote.setRead(false);
                docNote.setCreatedAt(now);
                notificationRepository.save(docNote);
            }

            // 3. Notify Hospital
            if (appointment.getHospitalId() != null && notificationRepository != null) {
                Notification hospNote = new Notification();
                hospNote.setUserId(appointment.getHospitalId());
                hospNote.setHospitalId(appointment.getHospitalId());
                hospNote.setDoctorId(appointment.getDoctorId());
                hospNote.setDoctorName(doctorName);
                hospNote.setScheduleId(appointment.getScheduleId());
                hospNote.setScheduleType(appointment.getConsultationType() != null ? appointment.getConsultationType() : "PHYSICAL");
                hospNote.setDate(appointment.getDate());
                hospNote.setTime(appointment.getTime());
                hospNote.setTitle("New Appointment Booking");
                hospNote.setMessage("Appointment (" + apptNum + ") was booked for " + doctorName + " on " + dateStr + timeSuffix + ".");
                hospNote.setRead(false);
                hospNote.setCreatedAt(now);
                notificationRepository.save(hospNote);
            }

        } catch (Exception e) {
            System.err.println("Failed to create appointment notifications: " + e.getMessage());
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