package com.example.demo.controller;

import com.example.demo.dto.AppointmentCancelRequest;
import com.example.demo.dto.AppointmentResponseDTO;
import com.example.demo.dto.BankDetailsRequest;
import com.example.demo.dto.RefundConfirmRequest;
import com.example.demo.dto.RefundPreviewDTO;
import com.example.demo.model.Appointment;
import com.example.demo.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(
        origins = {
                "http://localhost:5173",
                "https://cdcm-frontend.vercel.app"
        }
)
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    // Triggered when patient clicks "Book Now"
    @PostMapping("/book")
    public Appointment bookAppointment(@RequestBody Appointment appointment) {
        return appointmentService.bookAppointment(appointment);
    }

    @PostMapping("/auto-assign/{hospitalId}")
    public List<Appointment> autoAssignNumbers(
            @PathVariable String hospitalId,
            @RequestParam String date
    ) {
        return appointmentService.autoAssignNumbers(hospitalId, date);
    }

    // Triggered on the "My Appointments" page in the patient dashboard
    @GetMapping("/patient/{patientId}")
    public List<Appointment> getPatientAppointments(@PathVariable String patientId) {
        return appointmentService.getAppointmentsForPatient(patientId);
    }

    //  Fetch booked numbers for the visual grid
    @GetMapping("/schedule/{scheduleId}")
    public List<Appointment> getScheduleAppointments(@PathVariable String scheduleId) {
        return appointmentService.getAppointmentsBySchedule(scheduleId);
    }

    @GetMapping("/doctor/{doctorId}")
    public List<AppointmentResponseDTO> getDoctorAppointments(@PathVariable String doctorId) {
        return appointmentService.getAppointmentsByDoctor(doctorId);
    }

    @GetMapping("/hospital/{hospitalId}")
    public List<AppointmentResponseDTO> getHospitalAppointments(@PathVariable String hospitalId) {
        return appointmentService.getAppointmentsByHospital(hospitalId);
    }

    // Explicitly release / cancel a pending appointment that failed or was dismissed
    @PutMapping("/cancel-pending/{appointmentId}")
    public Appointment cancelPendingAppointmentPut(@PathVariable String appointmentId) {
        return appointmentService.cancelPendingAppointment(appointmentId);
    }

    @PostMapping("/cancel-pending/{appointmentId}")
    public Appointment cancelPendingAppointmentPost(@PathVariable String appointmentId) {
        return appointmentService.cancelPendingAppointment(appointmentId);
    }

    // =========================================================
    // CANCELLATION REFUND PREVIEW (Patient)
    // =========================================================
    @GetMapping("/{appointmentId}/cancel-preview")
    public ResponseEntity<?> getCancelPreview(
            @PathVariable String appointmentId,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Authentication required"));
        }
        try {
            RefundPreviewDTO preview = appointmentService.getCancellationRefundPreview(appointmentId, principal.getName());
            return ResponseEntity.ok(preview);
        } catch (SecurityException se) {
            return ResponseEntity.status(403).body(Map.of("message", se.getMessage()));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(Map.of("message", iae.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    // =========================================================
    // CANCEL APPOINTMENT (Patient)
    // =========================================================
    @PutMapping("/{appointmentId}/cancel")
    public ResponseEntity<?> cancelAppointmentByPatient(
            @PathVariable String appointmentId,
            @RequestBody(required = false) AppointmentCancelRequest request,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Authentication required"));
        }
        try {
            String reason = request != null ? request.getReason() : null;
            Appointment cancelled = appointmentService.cancelAppointmentByPatient(appointmentId, principal.getName(), reason);
            return ResponseEntity.ok(cancelled);
        } catch (SecurityException se) {
            return ResponseEntity.status(403).body(Map.of("message", se.getMessage()));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(Map.of("message", iae.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    // =========================================================
    // CANCEL APPOINTMENT (Hospital)
    // =========================================================
    @PutMapping("/{appointmentId}/hospital-cancel")
    public ResponseEntity<?> cancelAppointmentByHospital(
            @PathVariable String appointmentId,
            @RequestBody(required = false) AppointmentCancelRequest request,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Authentication required"));
        }
        try {
            String reason = request != null ? request.getReason() : null;
            Appointment cancelled = appointmentService.cancelAppointmentByHospital(appointmentId, principal.getName(), reason);
            return ResponseEntity.ok(cancelled);
        } catch (SecurityException se) {
            return ResponseEntity.status(403).body(Map.of("message", se.getMessage()));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(Map.of("message", iae.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    // =========================================================
    // SUBMIT BANK DETAILS FOR REFUND (Patient)
    // =========================================================
    @PostMapping("/{appointmentId}/bank-details")
    public ResponseEntity<?> submitBankDetails(
            @PathVariable String appointmentId,
            @RequestBody BankDetailsRequest request,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Authentication required"));
        }
        try {
            Appointment updated = appointmentService.submitBankDetails(appointmentId, principal.getName(), request);
            return ResponseEntity.ok(updated);
        } catch (SecurityException se) {
            return ResponseEntity.status(403).body(Map.of("message", se.getMessage()));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(Map.of("message", iae.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    // =========================================================
    // GET PENDING REFUNDS (Hospital)
    // =========================================================
    @GetMapping("/hospital/{hospitalId}/pending-refunds")
    public ResponseEntity<?> getPendingRefunds(
            @PathVariable String hospitalId,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Authentication required"));
        }
        try {
            List<AppointmentResponseDTO> pending = appointmentService.getPendingRefundsForHospital(hospitalId, principal.getName());
            return ResponseEntity.ok(pending);
        } catch (SecurityException se) {
            return ResponseEntity.status(403).body(Map.of("message", se.getMessage()));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(Map.of("message", iae.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    // =========================================================
    // GET APPOINTMENT REFUND DETAILS (Hospital)
    // =========================================================
    @GetMapping("/{appointmentId}/refund-details")
    public ResponseEntity<?> getRefundDetails(
            @PathVariable String appointmentId,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Authentication required"));
        }
        try {
            AppointmentResponseDTO details = appointmentService.getRefundDetailsForHospital(appointmentId, principal.getName());
            return ResponseEntity.ok(details);
        } catch (SecurityException se) {
            return ResponseEntity.status(403).body(Map.of("message", se.getMessage()));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(404).body(Map.of("message", iae.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    // =========================================================
    // CONFIRM REFUND TRANSFER (Hospital)
    // =========================================================
    @PutMapping("/{appointmentId}/confirm-refund")
    public ResponseEntity<?> confirmRefund(
            @PathVariable String appointmentId,
            @RequestBody(required = false) RefundConfirmRequest request,
            Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Authentication required"));
        }
        try {
            String ref = request != null ? request.getTransactionRef() : null;
            String notes = request != null ? request.getNotes() : null;
            Appointment updated = appointmentService.confirmRefundTransfer(appointmentId, principal.getName(), ref, notes);
            return ResponseEntity.ok(updated);
        } catch (SecurityException se) {
            return ResponseEntity.status(403).body(Map.of("message", se.getMessage()));
        } catch (IllegalStateException ise) {
            return ResponseEntity.status(409).body(Map.of("message", ise.getMessage()));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(Map.of("message", iae.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }
}