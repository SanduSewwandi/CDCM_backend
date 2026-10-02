package com.example.demo.controller;

import com.example.demo.model.Appointment;
import com.example.demo.model.Schedule;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.DoctorRepository;
import com.example.demo.repository.ScheduleRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.Notification;
import com.example.demo.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/video-appointments")
@CrossOrigin(
        origins = {
                "http://localhost:5173",
                "https://cdcm-frontend.vercel.app"
        }
)
public class VideoAppointmentPayment {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    // =========================================================
    // CREATE VIDEO APPOINTMENT
    // =========================================================

    @PostMapping("/book")
    public ResponseEntity<?> bookVideoAppointment(
            @RequestBody Map<String, String> req) {

        try {

            String patientId = req.get("patientId");
            String doctorId = req.get("doctorId");
            String scheduleId = req.get("scheduleId");
            String date = req.get("date");
            String time = req.get("time");


            // =================================================
            // VALIDATION
            // =================================================

            if (patientId == null || patientId.isBlank()) {
                return ResponseEntity
                        .badRequest()
                        .body(error("Patient ID is required"));
            }

            if (doctorId == null || doctorId.isBlank()) {
                return ResponseEntity
                        .badRequest()
                        .body(error("Doctor ID is required"));
            }

            if (scheduleId == null || scheduleId.isBlank()) {
                return ResponseEntity
                        .badRequest()
                        .body(error("Schedule ID is required"));
            }

            if (date == null || date.isBlank()) {
                return ResponseEntity
                        .badRequest()
                        .body(error("Date is required"));
            }

            if (time == null || time.isBlank()) {
                return ResponseEntity
                        .badRequest()
                        .body(error("Time is required"));
            }


            // =================================================
            // CHECK EXISTING BOOKING
            // =================================================

            List<Appointment> existingAppointments =
                    appointmentRepository.findByScheduleId(scheduleId);

            if (existingAppointments != null
                    && !existingAppointments.isEmpty()) {

                boolean alreadyBooked =
                        existingAppointments.stream()
                                .anyMatch(appt ->
                                        "PENDING".equalsIgnoreCase(
                                                appt.getStatus()
                                        )
                                                ||
                                                "PAID".equalsIgnoreCase(
                                                        appt.getStatus()
                                                )
                                );

                if (alreadyBooked) {

                    return ResponseEntity
                            .status(HttpStatus.CONFLICT)
                            .body(
                                    error(
                                            "This schedule has already been booked."
                                    )
                            );
                }
            }


            // =================================================
            // CREATE VIDEO APPOINTMENT
            // =================================================

            Appointment appointment = new Appointment();

            appointment.setPatientId(patientId);
            appointment.setDoctorId(doctorId);
            appointment.setScheduleId(scheduleId);
            appointment.setDate(date);
            appointment.setTime(time);

            appointment.setConsultationType("VIDEO");

// Payment has not been completed yet
            appointment.setStatus("PENDING");

// =================================================
// GET MEETING LINK FROM SCHEDULE
// =================================================

            // =================================================
// GET INFORMATION FROM SCHEDULE
// =================================================

            Schedule schedule = scheduleRepository
                    .findById(scheduleId)
                    .orElse(null);

            if (schedule == null) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(error("Video schedule not found"));
            }

// Copy hospital ID and meeting link from schedule
            appointment.setHospitalId(schedule.getHospitalId());
            appointment.setMeetingLink(schedule.getMeetingLink());

            // =================================================
            // SAVE APPOINTMENT
            // =================================================

            Appointment saved =
                    appointmentRepository.save(appointment);


            // =================================================
            // RESPONSE
            // =================================================

            Map<String, Object> response =
                    new HashMap<>();

            response.put(
                    "id",
                    saved.getId()
            );

            response.put(
                    "amount",
                    1000.00
            );

            response.put(
                    "currency",
                    "LKR"
            );

            response.put(
                    "status",
                    "CREATED"
            );

            response.put(
                    "consultationType",
                    "VIDEO"
            );

            response.put(
                    "meetingLink",
                    saved.getMeetingLink()
            );

            response.put(
                    "message",
                    "Video appointment created. Complete payment."
            );


            return ResponseEntity.ok(response);


        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            error(
                                    "Failed to create video appointment"
                            )
                    );
        }
    }


    // =========================================================
    // PAYMENT SUCCESS
    // =========================================================

    @PostMapping("/payment-success/{id}")
    public ResponseEntity<?> paymentSuccess(
            @PathVariable String id) {

        try {

            Appointment appointment =
                    appointmentRepository
                            .findById(id)
                            .orElse(null);


            if (appointment == null) {

                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(
                                error(
                                        "Appointment not found"
                                )
                        );
            }


            // =================================================
            // PREVENT DUPLICATE PAYMENT UPDATE
            // =================================================

            if ("PAID".equalsIgnoreCase(
                    appointment.getStatus())) {

                Map<String, Object> response =
                        new HashMap<>();

                response.put(
                        "message",
                        "Appointment is already paid"
                );

                response.put(
                        "appointmentId",
                        id
                );

                response.put(
                        "status",
                        "PAID"
                );

                return ResponseEntity.ok(response);
            }


            // =================================================
            // MARK AS PAID
            // =================================================

            appointment.setStatus("PAID");
            appointment.setPaymentStatus("PAID");
            appointment.setPaid(true);
            appointment.setPaidAt(LocalDateTime.now());

            // Ensure hospitalId is populated if missing
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

            appointmentRepository.save(
                    appointment
            );

            // Notify Doctor: Video Consultation Booking Notification
            try {
                if (appointment.getDoctorId() != null && notificationRepository != null) {
                    Notification docNote = new Notification();
                    docNote.setUserId(appointment.getDoctorId());
                    docNote.setDoctorId(appointment.getDoctorId());
                    docNote.setScheduleId(appointment.getScheduleId());
                    docNote.setScheduleType("VIDEO");
                    docNote.setDate(appointment.getDate());
                    docNote.setTime(appointment.getTime());
                    docNote.setHospitalId(appointment.getHospitalId());
                    docNote.setTitle("Video Consultation Booking Notification");
                    String timeStr = appointment.getTime() != null && !appointment.getTime().isBlank() ? " at " + appointment.getTime() : "";
                    docNote.setMessage("A patient has successfully booked a video consultation with you for " + appointment.getDate() + timeStr + ".");
                    docNote.setRead(false);
                    docNote.setCreatedAt(LocalDateTime.now());

                    notificationRepository.save(docNote);
                }
            } catch (Exception e) {
                System.err.println("Failed to create doctor video booking notification: " + e.getMessage());
            }

            // Notify Patient: Video Consultation Confirmed
            try {
                if (appointment.getPatientId() != null && notificationRepository != null) {
                    Notification patNote = new Notification();
                    patNote.setUserId(appointment.getPatientId());
                    patNote.setDoctorId(appointment.getDoctorId());
                    patNote.setScheduleId(appointment.getScheduleId());
                    patNote.setScheduleType("VIDEO");
                    patNote.setDate(appointment.getDate());
                    patNote.setTime(appointment.getTime());
                    patNote.setHospitalId(appointment.getHospitalId());
                    patNote.setTitle("Video Consultation Confirmed");
                    String timeSuffix = appointment.getTime() != null && !appointment.getTime().isBlank() ? " at " + appointment.getTime() : "";
                    patNote.setMessage("Payment completed. Your video consultation is confirmed for " + appointment.getDate() + timeSuffix + ". Meeting link is available in your appointments.");
                    patNote.setRead(false);
                    patNote.setCreatedAt(LocalDateTime.now());
                    notificationRepository.save(patNote);
                }
            } catch (Exception e) {
                System.err.println("Failed to create patient video notification: " + e.getMessage());
            }

            // Notify Hospital: Video Consultation Confirmed
            try {
                String targetHospitalId = appointment.getHospitalId();
                if (targetHospitalId != null && !targetHospitalId.trim().isEmpty() && !"null".equalsIgnoreCase(targetHospitalId) && notificationRepository != null) {
                    Notification hospNote = new Notification();
                    hospNote.setUserId(targetHospitalId);
                    hospNote.setHospitalId(targetHospitalId);
                    hospNote.setDoctorId(appointment.getDoctorId());
                    hospNote.setScheduleId(appointment.getScheduleId());
                    hospNote.setScheduleType("VIDEO");
                    hospNote.setDate(appointment.getDate());
                    hospNote.setTime(appointment.getTime());
                    hospNote.setTitle("Video Appointment Confirmed");
                    hospNote.setMessage("Payment confirmed for video appointment on " + appointment.getDate() + ".");
                    hospNote.setRead(false);
                    hospNote.setCreatedAt(LocalDateTime.now());
                    notificationRepository.save(hospNote);
                    System.out.println("Video payment notification saved for hospital: " + targetHospitalId);
                }
            } catch (Exception e) {
                System.err.println("Failed to create hospital video notification: " + e.getMessage());
            }



            Map<String, Object> response =
                    new HashMap<>();

            response.put(
                    "message",
                    "Payment completed successfully"
            );

            response.put(
                    "appointmentId",
                    id
            );

            response.put(
                    "status",
                    "PAID"
            );

            response.put(
                    "consultationType",
                    appointment.getConsultationType()
            );

            response.put(
                    "meetingLink",
                    appointment.getMeetingLink()
            );

            return ResponseEntity.ok(response);


        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            error(
                                    "Failed to update payment status"
                            )
                    );
        }
    }


    // =========================================================
    // PAYMENT FAILED
    // =========================================================

    @PostMapping("/payment-failed/{id}")
    public ResponseEntity<?> paymentFailed(
            @PathVariable String id) {

        try {

            Appointment appointment =
                    appointmentRepository
                            .findById(id)
                            .orElse(null);


            if (appointment == null) {

                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(
                                error(
                                        "Appointment not found"
                                )
                        );
            }


            // =================================================
            // DON'T CHANGE SUCCESSFUL PAYMENT TO FAILED
            // =================================================

            if ("PAID".equalsIgnoreCase(
                    appointment.getStatus())) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                error(
                                        "This appointment is already paid."
                                )
                        );
            }


            // =================================================
            // MARK PAYMENT AS FAILED
            // =================================================

            appointment.setStatus("FAILED");

            appointmentRepository.save(
                    appointment
            );


            Map<String, Object> response =
                    new HashMap<>();

            response.put(
                    "message",
                    "Payment failed"
            );

            response.put(
                    "appointmentId",
                    id
            );

            response.put(
                    "status",
                    "FAILED"
            );


            return ResponseEntity.ok(response);


        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            error(
                                    "Failed to update payment failure"
                            )
                    );
        }
    }


    // =========================================================
    // GET PATIENT VIDEO APPOINTMENTS
    // =========================================================

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<?> getPatientAppointments(
            @PathVariable String patientId) {

        try {

            List<Appointment> appointments =
                    appointmentRepository
                            .findByPatientId(patientId)
                            .stream()
                            .filter(appointment ->
                                    "VIDEO".equalsIgnoreCase(
                                            appointment.getConsultationType()
                                    )
                            )
                            .toList();


            return ResponseEntity.ok(
                    appointments
            );


        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            error(
                                    "Failed to load video appointments"
                            )
                    );
        }
    }


    // =========================================================
    // ERROR RESPONSE
    // =========================================================

    private Map<String, Object> error(
            String message) {

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "message",
                message
        );

        return response;
    }
}
