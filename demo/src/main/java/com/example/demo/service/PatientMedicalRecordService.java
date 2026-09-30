package com.example.demo.service;

import com.example.demo.model.Appointment;
import com.example.demo.model.PatientMedicalRecord;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.PatientMedicalRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PatientMedicalRecordService {

    @Autowired
    private PatientMedicalRecordRepository repository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public PatientMedicalRecord saveMedicalRecord(PatientMedicalRecord record) {
        // Automatically set the date of visit to today if empty
        if (record.getDateOfVisit() == null || record.getDateOfVisit().isEmpty()) {
            record.setDateOfVisit(LocalDate.now().toString());
        }
        return repository.save(record);
    }

    public List<PatientMedicalRecord> getHistoryByPatient(String patientId) {
        return repository.findByPatientIdOrderByDateOfVisitDesc(patientId);
    }

    public Map<String, Object> verifyAccessOtp(String appointmentId, String doctorId, String otp) {
        Map<String, Object> response = new HashMap<>();

        if (appointmentId == null || appointmentId.isBlank()
                || doctorId == null || doctorId.isBlank()
                || otp == null || otp.isBlank()) {
            response.put("status", 400);
            response.put("success", false);
            response.put("message", "appointmentId, doctorId, and otp are required.");
            return response;
        }

        Optional<Appointment> apptOpt = appointmentRepository.findById(appointmentId.trim());
        if (apptOpt.isEmpty()) {
            response.put("status", 404);
            response.put("success", false);
            response.put("message", "Appointment not found.");
            return response;
        }

        Appointment appt = apptOpt.get();

        // 1. Check doctor authorization
        if (appt.getDoctorId() == null || !appt.getDoctorId().equals(doctorId.trim())) {
            response.put("status", 403);
            response.put("success", false);
            response.put("message", "Doctor is not authorized for this appointment.");
            return response;
        }

        // 2. Check patientId
        if (appt.getPatientId() == null || appt.getPatientId().trim().isEmpty()) {
            response.put("status", 400);
            response.put("success", false);
            response.put("message", "Invalid appointment: patient ID is missing.");
            return response;
        }

        // 3. Check payment status
        if (!appt.isPaid() || !"PAID".equalsIgnoreCase(appt.getPaymentStatus())) {
            response.put("status", 403);
            response.put("success", false);
            response.put("message", "Appointment is not paid. Medical history access requires a confirmed and paid appointment.");
            return response;
        }

        LocalDateTime now = LocalDateTime.now();

        // 4. Repeated verification / active access: Do NOT extend or re-grant 1-hour timer
        if (appt.getMedicalHistoryAccessExpiresAt() != null && now.isBefore(appt.getMedicalHistoryAccessExpiresAt())) {
            response.put("status", 200);
            response.put("success", true);
            response.put("hasActiveAccess", true);
            response.put("message", "Medical history access is already active.");
            response.put("accessExpiresAt", appt.getMedicalHistoryAccessExpiresAt());
            return response;
        }

        // 5. Expired consultation access must remain expired
        if (appt.getMedicalHistoryAccessGrantedAt() != null
                && appt.getMedicalHistoryAccessExpiresAt() != null
                && !now.isBefore(appt.getMedicalHistoryAccessExpiresAt())) {
            response.put("status", 403);
            response.put("success", false);
            response.put("hasActiveAccess", false);
            response.put("message", "The 1-hour medical history access window for this consultation has expired.");
            return response;
        }

        // 6. Check if OTP was issued
        if (appt.getMedicalHistoryOtpHash() == null || appt.getMedicalHistoryOtpHash().trim().isEmpty()) {
            response.put("status", 400);
            response.put("success", false);
            response.put("message", "No medical history OTP has been issued for this appointment.");
            return response;
        }

        // 7. Check if OTP has explicitly expired
        if (appt.getMedicalHistoryOtpExpiresAt() != null && now.isAfter(appt.getMedicalHistoryOtpExpiresAt())) {
            response.put("status", 403);
            response.put("success", false);
            response.put("message", "Medical history OTP has expired.");
            return response;
        }

        // 8. Basic failed-attempt protection (limit 5 attempts)
        int failedAttempts = appt.getMedicalHistoryOtpFailedAttempts();
        if (failedAttempts >= 5) {
            response.put("status", 429);
            response.put("success", false);
            response.put("message", "Too many failed OTP attempts. Access has been locked for this appointment.");
            return response;
        }

        // 9. Verify entered OTP against stored hash
        boolean matches = passwordEncoder.matches(otp.trim(), appt.getMedicalHistoryOtpHash());
        if (!matches) {
            failedAttempts++;
            appt.setMedicalHistoryOtpFailedAttempts(failedAttempts);
            appointmentRepository.save(appt);
            int remaining = Math.max(0, 5 - failedAttempts);
            response.put("status", 401);
            response.put("success", false);
            response.put("hasActiveAccess", false);
            response.put("message", "Invalid OTP. Attempts remaining: " + remaining);
            return response;
        }

        // 10. First successful verification: Grant 1-hour access window
        appt.setMedicalHistoryAccessGrantedAt(now);
        appt.setMedicalHistoryAccessExpiresAt(now.plusHours(1));
        appt.setMedicalHistoryOtpFailedAttempts(0);
        appointmentRepository.save(appt);

        response.put("status", 200);
        response.put("success", true);
        response.put("hasActiveAccess", true);
        response.put("message", "Medical history access granted successfully for 1 hour.");
        response.put("accessExpiresAt", appt.getMedicalHistoryAccessExpiresAt());
        return response;
    }

    public Map<String, Object> getAccessStatus(String appointmentId, String doctorId) {
        Map<String, Object> response = new HashMap<>();

        if (appointmentId == null || appointmentId.isBlank() || doctorId == null || doctorId.isBlank()) {
            response.put("status", 400);
            response.put("message", "appointmentId and doctorId are required.");
            return response;
        }

        Optional<Appointment> apptOpt = appointmentRepository.findById(appointmentId.trim());
        if (apptOpt.isEmpty()) {
            response.put("status", 404);
            response.put("message", "Appointment not found.");
            return response;
        }

        Appointment appt = apptOpt.get();

        if (appt.getDoctorId() == null || !appt.getDoctorId().equals(doctorId.trim())) {
            response.put("status", 403);
            response.put("message", "Doctor is not authorized for this appointment.");
            return response;
        }

        LocalDateTime now = LocalDateTime.now();
        boolean isPaid = appt.isPaid() && "PAID".equalsIgnoreCase(appt.getPaymentStatus());
        boolean otpIssued = appt.getMedicalHistoryOtpHash() != null && !appt.getMedicalHistoryOtpHash().trim().isEmpty();
        boolean hasActiveAccess = appt.getMedicalHistoryAccessExpiresAt() != null && now.isBefore(appt.getMedicalHistoryAccessExpiresAt());
        boolean accessExpired = appt.getMedicalHistoryAccessGrantedAt() != null && !hasActiveAccess;
        boolean otpExpired = appt.getMedicalHistoryOtpExpiresAt() != null && now.isAfter(appt.getMedicalHistoryOtpExpiresAt());
        boolean locked = appt.getMedicalHistoryOtpFailedAttempts() >= 5;

        response.put("status", 200);
        response.put("appointmentId", appt.getId());
        response.put("doctorId", appt.getDoctorId());
        response.put("patientId", appt.getPatientId());
        response.put("isPaid", isPaid);
        response.put("otpIssued", otpIssued);
        response.put("hasActiveAccess", hasActiveAccess);
        response.put("accessGrantedAt", appt.getMedicalHistoryAccessGrantedAt());
        response.put("accessExpiresAt", appt.getMedicalHistoryAccessExpiresAt());
        response.put("accessExpired", accessExpired);
        response.put("otpExpired", otpExpired);
        response.put("locked", locked);

        return response;
    }

    public Map<String, Object> validateDoctorAccess(String patientId, String appointmentId, String doctorId) {
        Map<String, Object> response = new HashMap<>();

        if (patientId == null || patientId.isBlank()
                || appointmentId == null || appointmentId.isBlank()
                || doctorId == null || doctorId.isBlank()) {
            response.put("status", 400);
            response.put("error", "patientId, appointmentId, and doctorId parameters are required.");
            return response;
        }

        Optional<Appointment> apptOpt = appointmentRepository.findById(appointmentId.trim());
        if (apptOpt.isEmpty()) {
            response.put("status", 404);
            response.put("error", "Appointment not found.");
            return response;
        }

        Appointment appt = apptOpt.get();

        if (appt.getDoctorId() == null || !appt.getDoctorId().equals(doctorId.trim())) {
            response.put("status", 403);
            response.put("error", "Doctor is not authorized for this appointment.");
            return response;
        }

        if (appt.getPatientId() == null || !appt.getPatientId().equals(patientId.trim())) {
            response.put("status", 403);
            response.put("error", "Appointment does not belong to the requested patient.");
            return response;
        }

        if (!appt.isPaid() || !"PAID".equalsIgnoreCase(appt.getPaymentStatus())) {
            response.put("status", 403);
            response.put("error", "Appointment is not paid. Medical history access requires a confirmed and paid appointment.");
            return response;
        }

        LocalDateTime now = LocalDateTime.now();
        if (appt.getMedicalHistoryAccessExpiresAt() == null || !now.isBefore(appt.getMedicalHistoryAccessExpiresAt())) {
            response.put("status", 403);
            response.put("error", "Medical history access is expired or not authorized. Please verify the patient OTP.");
            return response;
        }

        response.put("status", 200);
        return response;
    }
}