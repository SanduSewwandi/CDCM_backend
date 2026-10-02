package com.example.demo.controller;

import com.example.demo.model.PatientMedicalRecord;
import com.example.demo.service.PatientMedicalRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/medical-records")
@CrossOrigin(
        origins = {
                "http://localhost:5173",
                "https://cdcm-frontend.vercel.app"
        }
)
public class PatientMedicalRecordController {

    @Autowired
    private PatientMedicalRecordService service;

    @PostMapping("/add")
    public PatientMedicalRecord addRecord(@RequestBody PatientMedicalRecord record) {
        return service.saveMedicalRecord(record);
    }

    // Existing patient self-view endpoint - PRESERVED UNCHANGED for patient portal
    @GetMapping("/patient/{patientId}")
    public List<PatientMedicalRecord> getHistory(@PathVariable String patientId) {
        return service.getHistoryByPatient(patientId);
    }

    // OTP verification endpoint for doctor consultation
    @PostMapping("/verify-access")
    public ResponseEntity<?> verifyAccess(@RequestBody Map<String, String> request) {
        String appointmentId = request != null ? request.get("appointmentId") : null;
        String doctorId = request != null ? request.get("doctorId") : null;
        String otp = request != null ? request.get("otp") : null;

        Map<String, Object> result = service.verifyAccessOtp(appointmentId, doctorId, otp);
        int status = (int) result.getOrDefault("status", 200);
        result.remove("status");
        return ResponseEntity.status(status).body(result);
    }

    // Access status endpoint for checking if 1-hour window is active
    @GetMapping("/access-status")
    public ResponseEntity<?> getAccessStatus(
            @RequestParam String appointmentId,
            @RequestParam String doctorId) {

        Map<String, Object> result = service.getAccessStatus(appointmentId, doctorId);
        int status = (int) result.getOrDefault("status", 200);
        result.remove("status");
        return ResponseEntity.status(status).body(result);
    }

    // Dedicated doctor-specific medical history endpoint enforcing the 1-hour access window server-side
    @GetMapping("/doctor/patient/{patientId}")
    public ResponseEntity<?> getDoctorPatientHistory(
            @PathVariable String patientId,
            @RequestParam String appointmentId,
            @RequestParam String doctorId) {

        Map<String, Object> validation = service.validateDoctorAccess(patientId, appointmentId, doctorId);
        int status = (int) validation.getOrDefault("status", 200);
        if (status != 200) {
            validation.remove("status");
            return ResponseEntity.status(status).body(validation);
        }

        List<PatientMedicalRecord> history = service.getHistoryByPatient(patientId);
        return ResponseEntity.ok(history);
    }
}