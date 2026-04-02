package com.example.demo.controller;

import com.example.demo.dto.LabTestRequest;
import com.example.demo.dto.ReportUploadRequest;
import com.example.demo.model.LabTest;
import com.example.demo.model.Patient;
import com.example.demo.model.Notification;
import com.example.demo.repository.LabTestRepository;
import com.example.demo.repository.PatientRepository;
import com.example.demo.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/lab")
@CrossOrigin(origins = "*")
public class LabController {

    private final LabTestRepository labTestRepository;
    private final PatientRepository patientRepository;
    private final NotificationRepository notificationRepository;

    public LabController(LabTestRepository labTestRepository,
                         PatientRepository patientRepository,
                         NotificationRepository notificationRepository) {
        this.labTestRepository = labTestRepository;
        this.patientRepository = patientRepository;
        this.notificationRepository = notificationRepository;
    }

    /** ---------------- PATIENTS ---------------- */
    @GetMapping("/patients")
    public List<Patient> getPatients() {
        return patientRepository.findAll();
    }

    /** ---------------- ADD LAB TEST ---------------- */
    @PostMapping("/add")
    public LabTest addLabTest(@RequestBody LabTestRequest request) {
        LabTest test = new LabTest();
        test.setPatientId(request.getPatientId());
        test.setTestType(request.getTestType());
        test.setPrice(request.getPrice());
        test.setTestDate(request.getTestDate());
        test.setRequestedDate(request.getRequestedDate());
        test.setStatus("Pending");
        test.setReportStatus("Pending");

        LabTest savedTest = labTestRepository.save(test);

        notifyPatient(test.getPatientId(), "New lab test assigned: " + test.getTestType());

        return savedTest;
    }

    /** ---------------- GET ALL TESTS ---------------- */
    @GetMapping("/all")
    public List<LabTest> getAllTests() {
        return labTestRepository.findAll();
    }

    /** ---------------- GET SINGLE TEST ---------------- */
    @GetMapping("/{id}")
    public LabTest getTest(@PathVariable String id) {
        return labTestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab test not found"));
    }

    /** ---------------- UPDATE TEST ---------------- */
    @PutMapping("/update/{id}")
    public LabTest updateTest(@PathVariable String id,
                              @RequestBody LabTestRequest request) {
        LabTest test = labTestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab test not found"));

        if (request.getTestType() != null) test.setTestType(request.getTestType());
        if (request.getTestDate() != null) test.setTestDate(request.getTestDate());
        if (request.getRequestedDate() != null) test.setRequestedDate(request.getRequestedDate());

        if (request.getStatus() != null) {
            validateAndSetStatus(test, request.getStatus());
        }

        LabTest updated = labTestRepository.save(test);

        notifyPatient(test.getPatientId(), "Lab test status updated to: " + test.getStatus());

        return updated;
    }

    /** ---------------- PAY FOR TEST ---------------- */
    @PostMapping("/pay/{id}")
    public String payForTest(@PathVariable String id) {
        LabTest test = labTestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab test not found"));

        test.setPaid(true);
        test.setPaidAt(LocalDateTime.now());

        labTestRepository.save(test);

        return "Payment successful";
    }

    /** ---------------- DELETE TEST ---------------- */
    @DeleteMapping("/delete/{id}")
    public void deleteTest(@PathVariable String id) {
        labTestRepository.deleteById(id);
    }

    /** ---------------- UPLOAD REPORT ---------------- */
    @PutMapping("/upload-report/{id}")
    public LabTest uploadReport(@PathVariable String id,
                                @RequestBody ReportUploadRequest request) {
        LabTest test = labTestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab test not found"));

        test.setReportUrl(request.getReportUrl());
        test.setReportText(request.getReportText());
        test.setReportStatus("Uploaded");
        test.setStatus("Completed");

        LabTest savedTest = labTestRepository.save(test);

        notifyPatient(test.getPatientId(), "Your lab report is ready for: " + test.getTestType());

        return savedTest;
    }

    /** ---------------- HELPER METHODS ---------------- */
    private void notifyPatient(String patientId, String message) {
        try {
            if (patientId == null || patientId.isEmpty()) {
                System.err.println("Notification Error: patientId is null. Message: " + message);
                return;
            }

            Notification n = new Notification();
            n.setUserId(patientId);
            n.setMessage(message);
            n.setRead(false);
            n.setCreatedAt(LocalDateTime.now());

            notificationRepository.save(n);
            System.out.println("Notification successfully saved for: " + patientId);
        } catch (Exception e) {
            // Log the error but don't crash the Lab Test creation
            System.err.println("Failed to save notification: " + e.getMessage());
        }
    }

    private void validateAndSetStatus(LabTest test, String nextStatus) {
        String current = test.getStatus();

        if ("Pending".equals(current) && "In Progress".equals(nextStatus)) {
            test.setStatus("In Progress");
        } else if ("In Progress".equals(current) && "Completed".equals(nextStatus)) {
            if (test.getReportUrl() == null && test.getReportText() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot mark Completed without report findings.");
            }
            test.setStatus("Completed");
        } else if (!current.equals(nextStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid status transition from " + current + " to " + nextStatus);
        }
    }
}