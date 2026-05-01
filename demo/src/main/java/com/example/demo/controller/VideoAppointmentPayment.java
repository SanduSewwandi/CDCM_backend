package com.example.demo.controller;

import com.example.demo.model.Appointment;
import com.example.demo.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/video-appointments")
@CrossOrigin("*")
public class VideoAppointmentPayment {

    @Autowired
    private AppointmentRepository appointmentRepository;

    // ================= CREATE VIDEO APPOINTMENT =================
    @PostMapping("/book")
    public Map<String, Object> bookVideoAppointment(@RequestBody Map<String, String> req) {

        Appointment appt = new Appointment();
        appt.setPatientId(req.get("patientId"));
        appt.setDoctorId(req.get("doctorId"));
        appt.setScheduleId(req.get("scheduleId"));
        appt.setDate(req.get("date"));
        appt.setTime(req.get("time"));
        appt.setStatus("PENDING");

        Appointment saved = appointmentRepository.save(appt);

        Map<String, Object> response = new HashMap<>();
        response.put("id", saved.getId());
        response.put("amount", 1000.00);
        response.put("status", "CREATED");

        return response;
    }

    // ================= PAYMENT SUCCESS =================
    @PostMapping("/payment-success/{id}")
    public Map<String, Object> paymentSuccess(@PathVariable String id) {

        Appointment appt = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        appt.setStatus("PAID");
        appointmentRepository.save(appt);

        Map<String, Object> res = new HashMap<>();
        res.put("message", "Payment completed");
        res.put("appointmentId", id);

        return res;
    }

    // ================= PAYMENT FAILED =================
    @PostMapping("/payment-failed/{id}")
    public Map<String, Object> paymentFailed(@PathVariable String id) {

        Appointment appt = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        appt.setStatus("FAILED");
        appointmentRepository.save(appt);

        Map<String, Object> res = new HashMap<>();
        res.put("message", "Payment failed");
        res.put("appointmentId", id);

        return res;
    }
}