package com.example.demo.controller;

import com.example.demo.dto.LabTestRequest;
import com.example.demo.dto.ReportUploadRequest;
import com.example.demo.model.LabTest;
import com.example.demo.model.Patient;
import com.example.demo.repository.LabTestRepository;
import com.example.demo.repository.PatientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lab")
@CrossOrigin(origins = "*")
public class LabController {

    private final LabTestRepository labTestRepository;
    private final PatientRepository patientRepository;

    public LabController(LabTestRepository labTestRepository,
                         PatientRepository patientRepository) {
        this.labTestRepository = labTestRepository;
        this.patientRepository = patientRepository;
    }

    @GetMapping("/patients")
    public List<Patient> getPatients() {
        return patientRepository.findAll();
    }

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

        return labTestRepository.save(test);
    }

    @GetMapping("/all")
    public List<LabTest> getAllTests() {
        return labTestRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTest(@PathVariable String id) {
        return labTestRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // update test(General updates & manual status changes)
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateTest(@PathVariable String id,
                                        @RequestBody LabTestRequest request) {
        try {
            LabTest test = labTestRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Test not found"));

            if (request.getTestType() != null) test.setTestType(request.getTestType());
            if (request.getTestDate() != null) test.setTestDate(request.getTestDate());
            if (request.getRequestedDate() != null) test.setRequestedDate(request.getRequestedDate());

            if (request.getStatus() != null) {
                String current = test.getStatus();
                String next = request.getStatus();

                if ("Pending".equals(current) && "In Progress".equals(next)) {
                    test.setStatus("In Progress");
                } else if ("In Progress".equals(current) && "Completed".equals(next)) {
                    if (test.getReportUrl() == null && test.getReportText() == null) {
                        return ResponseEntity.badRequest().body("Cannot mark Completed without report findings.");
                    }
                    test.setStatus("Completed");
                } else if (!current.equals(next)) {
                    return ResponseEntity.badRequest().body("Invalid status transition from " + current + " to " + next);
                }
            }

            return ResponseEntity.ok(labTestRepository.save(test));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // delete test
    @DeleteMapping("/delete/{id}")
    public void deleteTest(@PathVariable String id) {
        labTestRepository.deleteById(id);
    }

    // upload report
    @PutMapping("/upload-report/{id}")
    public ResponseEntity<?> uploadReport(@PathVariable String id,
                                          @RequestBody ReportUploadRequest request) {
        try {
            LabTest test = labTestRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Test not found with ID: " + id));


            if ("Completed".equals(test.getStatus())) {
                // If already completed, we just update the report content
                test.setReportUrl(request.getReportUrl());
                test.setReportText(request.getReportText());
            } else {
                // Normal flow: set data and move to Completed
                test.setReportUrl(request.getReportUrl());
                test.setReportText(request.getReportText());
                test.setReportStatus("Uploaded");
                test.setStatus("Completed");
            }

            LabTest savedTest = labTestRepository.save(test);
            return ResponseEntity.ok(savedTest);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}