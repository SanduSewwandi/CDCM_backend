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

import com.example.demo.dto.BankDetailsRequest;
import com.example.demo.dto.RefundPreviewDTO;
import com.example.demo.model.CancellationRefundPolicy;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

    public static final ZoneId SRI_LANKA_ZONE = ZoneId.of("Asia/Colombo");

    public LocalDateTime nowInSriLanka() {
        return ZonedDateTime.now(SRI_LANKA_ZONE).toLocalDateTime();
    }

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

    @Autowired
    private EmailService emailService;


    public Appointment bookAppointment(Appointment appointment) {
        if (appointment == null) {
            throw new IllegalArgumentException("Appointment details are required.");
        }

        // Validate schedule status and expiration
        if (appointment.getScheduleId() != null && scheduleRepository != null) {
            Optional<Schedule> sOpt = scheduleRepository.findById(appointment.getScheduleId().trim());
            if (sOpt.isPresent()) {
                Schedule schedule = sOpt.get();
                String scheduleStatus = schedule.getStatus() != null ? schedule.getStatus().trim().toUpperCase() : "";
                if ("PENDING".equals(scheduleStatus)) {
                    throw new IllegalStateException("Cannot book an appointment for a schedule pending doctor acceptance.");
                }
                if ("REJECTED".equals(scheduleStatus)) {
                    throw new IllegalStateException("Cannot book an appointment for a rejected schedule.");
                }
                if ("CANCELLED".equals(scheduleStatus)) {
                    throw new IllegalStateException("Cannot book an appointment for a cancelled schedule.");
                }
                if (!"ACCEPTED".equals(scheduleStatus)) {
                    throw new IllegalStateException("Cannot book an appointment for an inactive schedule. Current status: " + schedule.getStatus());
                }
                if (isSchedulePast(schedule.getDate(), schedule.getEndTime())) {
                    throw new IllegalArgumentException("Cannot book an appointment for an expired schedule.");
                }
                if ((appointment.getHospitalId() == null || appointment.getHospitalId().trim().isEmpty() || "null".equalsIgnoreCase(appointment.getHospitalId()))
                        && schedule.getHospitalId() != null && !schedule.getHospitalId().trim().isEmpty()) {
                    appointment.setHospitalId(schedule.getHospitalId());
                }
            }
        }

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

        // Check if the appointment number is already booked and confirmed on this schedule
        if (appointment.getScheduleId() != null && appointmentRepository != null) {
            List<Appointment> scheduleAppointments = appointmentRepository.findByScheduleId(appointment.getScheduleId());
            if (scheduleAppointments != null) {
                for (Appointment existing : scheduleAppointments) {
                    if (formattedApptNumber.equalsIgnoreCase(existing.getAppointmentNumber())) {
                        if (existing.isPaid() || "CONFIRMED".equalsIgnoreCase(existing.getStatus()) || "PAID".equalsIgnoreCase(existing.getPaymentStatus())) {
                            throw new IllegalArgumentException("Appointment number " + formattedApptNumber + " is already booked for this schedule.");
                        } else if (appointment.getId() == null || !appointment.getId().equals(existing.getId())) {
                            // Abandoned or unconfirmed attempt for this number; clean it up so it does not permanently block the slot
                            try {
                                appointmentRepository.delete(existing);
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        }

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

        // Note: Booking confirmation notifications are sent to Patient, Doctor, and Hospital
        // ONLY after successful payment confirmation in PaymentService.confirmPaymentSuccess

        return savedAppointment;
    }

    public List<Appointment> getAppointmentsForPatient(String patientId) {
        List<Appointment> all = appointmentRepository.findByPatientId(patientId);
        if (all == null) return List.of();

        return all.stream()
                .filter(appt -> {
                    // Must not be pending
                    if ("PENDING".equalsIgnoreCase(appt.getStatus()) || "PENDING".equalsIgnoreCase(appt.getPaymentStatus())) {
                        return false;
                    }
                    if ("FAILED".equalsIgnoreCase(appt.getPaymentStatus())) {
                        return false;
                    }
                    boolean isPaid = appt.isPaid() || "PAID".equalsIgnoreCase(appt.getPaymentStatus());
                    if (!isPaid) {
                        return false;
                    }
                    return "CONFIRMED".equalsIgnoreCase(appt.getStatus())
                            || "COMPLETED".equalsIgnoreCase(appt.getStatus())
                            || "PAID".equalsIgnoreCase(appt.getStatus())
                            || "CANCELLED".equalsIgnoreCase(appt.getStatus());
                })
                .collect(Collectors.toList());
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

    public AppointmentResponseDTO mapToResponseDTO(Appointment appt) {
        if (appt == null) return null;
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

        dto.setScheduleId(appt.getScheduleId());
        dto.setBankAccountHolderName(appt.getBankAccountHolderName());
        dto.setBankName(appt.getBankName());
        dto.setBankBranchName(appt.getBankBranchName());
        dto.setMaskedBankAccountNumber(appt.getMaskedBankAccountNumber());
        dto.setBankDetailsSubmittedAt(appt.getBankDetailsSubmittedAt());
        dto.setRefundTransactionRef(appt.getRefundTransactionRef());
        dto.setRefundConfirmedBy(appt.getRefundConfirmedBy());

        boolean accessActive = appt.getMedicalHistoryAccessExpiresAt() != null
                && nowInSriLanka().isBefore(appt.getMedicalHistoryAccessExpiresAt());
        dto.setMedicalHistoryAccessActive(accessActive);
        dto.setMedicalHistoryAccessExpiresAt(appt.getMedicalHistoryAccessExpiresAt());

        // Fetch hospital name
        if (appt.getHospitalId() != null && hospitalRepository != null) {
            hospitalRepository.findById(appt.getHospitalId()).ifPresent(h -> {
                dto.setHospitalName(h.getName());
            });
        }
        if (dto.getHospitalName() == null || dto.getHospitalName().isBlank()) {
            dto.setHospitalName("General Clinic");
        }

        // Fetch patient name from Patient collection
        if (appt.getPatientId() != null && patientRepository != null) {
            patientRepository.findById(appt.getPatientId()).ifPresent(p -> {
                dto.setPatientName((p.getFirstName() != null ? p.getFirstName() : "") + " " + (p.getLastName() != null ? p.getLastName() : ""));
                dto.setProfileImage(p.getProfileImage());
            });
        }
        if (dto.getPatientName() == null && appt.getPatientName() != null) {
            dto.setPatientName(appt.getPatientName());
        }

        return dto;
    }

    public List<AppointmentResponseDTO> getAppointmentsByHospital(String hospitalId) {
        List<Appointment> appointments = appointmentRepository.findByHospitalId(hospitalId);

        return appointments.stream()
                .map(this::mapToResponseDTO)
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

        return validAppointments.stream()
                .map(this::mapToResponseDTO)
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

    private boolean isSchedulePast(String dateStr, String timeStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return false;
        }
        try {
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
                return false;
            }

            LocalTime time = null;
            if (timeStr != null && !timeStr.trim().isEmpty()) {
                String trimmedTime = timeStr.trim();
                String[] timePatterns = { "h:mm a", "hh:mm a", "H:mm", "HH:mm", "H:mm:ss", "HH:mm:ss" };
                for (String pattern : timePatterns) {
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
                        time = LocalTime.parse(trimmedTime, formatter);
                        break;
                    } catch (Exception ignored) {}
                }
            }
            if (time == null) {
                time = LocalTime.MIN;
            }
            LocalDateTime scheduleDateTime = LocalDateTime.of(date, time);
            return !scheduleDateTime.isAfter(nowInSriLanka());
        } catch (Exception e) {
            return false;
        }
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

        LocalDateTime now = nowInSriLanka();
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

        LocalDateTime now = nowInSriLanka();
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
            if (refundAmount > 0) {
                appt.setRefundStatus("AWAITING_BANK_DETAILS");
            } else {
                appt.setRefundStatus("NOT_APPLICABLE");
            }
            appt.setRefundedAt(null);
        } else {
            appt.setPaymentStatus("FAILED");
            appt.setRefundPercentage(0.0);
            appt.setRefundAmount(0.0);
            appt.setRefundStatus("NOT_APPLICABLE");
            appt.setRefundedAt(null);
        }

        Appointment saved = appointmentRepository.save(appt);

        // Notify Doctor and Hospital
        try {
            String apptNumStr = saved.getAppointmentNumber() != null ? saved.getAppointmentNumber() : saved.getId();
            String patientNameStr = patient.getFirstName() + " " + patient.getLastName();
            String dateStr = saved.getDate() != null ? saved.getDate() : "";
            String formattedTime = EmailService.formatTimeString(saved.getTime());
            String timeStr = !formattedTime.equals("—") ? " at " + formattedTime : "";

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
                docNote.setCreatedAt(Instant.now());
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
                hospNote.setCreatedAt(Instant.now());
                hospNote.setRead(false);
                notificationRepository.save(hospNote);
            }
        } catch (Exception e) {
            System.err.println("Failed to send cancellation notifications: " + e.getMessage());
        }

        // Send Cancellation Email to Patient
        try {
            if (emailService != null && patient.getEmail() != null) {
                String doctorNameStr = "Doctor";
                if (saved.getDoctorId() != null && doctorRepository != null) {
                    Doctor d = doctorRepository.findById(saved.getDoctorId()).orElse(null);
                    if (d != null) {
                        doctorNameStr = "Dr. " + (d.getFirstName() != null ? d.getFirstName() : "") + " " + (d.getLastName() != null ? d.getLastName() : "");
                    }
                }
                String hospitalNameStr = "Hospital";
                if (saved.getHospitalId() != null && hospitalRepository != null) {
                    Hospital h = hospitalRepository.findById(saved.getHospitalId()).orElse(null);
                    if (h != null && h.getName() != null) {
                        hospitalNameStr = h.getName();
                    }
                }
                emailService.sendPatientCancellationEmail(
                        patient.getEmail(),
                        patient.getFirstName(),
                        doctorNameStr.trim(),
                        saved.getAppointmentNumber(),
                        saved.getConsultationType() != null ? saved.getConsultationType() : "Physical Consultation",
                        saved.getDate(),
                        saved.getTime(),
                        hospitalNameStr,
                        saved.getRefundReason(),
                        saved.getAmount(),
                        saved.getRefundPercentage() != null ? saved.getRefundPercentage() : 0.0,
                        saved.getRefundAmount() != null ? saved.getRefundAmount() : 0.0,
                        isPaid && saved.getRefundAmount() != null && saved.getRefundAmount() > 0
                );
            }
        } catch (Exception e) {
            System.err.println("Failed to send patient cancellation email: " + e.getMessage());
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

        LocalDateTime now = nowInSriLanka();
        boolean isPaid = appt.isPaid() || "PAID".equalsIgnoreCase(appt.getPaymentStatus());

        appt.setStatus("CANCELLED");
        appt.setCancelledBy("HOSPITAL");
        appt.setRefundReason(reason != null && !reason.trim().isEmpty() ? reason.trim() : "Cancelled by hospital: " + hospital.getName());

        if (isPaid) {
            double originalPayment = appt.getAmount() > 0 ? appt.getAmount() : 1000.00;
            appt.setRefundPercentage(100.0);
            appt.setRefundAmount(originalPayment);
            appt.setRefundStatus("AWAITING_BANK_DETAILS");
            appt.setRefundedAt(null);
        } else {
            appt.setPaymentStatus("FAILED");
            appt.setRefundPercentage(0.0);
            appt.setRefundAmount(0.0);
            appt.setRefundStatus("NOT_APPLICABLE");
            appt.setRefundedAt(null);
        }

        Appointment saved = appointmentRepository.save(appt);

        // Send Cancellation Email to Patient
        try {
            if (emailService != null && saved.getPatientId() != null) {
                Patient p = patientRepository.findById(saved.getPatientId()).orElse(null);
                if (p != null && p.getEmail() != null) {
                    String doctorNameStr = "Doctor";
                    if (saved.getDoctorId() != null && doctorRepository != null) {
                        Doctor d = doctorRepository.findById(saved.getDoctorId()).orElse(null);
                        if (d != null) {
                            doctorNameStr = "Dr. " + (d.getFirstName() != null ? d.getFirstName() : "") + " " + (d.getLastName() != null ? d.getLastName() : "");
                        }
                    }
                    emailService.sendHospitalScheduleCancellationEmail(
                            p.getEmail(),
                            p.getFirstName(),
                            doctorNameStr.trim(),
                            saved.getAppointmentNumber(),
                            saved.getConsultationType() != null ? saved.getConsultationType() : "Physical Consultation",
                            saved.getDate(),
                            saved.getTime(),
                            hospital.getName(),
                            saved.getRefundReason(),
                            saved.getAmount(),
                            isPaid
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to send hospital cancellation email: " + e.getMessage());
        }

        return saved;
    }

    // =========================================================
    // BANK DETAILS COLLECTION (Patient)
    // =========================================================
    public Appointment submitBankDetails(String appointmentId, String patientEmail, BankDetailsRequest request) {
        if (appointmentId == null || appointmentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Appointment ID is required.");
        }
        if (patientEmail == null || patientEmail.trim().isEmpty()) {
            throw new SecurityException("Authentication is required.");
        }
        if (request == null) {
            throw new IllegalArgumentException("Bank details request is required.");
        }
        if (request.getAccountHolderName() == null || request.getAccountHolderName().trim().isEmpty()) {
            throw new IllegalArgumentException("Account holder name is required.");
        }
        if (request.getBankName() == null || request.getBankName().trim().isEmpty()) {
            throw new IllegalArgumentException("Bank name is required.");
        }
        String branch = request.getBranchName() != null && !request.getBranchName().trim().isEmpty()
                ? request.getBranchName().trim()
                : (request.getBankBranchName() != null && !request.getBankBranchName().trim().isEmpty()
                ? request.getBankBranchName().trim() : null);
        if (branch == null || branch.isEmpty()) {
            throw new IllegalArgumentException("Branch name is required.");
        }
        if (request.getAccountNumber() == null || request.getAccountNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Account number is required.");
        }
        String cleanAccNum = request.getAccountNumber().trim();
        if (!cleanAccNum.matches("^[0-9A-Za-z\\-]{5,30}$")) {
            throw new IllegalArgumentException("Invalid bank account number format. Must contain 5-30 alphanumeric characters.");
        }

        Optional<Patient> pOpt = patientRepository.findByEmail(patientEmail.trim().toLowerCase());
        if (pOpt.isEmpty()) {
            pOpt = patientRepository.findByEmail(patientEmail.trim());
        }
        Patient patient = pOpt.orElseThrow(() -> new SecurityException("Authenticated patient profile not found."));

        Appointment appt = appointmentRepository.findById(appointmentId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found with id: " + appointmentId));

        if (appt.getPatientId() == null || !appt.getPatientId().equals(patient.getId())) {
            throw new SecurityException("Forbidden: You are not authorized to submit bank details for this appointment.");
        }

        if (!"CANCELLED".equalsIgnoreCase(appt.getStatus())) {
            throw new IllegalArgumentException("Bank details can only be submitted for cancelled appointments.");
        }

        if (appt.getRefundAmount() == null || appt.getRefundAmount() <= 0) {
            throw new IllegalArgumentException("This appointment does not have an eligible refund amount.");
        }

        if ("REFUNDED".equalsIgnoreCase(appt.getRefundStatus())) {
            throw new IllegalArgumentException("This refund has already been completed and confirmed.");
        }

        appt.setBankAccountHolderName(request.getAccountHolderName().trim());
        appt.setBankName(request.getBankName().trim());
        appt.setBankBranchName(branch);
        appt.setBankAccountNumber(cleanAccNum);
        appt.setBankDetailsSubmittedAt(LocalDateTime.now());
        appt.setRefundStatus("AWAITING_PROCESSING");

        Appointment saved = appointmentRepository.save(appt);

        // Notify Hospital in-app
        try {
            if (saved.getHospitalId() != null && notificationRepository != null) {
                Notification hospNote = new Notification();
                hospNote.setUserId(saved.getHospitalId());
                hospNote.setHospitalId(saved.getHospitalId());
                hospNote.setDoctorId(saved.getDoctorId());
                hospNote.setScheduleId(saved.getScheduleId());
                hospNote.setDate(saved.getDate());
                hospNote.setTime(saved.getTime());
                hospNote.setTitle("Refund Bank Details Submitted");
                hospNote.setMessage("Patient " + patient.getFirstName() + " " + patient.getLastName() + 
                        " has submitted bank details for appointment #" + (saved.getAppointmentNumber() != null ? saved.getAppointmentNumber() : saved.getId()) + 
                        ". Refund of LKR " + String.format(Locale.US, "%.2f", saved.getRefundAmount()) + " is awaiting transfer.");
                hospNote.setCreatedAt(Instant.now());
                hospNote.setRead(false);
                notificationRepository.save(hospNote);
            }
        } catch (Exception e) {
            System.err.println("Failed to notify hospital of bank details submission: " + e.getMessage());
        }

        return saved;
    }

    // =========================================================
    // HOSPITAL PENDING REFUNDS RETRIEVAL
    // =========================================================
    public List<AppointmentResponseDTO> getPendingRefundsForHospital(String hospitalId, String hospitalEmail) {
        if (hospitalId == null || hospitalId.trim().isEmpty()) {
            throw new IllegalArgumentException("Hospital ID is required.");
        }
        if (hospitalEmail == null || hospitalEmail.trim().isEmpty()) {
            throw new SecurityException("Authentication is required.");
        }

        String cleanEmail = hospitalEmail.trim().toLowerCase();
        Hospital hospital = hospitalRepository.findByEmail(cleanEmail)
                .or(() -> hospitalRepository.findByEmail(hospitalEmail.trim()))
                .orElseThrow(() -> new SecurityException("Hospital profile not found."));

        if (!hospital.getId().equals(hospitalId.trim())) {
            throw new SecurityException("Forbidden: You cannot access refund records belonging to another hospital.");
        }

        List<Appointment> allAppts = appointmentRepository.findByHospitalId(hospital.getId());

        return allAppts.stream()
                .filter(a -> "CANCELLED".equalsIgnoreCase(a.getStatus()) &&
                        ("AWAITING_BANK_DETAILS".equalsIgnoreCase(a.getRefundStatus()) ||
                         "AWAITING_PROCESSING".equalsIgnoreCase(a.getRefundStatus())))
                .map(a -> {
                    // If bank details were submitted but status is still awaiting bank details, self-heal status
                    if (a.getBankAccountNumber() != null && !a.getBankAccountNumber().trim().isEmpty() &&
                            "AWAITING_BANK_DETAILS".equalsIgnoreCase(a.getRefundStatus())) {
                        a.setRefundStatus("AWAITING_PROCESSING");
                        appointmentRepository.save(a);
                    }
                    AppointmentResponseDTO dto = this.mapToResponseDTO(a);
                    dto.setBankAccountNumber(a.getBankAccountNumber());
                    return dto;
                })
                .sorted(Comparator.comparing(AppointmentResponseDTO::getDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AppointmentResponseDTO::getAppointmentNumber, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    // =========================================================
    // HOSPITAL GET SINGLE APPOINTMENT REFUND DETAILS
    // =========================================================
    public AppointmentResponseDTO getRefundDetailsForHospital(String appointmentId, String hospitalEmail) {
        if (appointmentId == null || appointmentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Appointment ID is required.");
        }
        if (hospitalEmail == null || hospitalEmail.trim().isEmpty()) {
            throw new SecurityException("Authentication is required.");
        }

        String cleanEmail = hospitalEmail.trim().toLowerCase();
        Hospital hospital = hospitalRepository.findByEmail(cleanEmail)
                .or(() -> hospitalRepository.findByEmail(hospitalEmail.trim()))
                .orElseThrow(() -> new SecurityException("Hospital profile not found."));

        Appointment appt = appointmentRepository.findById(appointmentId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found with id: " + appointmentId));

        if (appt.getHospitalId() == null || !appt.getHospitalId().equals(hospital.getId())) {
            throw new SecurityException("Forbidden: You cannot access refund details belonging to another hospital.");
        }

        // If bank account is saved on the appointment, ensure status is AWAITING_PROCESSING
        if (appt.getBankAccountNumber() != null && !appt.getBankAccountNumber().trim().isEmpty() &&
                (appt.getRefundStatus() == null || "AWAITING_BANK_DETAILS".equalsIgnoreCase(appt.getRefundStatus()))) {
            appt.setRefundStatus("AWAITING_PROCESSING");
            appt = appointmentRepository.save(appt);
        }

        AppointmentResponseDTO dto = this.mapToResponseDTO(appt);
        // Expose full bank account number only to this authorized hospital administrator
        dto.setBankAccountNumber(appt.getBankAccountNumber());
        return dto;
    }

    // =========================================================
    // HOSPITAL CONFIRM REFUND TRANSFER
    // =========================================================
    public Appointment confirmRefundTransfer(String appointmentId, String hospitalEmail, String transactionRef, String notes) {
        if (appointmentId == null || appointmentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Appointment ID is required.");
        }
        if (hospitalEmail == null || hospitalEmail.trim().isEmpty()) {
            throw new SecurityException("Authentication is required.");
        }

        String cleanEmail = hospitalEmail.trim().toLowerCase();
        Hospital hospital = hospitalRepository.findByEmail(cleanEmail)
                .or(() -> hospitalRepository.findByEmail(hospitalEmail.trim()))
                .orElseThrow(() -> new SecurityException("Hospital profile not found."));

        Appointment appt = appointmentRepository.findById(appointmentId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found with id: " + appointmentId));

        if (appt.getHospitalId() == null || !appt.getHospitalId().equals(hospital.getId())) {
            throw new SecurityException("Forbidden: You are not authorized to confirm a refund for another hospital's appointment.");
        }

        if (!"CANCELLED".equalsIgnoreCase(appt.getStatus())) {
            throw new IllegalArgumentException("Cannot complete refund for an appointment that is not cancelled.");
        }

        if ("REFUNDED".equalsIgnoreCase(appt.getRefundStatus())) {
            throw new IllegalStateException("This refund has already been completed and confirmed.");
        }

        if (appt.getRefundAmount() == null || appt.getRefundAmount() <= 0) {
            throw new IllegalArgumentException("No refund is due for this appointment.");
        }

        if (!appt.isPaid() || "PENDING".equalsIgnoreCase(appt.getPaymentStatus()) || "FAILED".equalsIgnoreCase(appt.getPaymentStatus())) {
            throw new IllegalArgumentException("Cannot complete refund for an unpaid appointment.");
        }

        if (appt.getBankAccountNumber() == null || appt.getBankAccountNumber().trim().isEmpty()) {
            throw new IllegalStateException("Patient has not submitted bank details yet.");
        }

        LocalDateTime now = LocalDateTime.now();
        appt.setRefundStatus("REFUNDED");
        appt.setRefundedAt(now);
        String finalRef = (transactionRef != null && !transactionRef.trim().isEmpty())
                ? transactionRef.trim()
                : "TRANSFER-" + System.currentTimeMillis();
        appt.setRefundTransactionRef(finalRef);
        appt.setRefundConfirmedBy(hospital.getName());

        Appointment saved = appointmentRepository.save(appt);

        // In-app notification to Patient
        try {
            if (saved.getPatientId() != null && notificationRepository != null) {
                Notification pNote = new Notification();
                pNote.setUserId(saved.getPatientId());
                pNote.setHospitalId(saved.getHospitalId());
                pNote.setDoctorId(saved.getDoctorId());
                pNote.setScheduleId(saved.getScheduleId());
                pNote.setDate(saved.getDate());
                pNote.setTime(saved.getTime());
                pNote.setTitle("Refund Completed");
                pNote.setMessage("Your refund of LKR " + String.format(Locale.US, "%.2f", saved.getRefundAmount()) + 
                        " for appointment #" + (saved.getAppointmentNumber() != null ? saved.getAppointmentNumber() : saved.getId()) + 
                        " has been completed by " + hospital.getName() + " (Ref: " + finalRef + ").");
                pNote.setCreatedAt(Instant.now());
                pNote.setRead(false);
                notificationRepository.save(pNote);
            }
        } catch (Exception e) {
            System.err.println("Failed to send refund completion notification: " + e.getMessage());
        }

        // Email to patient
        try {
            if (emailService != null && saved.getPatientId() != null) {
                Patient p = patientRepository.findById(saved.getPatientId()).orElse(null);
                if (p != null && p.getEmail() != null) {
                    emailService.sendRefundCompletedEmail(
                            p.getEmail(),
                            p.getFirstName(),
                            saved.getAppointmentNumber() != null ? saved.getAppointmentNumber() : saved.getId(),
                            saved.getRefundAmount() != null ? saved.getRefundAmount() : 0.0,
                            finalRef,
                            hospital.getName()
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to send refund completion email: " + e.getMessage());
        }

        return saved;
    }
}