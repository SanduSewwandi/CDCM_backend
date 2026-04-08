package com.example.demo.service;

import com.example.demo.model.LabTest;
import com.example.demo.repository.LabTestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LabTestService {

    @Autowired
    private LabTestRepository labTestRepository;

    // Get all lab tests
    public List<LabTest> getAllLabTests() {
        return labTestRepository.findAll();
    }

    // Get lab test by ID
    public LabTest getLabTestById(String id) {
        return labTestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lab test not found with id: " + id));
    }

    // Get lab tests by hospital ID
    public List<LabTest> getLabTestsByHospitalId(String hospitalId) {
        return labTestRepository.findByHospitalId(hospitalId);
    }

    // Update report (text and/or URL)
    public LabTest updateReport(String id, String reportText, String reportUrl) {
        LabTest labTest = getLabTestById(id);

        if (reportText != null && !reportText.trim().isEmpty()) {
            labTest.setReportText(reportText);
            labTest.setReportStatus("Completed");
        }

        if (reportUrl != null && !reportUrl.trim().isEmpty()) {
            labTest.setReportUrl(reportUrl);
            labTest.setReportStatus("Completed");
        }

        // Update status if both report and payment are done
        if (labTest.isPaid() && (labTest.getReportText() != null || labTest.getReportUrl() != null)) {
            labTest.setStatus("Completed");
        }

        // DON'T try to set createdAt or updatedAt here!
        // The touch() method in the model handles updatedAt automatically
        // createdAt is already set in the constructor

        return labTestRepository.save(labTest);
    }

    // Update payment status
    public LabTest updatePaymentStatus(String id, boolean isPaid) {
        LabTest labTest = getLabTestById(id);
        labTest.setPaid(isPaid);
        if (isPaid) {
            labTest.setPaidAt(LocalDateTime.now());
        }
        return labTestRepository.save(labTest);
    }

    // Create new lab test
    public LabTest createLabTest(LabTest labTest) {
        // Don't set createdAt/updatedAt here - they're set in the constructor
        // Just save the object
        return labTestRepository.save(labTest);
    }
}