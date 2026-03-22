package com.example.demo.controller;

import com.example.demo.dto.LabTestRequest;
import com.example.demo.model.LabTest;
import com.example.demo.model.Patient;
import com.example.demo.repository.LabTestRepository;
import com.example.demo.repository.PatientRepository;
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

    // Get all patients (dropdown)
    @GetMapping("/patients")
    public List<Patient> getPatients() {
        return patientRepository.findAll();
    }

    // Add lab test (DEFAULT STATUS)
    @PostMapping("/add")
    public LabTest addLabTest(@RequestBody LabTestRequest request) {

        LabTest test = new LabTest();
        test.setPatientId(request.getPatientId());
        test.setTestType(request.getTestType());
        test.setPrice(request.getPrice());
        test.setTestDate(request.getTestDate());
        test.setRequestedDate(request.getRequestedDate());

        // DEFAULT VALUES
        test.setStatus("Pending");
        test.setReportStatus("Pending");

        return labTestRepository.save(test);
    }

    // Get all lab tests
    @GetMapping("/all")
    public List<LabTest> getAllTests() {
        return labTestRepository.findAll();
    }

    // Get single test
    @GetMapping("/{id}")
    public LabTest getTest(@PathVariable String id) {
        return labTestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Test not found"));
    }

    // UPDATE TEST WITH WORKFLOW RULES
    @PutMapping("/update/{id}")
    public LabTest updateTest(@PathVariable String id,
                              @RequestBody LabTestRequest request) {

        LabTest test = labTestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Test not found"));

        // Update basic fields
        if (request.getTestType() != null)
            test.setTestType(request.getTestType());

        if (request.getTestDate() != null)
            test.setTestDate(request.getTestDate());

        if (request.getRequestedDate() != null)
            test.setRequestedDate(request.getRequestedDate());

        // STATUS WORKFLOW LOGIC
        if (request.getStatus() != null) {

            String current = test.getStatus();
            String next = request.getStatus();

            // 🔹 Pending → In Progress
            if (current.equals("Pending") && next.equals("In Progress")) {
                test.setStatus("In Progress");

                // 🔹 In Progress → Completed (ONLY if report exists)
            } else if (current.equals("In Progress") && next.equals("Completed")) {

                if (test.getReportUrl() == null && test.getReportText() == null) {
                    throw new RuntimeException("Cannot complete test without report");
                }

                test.setStatus("Completed");

            } else {
                throw new RuntimeException("Invalid status transition");
            }
        }

        return labTestRepository.save(test);
    }

    // DELETE
    @DeleteMapping("/delete/{id}")
    public void deleteTest(@PathVariable String id) {
        labTestRepository.deleteById(id);
    }

    // UPLOAD REPORT + AUTO COMPLETE
    @PutMapping("/upload-report/{id}")
    public LabTest uploadReport(
            @PathVariable String id,
            @RequestBody LabTest request
    ) {
        LabTest test = labTestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Test not found"));

        // ❗ Only allow upload when IN PROGRESS
        if (!"In Progress".equals(test.getStatus())) {
            throw new RuntimeException("Test must be In Progress to upload report");
        }

        // Save report
        test.setReportUrl(request.getReportUrl());
        test.setReportText(request.getReportText());
        test.setReportStatus("Uploaded");

        // AUTO COMPLETE AFTER REPORT
        test.setStatus("Completed");

        return labTestRepository.save(test);
    }
}