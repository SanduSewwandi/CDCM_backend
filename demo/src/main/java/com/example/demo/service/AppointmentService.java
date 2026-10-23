package com.example.demo.service;

import com.example.demo.dto.AppointmentResponseDTO;
import com.example.demo.model.Appointment;
import com.example.demo.model.Doctor;
import com.example.demo.model.Hospital;
import com.example.demo.model.Notification;
import com.example.demo.model.Patient;
import com.example.demo.model.Schedule;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.DoctorRepository;
import com.example.demo.repository.HospitalRepository;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.repository.PatientRepository;
import com.example.demo.repository.ScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.demo.model.Conversation;
import com.example.demo.service.ChatService;

import com.example.demo.dto.RefundPreviewDTO;
import com.example.demo.model.CancellationRefundPolicy;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
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
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ChatService chatService;


    public Appointment bookAppointment(Appointment appointment) {
        // Clean up any prior unconfirmed/unpaid attempts for this patient on this schedule so they can retry
        if (appointment.getPatientId() != null && appointment.getScheduleId() != null && appointmentRepository != null) {
            try {
                List<Appointment> existingList = appointmentRepository.findByPatientId(appointment.getPatientId());
                if (existingList != null) {
                    for (Appointment existing : existingList) {
                        if (appointment.getScheduleId().equals(existing.getScheduleId())
                                && (appointment.getDoctorId() == null || appointment.getDoctorId().equals(existing.getDoctorId()))) {
                            if (existing.isPaid() || "CONFIRMED".equalsIgnoreCase(existing.getStatus()) || "PAID".equalsIgnoreCase(existing.getPaymentStatus())) {
                                throw new IllegalArgumentException("You have already booked this doctor's schedule.");
                            } else {
                                appointmentRepository.delete(existing);
                            }
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception ignored) {}
        }

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
        appointment.setCreatedAt(LocalDateTime.now());
        if (appointment.getAmount() <= 0) {
            appointment.setAmount(1000.00);
        }

        // Ensure hospitalId is populated from schedule if missing
        if ((appointment.getHospitalId() == null || appointment.getHospitalId().trim().isEmpty() || "null".equalsIgnoreCase(appointment.getHospitalId()))
                && appointment.getScheduleId() != null && scheduleRepository != null) {
            try {
                scheduleRepository.findById(appointment.getScheduleId()).ifPresent(s -> {
                    if (s.getHospitalId() != null && !s.getHospitalId().trim().isEmpty()) {
                        appointment.setHospitalId(s.getHospitalId());
                    }
                });
            } catch (Exception ignored) {}
        }

        // Fallback: Check doctor's associated hospital if still missing
        if ((appointment.getHospitalId() == null || appointment.getHospitalId().trim().isEmpty() || "null".equalsIgnoreCase(appointment.getHospitalId()))
                && appointment.getDoctorId() != null && doctorRepository != null) {
            try {
                doctorRepository.findById(appointment.getDoctorId()).ifPresent(d -> {
                    if (d.getHospitals() != null && !d.getHospitals().isEmpty()) {
                        appointment.setHospitalId(d.getHospitals().get(0));
                    }
                });
            } catch (Exception ignored) {}
        }

        // Populate patientName if missing
        if ((appointment.getPatientName() == null || appointment.getPatientName().trim().isEmpty())
                && appointment.getPatientId() != null && patientRepository != null) {
            try {
                patientRepository.findById(appointment.getPatientId()).ifPresent(p -> {
                    String fullName = ((p.getFirstName() != null ? p.getFirstName() : "") + " " + (p.getLastName() != null ? p.getLastName() : "")).trim();
                    if (!fullName.isEmpty()) {
                        appointment.setPatientName(fullName);
                    }
                });
            } catch (Exception ignored) {}
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

            // Note: Patient notification is sent only after successful payment confirmation in PaymentService

            // 1. Notify Doctor
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
        List<Appointment> all = appointmentRepository.findByScheduleId(scheduleId);
        if (all == null) return List.of();

        return all.stream()
                .filter(appt -> appt.isPaid()
                        || "CONFIRMED".equalsIgnoreCase(appt.getStatus())
                        || "PAID".equalsIgnoreCase(appt.getPaymentStatus()))
                .collect(Collectors.toList());
    }

    public Appointment cancelPendingAppointment(String appointmentId) {
        if (appointmentId == null || appointmentId.trim().isEmpty()) {
            return null;
        }
        Optional<Appointment> opt = appointmentRepository.findById(appointmentId.trim());
        if (opt.isPresent()) {
            Appointment appt = opt.get();
            // Only cancel if it has not already been confirmed or paid
            if (!appt.isPaid() && !"PAID".equalsIgnoreCase(appt.getPaymentStatus()) && !"CONFIRMED".equalsIgnoreCase(appt.getStatus())) {
                appt.setStatus("CANCELLED");
                appt.setPaymentStatus("FAILED");
                return appointmentRepository.save(appt);
            }
            return appt;
        }
        return null;
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
                    dto.setAmount(appt.getAmount());
                    dto.setRefundPercentage(appt.getRefundPercentage());
                    dto.setRefundAmount(appt.getRefundAmount());
                    dto.setRefundStatus(appt.getRefundStatus());
                    dto.setCancelledBy(appt.getCancelledBy());
                    dto.setRefundReason(appt.getRefundReason());
                    dto.setRefundedAt(appt.getRefundedAt());

                    boolean accessActive = appt.getMedicalHistoryAccessExpiresAt() != null
                            && LocalDateTime.now().isBefore(appt.getMedicalHistoryAccessExpiresAt());
                    dto.setMedicalHistoryAccessActive(accessActive);
                    dto.setMedicalHistoryAccessExpiresAt(appt.getMedicalHistoryAccessExpiresAt());

                    // Fetch hospital name
                    if (appt.getHospitalId() != null) {
                        hospitalRepository.findById(appt.getHospitalId()).ifPresent(h -> {
                            dto.setHospitalName(h.getName());
                        });
                    }

                    // Fetch patient name from Patient collection
                    if (appt.getPatientId() != null) {
                        patientRepository.findById(appt.getPatientId()).ifPresent(p -> {
                            dto.setPatientName(p.getFirstName() + " " + p.getLastName());
                            dto.setProfileImage(p.getProfileImage());
                        });
                    }

                    return dto;
                })
                .sorted(Comparator.comparing(AppointmentResponseDTO::getDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AppointmentResponseDTO::getAppointmentNumber, Comparator.nullsLast(Comparator.naturalOrder())))
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
        if (doctorId == null || doctorId.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<Appointment> appointments = appointmentRepository.findByDoctorId(doctorId);
        if (appointments == null || appointments.isEmpty()) {
            return Collections.emptyList();
        }

        List<Appointment> validAppointments = appointments;

        if (scheduleRepository != null) {
            Set<String> scheduleIds = appointments.stream()
                    .map(Appointment::getScheduleId)
                    .filter(sId -> sId != null && !sId.trim().isEmpty())
                    .collect(Collectors.toSet());

            Set<String> validScheduleIds = new HashSet<>();
            if (!scheduleIds.isEmpty()) {
                Iterable<Schedule> existingSchedules = scheduleRepository.findAllById(scheduleIds);
                if (existingSchedules != null) {
                    for (Schedule schedule : existingSchedules) {
                        if (schedule != null && schedule.getId() != null
                                && doctorId.equals(schedule.getDoctorId())
                                && !"CANCELLED".equalsIgnoreCase(schedule.getStatus())
                                && !"REJECTED".equalsIgnoreCase(schedule.getStatus())) {
                            validScheduleIds.add(schedule.getId());
                        }
                    }
                }
            }

            validAppointments = appointments.stream()
                    .filter(appt -> appt.getScheduleId() != null
                            && validScheduleIds.contains(appt.getScheduleId())
                            && !"CANCELLED".equalsIgnoreCase(appt.getStatus()))
                    .collect(Collectors.toList());
        }

        return validAppointments.stream().map(appt -> {
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
                    dto.setAmount(appt.getAmount());
                    dto.setRefundPercentage(appt.getRefundPercentage());
                    dto.setRefundAmount(appt.getRefundAmount());
                    dto.setRefundStatus(appt.getRefundStatus());
                    dto.setCancelledBy(appt.getCancelledBy());
                    dto.setRefundReason(appt.getRefundReason());
                    dto.setRefundedAt(appt.getRefundedAt());

                    boolean docAccessActive = appt.getMedicalHistoryAccessExpiresAt() != null
                            && LocalDateTime.now().isBefore(appt.getMedicalHistoryAccessExpiresAt());
                    dto.setMedicalHistoryAccessActive(docAccessActive);
                    dto.setMedicalHistoryAccessExpiresAt(appt.getMedicalHistoryAccessExpiresAt());

                    //  Fetch hospital name using the hospitalId from the appointment
                    if (appt.getHospitalId() != null) {
                        hospitalRepository.findById(appt.getHospitalId()).ifPresent(h -> {
                            dto.setHospitalName(h.getName());
                        });
                    } else {
                        dto.setHospitalName("General Clinic"); // Fallback
                    }

                    // Fetch patient details from the Patient collection
                    if (appt.getPatientId() != null) {
                        patientRepository.findById(appt.getPatientId()).ifPresent(p -> {
                            dto.setPatientName(p.getFirstName() + " " + p.getLastName());
                            dto.setProfileImage(p.getProfileImage());
                        });
                    }

                    return dto;
                })
                // Sort by Date first (Newest first), then by Appointment Number
                .sorted(Comparator.comparing(AppointmentResponseDTO::getDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AppointmentResponseDTO::getAppointmentNumber, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    // =========================================================
    // CANCELLATION & REFUND POLICY LOGIC
    // =========================================================

    public LocalDateTime parseAppointmentDateTime(String dateStr, String timeStr, String scheduleId) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            if (scheduleId != null && scheduleRepository != null) {
                Optional<Schedule> sOpt = scheduleRepository.findById(scheduleId);
                if (sOpt.isPresent() && sOpt.get().getDate() != null) {
                    dateStr = sOpt.get().getDate();
                    if ((timeStr == null || timeStr.trim().isEmpty()) && sOpt.get().getStartTime() != null) {
                        timeStr = sOpt.get().getStartTime();
                    }
                }
            }
        }
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        LocalDate date = null;
        String trimmedDate = dateStr.trim();
        if (trimmedDate.contains("T")) {
            trimmedDate = trimmedDate.split("T")[0].trim();
        }

        String[] datePatterns = { "yyyy-MM-dd", "yyyy/MM/dd", "dd-MM-yyyy", "dd/MM/yyyy" };
        for (String pattern : datePatterns) {
            try {
                date = LocalDate.parse(trimmedDate, DateTimeFormatter.ofPattern(pattern));
                break;
            } catch (Exception ignored) {}
        }

        if (date == null) {
            return null;
        }

        LocalTime time = null;
        if (timeStr != null && !timeStr.trim().isEmpty()) {
            String trimmedTime = timeStr.trim();
            if (trimmedTime.contains("-")) {
                trimmedTime = trimmedTime.split("-")[0].trim();
            }
            String[] timePatterns = { "h:mm a", "hh:mm a", "H:mm", "HH:mm", "H:mm:ss", "HH:mm:ss" };
            for (String pattern : timePatterns) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
                    time = LocalTime.parse(trimmedTime, formatter);
                    break;
                } catch (Exception ignored) {}
            }
        }

        if (time == null && scheduleId != null && scheduleRepository != null) {
            try {
                Optional<Schedule> sOpt = scheduleRepository.findById(scheduleId);
                if (sOpt.isPresent() && sOpt.get().getStartTime() != null) {
                    String sTime = sOpt.get().getStartTime().trim();
                    if (sTime.contains("-")) {
                        sTime = sTime.split("-")[0].trim();
                    }
                    String[] timePatterns = { "h:mm a", "hh:mm a", "H:mm", "HH:mm", "H:mm:ss", "HH:mm:ss" };
                    for (String pattern : timePatterns) {
                        try {
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
                            time = LocalTime.parse(sTime, formatter);
                            break;
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception ignored) {}
        }

        if (time == null) {
            time = LocalTime.of(0, 0);
        }

        return LocalDateTime.of(date, time);
    }

    public CancellationRefundPolicy resolveHospitalRefundPolicy(String hospitalId, String scheduleId, String doctorId) {
        if (hospitalId != null && !hospitalId.trim().isEmpty() && !"null".equalsIgnoreCase(hospitalId) && hospitalRepository != null) {
            try {
                Optional<Hospital> hOpt = hospitalRepository.findById(hospitalId.trim());
                if (hOpt.isPresent() && hOpt.get().getCancellationRefundPolicy() != null) {
                    return hOpt.get().getCancellationRefundPolicy();
                }
            } catch (Exception ignored) {}
        }

        if (scheduleId != null && scheduleRepository != null && hospitalRepository != null) {
            try {
                Optional<Schedule> sOpt = scheduleRepository.findById(scheduleId);
                if (sOpt.isPresent() && sOpt.get().getHospitalId() != null) {
                    Optional<Hospital> hOpt = hospitalRepository.findById(sOpt.get().getHospitalId().trim());
                    if (hOpt.isPresent() && hOpt.get().getCancellationRefundPolicy() != null) {
                        return hOpt.get().getCancellationRefundPolicy();
                    }
                }
            } catch (Exception ignored) {}
        }

        if (doctorId != null && doctorRepository != null && hospitalRepository != null) {
            try {
                Optional<Doctor> dOpt = doctorRepository.findById(doctorId);
                if (dOpt.isPresent() && dOpt.get().getHospitals() != null && !dOpt.get().getHospitals().isEmpty()) {
                    Optional<Hospital> hOpt = hospitalRepository.findById(dOpt.get().getHospitals().get(0));
                    if (hOpt.isPresent() && hOpt.get().getCancellationRefundPolicy() != null) {
                        return hOpt.get().getCancellationRefundPolicy();
                    }
                }
            } catch (Exception ignored) {}
        }

        return new CancellationRefundPolicy();
    }

    public double calculateRefundPercentage(Appointment appt, LocalDateTime cancelTime) {
        LocalDateTime apptStart = parseAppointmentDateTime(appt.getDate(), appt.getTime(), appt.getScheduleId());
        if (apptStart == null) {
            throw new IllegalArgumentException("Cannot determine appointment start time.");
        }
        if (!apptStart.isAfter(cancelTime)) {
            throw new IllegalArgumentException("Cannot cancel an appointment that has already started or is in the past.");
        }

        double hoursRemaining = Duration.between(cancelTime, apptStart).toSeconds() / 3600.0;
        CancellationRefundPolicy policy = resolveHospitalRefundPolicy(appt.getHospitalId(), appt.getScheduleId(), appt.getDoctorId());

        if (hoursRemaining >= 48.0) {
            return policy.getOver48HoursPercent();
        } else if (hoursRemaining >= 24.0) {
            return policy.getBetween24And48HoursPercent();
        } else if (hoursRemaining >= 6.0) {
            return policy.getBetween6And24HoursPercent();
        } else {
            return policy.getUnder6HoursPercent();
        }
    }

    public RefundPreviewDTO getCancellationRefundPreview(String appointmentId, String patientEmail) {
        if (appointmentId == null || appointmentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Appointment ID is required.");
        }
        if (patientEmail == null || patientEmail.trim().isEmpty()) {
            throw new SecurityException("Authentication is required.");
        }

        Appointment appt = appointmentRepository.findById(appointmentId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found with id: " + appointmentId));

        Optional<Patient> pOpt = patientRepository.findByEmail(patientEmail.trim().toLowerCase());
        if (pOpt.isEmpty()) {
            pOpt = patientRepository.findByEmail(patientEmail.trim());
        }
        Patient patient = pOpt.orElseThrow(() -> new SecurityException("Authenticated patient profile not found."));

        if (appt.getPatientId() == null || !appt.getPatientId().equals(patient.getId())) {
            throw new SecurityException("Forbidden: You are not authorized to preview or cancel this appointment.");
        }

        if ("CANCELLED".equalsIgnoreCase(appt.getStatus())) {
            throw new IllegalArgumentException("This appointment is already cancelled.");
        }
        if ("COMPLETED".equalsIgnoreCase(appt.getStatus())) {
            throw new IllegalArgumentException("Completed appointments cannot be cancelled.");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime apptStart = parseAppointmentDateTime(appt.getDate(), appt.getTime(), appt.getScheduleId());
        if (apptStart == null || !apptStart.isAfter(now)) {
            throw new IllegalArgumentException("Cannot cancel an appointment that has already started or is in the past.");
        }

        double hoursRemaining = Duration.between(now, apptStart).toSeconds() / 3600.0;
        double refundPercentage = calculateRefundPercentage(appt, now);
        boolean isPaid = appt.isPaid() || "PAID".equalsIgnoreCase(appt.getPaymentStatus());
        double originalPayment = appt.getAmount() > 0 ? appt.getAmount() : (isPaid ? 1000.00 : 0.0);
        double eligibleRefund = isPaid ? Math.round((originalPayment * refundPercentage / 100.0) * 100.0) / 100.0 : 0.0;

        String doctorName = "";
        if (appt.getDoctorId() != null && doctorRepository != null) {
            try {
                doctorName = doctorRepository.findById(appt.getDoctorId())
                        .map(d -> ((d.getTitle() != null ? d.getTitle() : "Dr.") + " " +
                                (d.getFirstName() != null ? d.getFirstName() : "") + " " +
                                (d.getLastName() != null ? d.getLastName() : "")).trim())
                        .orElse("");
            } catch (Exception ignored) {}
        }

        String hospitalName = "";
        if (appt.getHospitalId() != null && hospitalRepository != null) {
            try {
                hospitalName = hospitalRepository.findById(appt.getHospitalId())
                        .map(Hospital::getName)
                        .orElse("");
            } catch (Exception ignored) {}
        }

        RefundPreviewDTO preview = new RefundPreviewDTO();
        preview.setAppointmentId(appt.getId());
        preview.setAppointmentNumber(appt.getAppointmentNumber());
        preview.setPatientId(appt.getPatientId());
        preview.setPatientName(appt.getPatientName());
        preview.setDoctorId(appt.getDoctorId());
        preview.setDoctorName(doctorName);
        preview.setHospitalId(appt.getHospitalId());
        preview.setHospitalName(hospitalName);
        preview.setDate(appt.getDate());
        preview.setTime(appt.getTime());
        preview.setPaid(isPaid);
        preview.setStatus(appt.getStatus());
        preview.setOriginalPayment(originalPayment);
        preview.setRefundPercentage(refundPercentage);
        preview.setEligibleRefund(eligibleRefund);
        preview.setHoursRemaining(Math.round(hoursRemaining * 100.0) / 100.0);
        preview.setEligibleForRefund(isPaid && eligibleRefund > 0);
        preview.setMessage(isPaid 
                ? "Cancellation will record a refund of " + refundPercentage + "% (Rs. " + String.format(Locale.US, "%.2f", eligibleRefund) + ")."
                : "Appointment is unpaid. No refund will be recorded.");

        return preview;
    }

    public Appointment cancelAppointmentByPatient(String appointmentId, String patientEmail, String reason) {
        if (appointmentId == null || appointmentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Appointment ID is required.");
        }
        if (patientEmail == null || patientEmail.trim().isEmpty()) {
            throw new SecurityException("Authentication is required.");
        }

        Appointment appt = appointmentRepository.findById(appointmentId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found with id: " + appointmentId));

        Optional<Patient> pOpt = patientRepository.findByEmail(patientEmail.trim().toLowerCase());
        if (pOpt.isEmpty()) {
            pOpt = patientRepository.findByEmail(patientEmail.trim());
        }
        Patient patient = pOpt.orElseThrow(() -> new SecurityException("Authenticated patient profile not found."));

        if (appt.getPatientId() == null || !appt.getPatientId().equals(patient.getId())) {
            throw new SecurityException("Forbidden: You are not authorized to cancel this appointment.");
        }

        if ("CANCELLED".equalsIgnoreCase(appt.getStatus())) {
            throw new IllegalArgumentException("This appointment is already cancelled.");
        }
        if ("COMPLETED".equalsIgnoreCase(appt.getStatus())) {
            throw new IllegalArgumentException("Completed appointments cannot be cancelled.");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime apptStart = parseAppointmentDateTime(appt.getDate(), appt.getTime(), appt.getScheduleId());
        if (apptStart == null || !apptStart.isAfter(now)) {
            throw new IllegalArgumentException("Cannot cancel an appointment that has already started or is in the past.");
        }

        boolean isPaid = appt.isPaid() || "PAID".equalsIgnoreCase(appt.getPaymentStatus());
        appt.setStatus("CANCELLED");
        appt.setCancelledBy("PATIENT");
        appt.setRefundReason(reason != null && !reason.trim().isEmpty() ? reason.trim() : "Cancelled by patient");

        if (isPaid) {
            double refundPercentage = calculateRefundPercentage(appt, now);
            double originalPayment = appt.getAmount() > 0 ? appt.getAmount() : 1000.00;
            double refundAmount = Math.round((originalPayment * refundPercentage / 100.0) * 100.0) / 100.0;
            appt.setRefundPercentage(refundPercentage);
            appt.setRefundAmount(refundAmount);
            appt.setRefundStatus("REFUNDED");
            appt.setRefundedAt(now);
        } else {
            appt.setPaymentStatus("FAILED");
            appt.setRefundPercentage(0.0);
            appt.setRefundAmount(0.0);
            appt.setRefundStatus(null);
        }

        Appointment saved = appointmentRepository.save(appt);

        // Notify Doctor and Hospital
        try {
            String apptNumStr = saved.getAppointmentNumber() != null ? saved.getAppointmentNumber() : saved.getId();
            String patientNameStr = patient.getFirstName() + " " + patient.getLastName();
            String dateStr = saved.getDate() != null ? saved.getDate() : "";
            String timeStr = saved.getTime() != null ? " at " + saved.getTime() : "";

            if (saved.getDoctorId() != null && notificationRepository != null) {
                Notification docNote = new Notification();
                docNote.setUserId(saved.getDoctorId());
                docNote.setDoctorId(saved.getDoctorId());
                docNote.setScheduleId(saved.getScheduleId());
                docNote.setDate(saved.getDate());
                docNote.setTime(saved.getTime());
                docNote.setHospitalId(saved.getHospitalId());
                docNote.setTitle("Appointment Cancelled by Patient");
                docNote.setMessage("Patient " + patientNameStr + " has cancelled appointment #" + apptNumStr + " on " + dateStr + timeStr + ".");
                docNote.setCreatedAt(now);
                docNote.setRead(false);
                notificationRepository.save(docNote);
            }

            if (saved.getHospitalId() != null && notificationRepository != null) {
                Notification hospNote = new Notification();
                hospNote.setUserId(saved.getHospitalId());
                hospNote.setHospitalId(saved.getHospitalId());
                hospNote.setDoctorId(saved.getDoctorId());
                hospNote.setScheduleId(saved.getScheduleId());
                hospNote.setDate(saved.getDate());
                hospNote.setTime(saved.getTime());
                hospNote.setTitle("Appointment Cancelled by Patient");
                hospNote.setMessage("Appointment #" + apptNumStr + " cancelled by patient. Refund status: " + 
                        (saved.getRefundStatus() != null ? saved.getRefundStatus() + " (" + saved.getRefundPercentage() + "% - LKR " + saved.getRefundAmount() + ")" : "Unpaid") + ".");
                hospNote.setCreatedAt(now);
                hospNote.setRead(false);
                notificationRepository.save(hospNote);
            }
        } catch (Exception e) {
            System.err.println("Failed to send cancellation notifications: " + e.getMessage());
        }

        return saved;
    }

    public Appointment cancelAppointmentByHospital(String appointmentId, String hospitalEmail, String reason) {
        if (appointmentId == null || appointmentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Appointment ID is required.");
        }
        if (hospitalEmail == null || hospitalEmail.trim().isEmpty()) {
            throw new SecurityException("Authentication is required.");
        }

        Hospital hospital = hospitalRepository.findByEmail(hospitalEmail.trim().toLowerCase())
                .orElseThrow(() -> new SecurityException("Hospital profile not found."));

        Appointment appt = appointmentRepository.findById(appointmentId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found with id: " + appointmentId));

        if (appt.getHospitalId() != null && !appt.getHospitalId().equals(hospital.getId())) {
            throw new SecurityException("Forbidden: You are not authorized to cancel an appointment belonging to another hospital.");
        }

        if ("CANCELLED".equalsIgnoreCase(appt.getStatus())) {
            throw new IllegalArgumentException("This appointment is already cancelled.");
        }

        LocalDateTime now = LocalDateTime.now();
        boolean isPaid = appt.isPaid() || "PAID".equalsIgnoreCase(appt.getPaymentStatus());

        appt.setStatus("CANCELLED");
        appt.setCancelledBy("HOSPITAL");
        appt.setRefundReason(reason != null && !reason.trim().isEmpty() ? reason.trim() : "Cancelled by hospital: " + hospital.getName());

        if (isPaid) {
            double originalPayment = appt.getAmount() > 0 ? appt.getAmount() : 1000.00;
            appt.setRefundPercentage(100.0);
            appt.setRefundAmount(originalPayment);
            appt.setRefundStatus("REFUNDED");
            appt.setRefundedAt(now);
        } else {
            appt.setPaymentStatus("FAILED");
            appt.setRefundPercentage(0.0);
            appt.setRefundAmount(0.0);
            appt.setRefundStatus(null);
        }

        return appointmentRepository.save(appt);
    }
}