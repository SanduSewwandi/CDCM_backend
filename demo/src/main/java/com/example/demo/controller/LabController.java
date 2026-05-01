package com.example.demo.controller;

import com.example.demo.dto.LabTestRequest;
import com.example.demo.dto.ReportUploadRequest;

import com.example.demo.model.LabTest;
import com.example.demo.model.Patient;
import com.example.demo.model.Notification;

import com.example.demo.repository.LabTestRepository;
import com.example.demo.repository.PatientRepository;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.service.FileUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;


import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lab")
@CrossOrigin(origins = "*")
public class LabController {

    private final LabTestRepository labTestRepository;
    private final PatientRepository patientRepository;
    private final NotificationRepository notificationRepository;
    private final FileUploadService fileUploadService;

    public LabController(LabTestRepository labTestRepository,
                         PatientRepository patientRepository,
                         NotificationRepository notificationRepository,
                         FileUploadService fileUploadService) {
        this.labTestRepository = labTestRepository;
        this.patientRepository = patientRepository;
        this.notificationRepository = notificationRepository;
        this.fileUploadService = fileUploadService;
    }

    /** patients */
    @GetMapping("/patients")
    public List<Patient> getPatients() {
        return patientRepository.findAll();
    }

    /** add lab test */
    @PostMapping("/add")
    public LabTest addLabTest(@RequestBody LabTestRequest request) {
        LabTest test = new LabTest();  // createdAt and updatedAt are set in constructor
        test.setPatientId(request.getPatientId());
        test.setHospitalId(request.getHospitalId());
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

    /** get all tests */
    @GetMapping("/all")
    public List<LabTest> getAllTests() {
        return labTestRepository.findAll();
    }

    /** get test by patient */
    @GetMapping("/patient/{patientId}")
    public List<LabTest> getTestsByPatient(@PathVariable String patientId) {
        return labTestRepository.findByPatientId(patientId);
    }

    /** get single test */
    @GetMapping("/{id}")
    public LabTest getTest(@PathVariable String id) {
        return labTestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab test not found"));
    }

    /** update test */
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

    /** pay for test */
    @PostMapping("/pay/{id}")
    public LabTest payForTest(@PathVariable String id) {
        LabTest test = labTestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab test not found"));

        test.setPaid(true);
        test.setPaidAt(LocalDateTime.now());

        LabTest savedTest = labTestRepository.save(test);

        // ---- SEND HOSPITAL NOTIFICATION ----
        if (test.getHospitalId() != null && !test.getHospitalId().isEmpty()) {
            Notification hospitalNotification = new Notification();
            hospitalNotification.setUserId(test.getHospitalId()); // hospital gets the notification
            hospitalNotification.setMessage(
                    "Patient " + test.getPatientId() + " has paid for " + test.getTestType() + " (Rs " + test.getPrice() + ")"
            );
            hospitalNotification.setRead(false);
            hospitalNotification.setCreatedAt(LocalDateTime.now());

            notificationRepository.save(hospitalNotification);
            System.out.println("Hospital notification saved for: " + test.getHospitalId());
        } else {
            System.err.println("Hospital ID missing → cannot send notification");
        }

        return savedTest;
    }
    /** delete test */
    @DeleteMapping("/delete/{id}")
    public void deleteTest(@PathVariable String id) {
        labTestRepository.deleteById(id);
    }

    /** upload report with file */
    @PostMapping("/upload-report-with-file/{id}")
    public ResponseEntity<?> uploadReportWithFile(
            @PathVariable String id,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "reportText", required = false) String reportText) {

        try {
            LabTest test = labTestRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab test not found"));

            // BLOCK if not paid
            if (!test.isPaid()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "success", false,
                        "message", "Payment required before uploading report"
                ));
            }

            String fileUrl = null;

            // Upload file to Cloudinary if provided
            if (file != null && !file.isEmpty()) {
                System.out.println("Uploading file: " + file.getOriginalFilename());
                fileUrl = fileUploadService.uploadFile(file);
                test.setReportUrl(fileUrl);
                System.out.println("File uploaded to: " + fileUrl);
            }

            // Set report text if provided
            if (reportText != null && !reportText.trim().isEmpty()) {
                test.setReportText(reportText);
            }

            test.setReportStatus("Uploaded");
            test.setStatus("Completed");

            LabTest savedTest = labTestRepository.save(test);
            System.out.println("Report URL saved: " + savedTest.getReportUrl());

            // Send notification
            notifyPatient(test.getPatientId(),
                    "Your lab report is ready for: " + test.getTestType());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", savedTest);
            response.put("message", "Report uploaded successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Upload error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Upload failed: " + e.getMessage()
            ));
        }
    }


    @PutMapping("/upload-report/{id}")
    public LabTest uploadReport(@PathVariable String id,
                                @RequestBody ReportUploadRequest request) {

        LabTest test = labTestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab test not found"));

        // BLOCK if not paid
        if (!test.isPaid()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Payment required before uploading report");
        }

        test.setReportUrl(request.getReportUrl());
        test.setReportText(request.getReportText());
        test.setReportStatus("Uploaded");
        test.setStatus("Completed");

        LabTest savedTest = labTestRepository.save(test);

        notifyPatient(test.getPatientId(),
                "Your lab report is ready for: " + test.getTestType());

        return savedTest;
    }

    @GetMapping("/download-report/{id}")
    public ResponseEntity<?> downloadReport(@PathVariable String id) {
        try {
            LabTest test = labTestRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab test not found"));

            if (test.getReportUrl() == null || test.getReportUrl().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "No report uploaded"));
            }

            Map<String, String> response = new HashMap<>();
            response.put("url", test.getReportUrl());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
    /** helper methods */
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