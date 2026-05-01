package com.example.demo.controller;

import com.example.demo.dto.AppointmentResponseDTO;
import com.example.demo.model.Appointment;
import com.example.demo.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(origins = "http://localhost:5173")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    // Triggered when patient clicks "Book Now"
    @PostMapping("/book")
    public Appointment bookAppointment(@RequestBody Appointment appointment) {
        return appointmentService.bookAppointment(appointment);
    }

    // Triggered on the "My Appointments" page in the patient dashboard
    @GetMapping("/patient/{patientId}")
    public List<Appointment> getPatientAppointments(@PathVariable String patientId) {
        return appointmentService.getAppointmentsForPatient(patientId);
    }

    // ✅ NEW ENDPOINT: Fetch booked numbers for the visual grid
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
}