 package com.example.demo.controller;

import com.example.demo.model.Appointment;
import com.example.demo.repository.AppointmentRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.Schedule;
import com.example.demo.repository.ScheduleRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/video-appointments")
@CrossOrigin(origins = "http://localhost:5173")
public class VideoAppointmentPayment {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

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

            appointmentRepository.save(
                    appointment
            );


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
