
package com.example.demo.integration;

import com.example.demo.config.SecurityConfig;
import com.example.demo.controller.LabController;
import com.example.demo.model.LabTest;
import com.example.demo.model.Patient;
import com.example.demo.repository.*;
import com.example.demo.security.JwtAuthenticationFilter;
import com.example.demo.security.JwtService;
import com.example.demo.service.FileUploadService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LabController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class LabIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LabTestRepository labTestRepository;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private TestCategoryRepository testCategoryRepository;

    @MockitoBean
    private HospitalRepository hospitalRepository;

    @MockitoBean
    private FileUploadService fileUploadService;

    @MockitoBean
    private JwtService jwtService;

    private void mockHospitalToken() {
        String token = "valid.hospital.token";

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token))
                .thenReturn("hospital@example.com");
        when(jwtService.extractRole(token))
                .thenReturn("HOSPITAL");
    }

    private LabTest createLabTest() {
        LabTest test = new LabTest();
        test.setId("lab123");
        test.setPatientId("patient123");
        test.setHospitalId("hospital123");
        test.setTestType("Blood Test");
        test.setStatus("Pending");
        test.setReportStatus("Pending");
        return test;
    }



    @Test
    void getLabPatientsShouldReturn200() throws Exception {
        mockHospitalToken();

        Patient patient = new Patient();
        patient.setId("patient123");

        when(patientRepository.findAll())
                .thenReturn(List.of(patient));

        mockMvc.perform(
                        get("/api/lab/patients")
                                .header("Authorization", "Bearer valid.hospital.token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("patient123"));

        verify(patientRepository).findAll();
    }

    // Test 02 - Get All Lab Patients - Empty List (200 OK)

    @Test
    void getLabPatientsEmptyShouldReturn200() throws Exception {
        mockHospitalToken();

        when(patientRepository.findAll())
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/lab/patients")
                                .header("Authorization", "Bearer valid.hospital.token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(patientRepository).findAll();
    }

    // Test 03 - Add Lab Test (200 OK)

    @Test
    void addLabTestShouldReturn200() throws Exception {
        mockHospitalToken();

        when(labTestRepository.save(any(LabTest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(
                        post("/api/lab/add")
                                .header("Authorization", "Bearer valid.hospital.token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "patientId": "patient123",
                                  "hospitalId": "hospital123",
                                  "testType": "Blood Test"
                                }
                                """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value("patient123"))
                .andExpect(jsonPath("$.hospitalId").value("hospital123"))
                .andExpect(jsonPath("$.status").value("Pending"))
                .andExpect(jsonPath("$.reportStatus").value("Pending"));

        verify(labTestRepository).save(any(LabTest.class));
        verify(notificationRepository).save(any());
    }

    // Test 04 - Get All Lab Tests (200 OK)

    @Test
    void getAllLabTestsShouldReturn200() throws Exception {
        mockHospitalToken();

        when(labTestRepository.findAll())
                .thenReturn(List.of(createLabTest()));

        mockMvc.perform(
                        get("/api/lab/all")
                                .header("Authorization", "Bearer valid.hospital.token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("lab123"))
                .andExpect(jsonPath("$[0].testType").value("Blood Test"));

        verify(labTestRepository).findAll();
    }

    // Test 05 - Get Lab Tests by Patient (200 OK)

    @Test
    void getLabTestsByPatientShouldReturn200() throws Exception {
        mockHospitalToken();

        when(labTestRepository.findByPatientId("patient123"))
                .thenReturn(List.of(createLabTest()));

        mockMvc.perform(
                        get("/api/lab/patient/patient123")
                                .header("Authorization", "Bearer valid.hospital.token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].patientId").value("patient123"));

        verify(labTestRepository).findByPatientId("patient123");
    }

    // Test 06 - Get Single Lab Test (200 OK)

    @Test
    void getSingleLabTestShouldReturn200() throws Exception {
        mockHospitalToken();

        when(labTestRepository.findById("lab123"))
                .thenReturn(Optional.of(createLabTest()));

        mockMvc.perform(
                        get("/api/lab/lab123")
                                .header("Authorization", "Bearer valid.hospital.token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("lab123"))
                .andExpect(jsonPath("$.status").value("Pending"));

        verify(labTestRepository).findById("lab123");
    }

    // Test 07 - Get Single Lab Test - Not Found
    // Verify that an unknown lab test triggers the configured error response.

    @Test
    void getSingleLabTestNotFoundShouldReturn500() throws Exception {
        mockHospitalToken();

        when(labTestRepository.findById("invalidLab"))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                        get("/api/lab/invalidLab")
                                .header("Authorization", "Bearer valid.hospital.token")
                )
                .andExpect(status().isInternalServerError());

        verify(labTestRepository).findById("invalidLab");
    }

    // Test 08 - Update Lab Test (200 OK)
    // Verify that a lab test's type and status can be updated.

    @Test
    void updateLabTestShouldReturn200() throws Exception {
        mockHospitalToken();

        when(labTestRepository.findById("lab123"))
                .thenReturn(Optional.of(createLabTest()));

        when(labTestRepository.save(any(LabTest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(
                        put("/api/lab/update/lab123")
                                .header("Authorization", "Bearer valid.hospital.token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "testType": "Full Blood Count",
                                  "status": "In Progress"
                                }
                                """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.testType").value("Full Blood Count"))
                .andExpect(jsonPath("$.status").value("In Progress"));

        verify(labTestRepository).save(any(LabTest.class));
        verify(notificationRepository).save(any());
    }

    // Test 09 - Update Lab Test - Invalid Status (500)

    @Test
    void updateLabTestInvalidStatusShouldReturn500() throws Exception {
        mockHospitalToken();

        when(labTestRepository.findById("lab123"))
                .thenReturn(Optional.of(createLabTest()));

        mockMvc.perform(
                        put("/api/lab/update/lab123")
                                .header("Authorization", "Bearer valid.hospital.token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"Completed\"}")
                )
                .andExpect(status().isInternalServerError());

        verify(labTestRepository, never()).save(any(LabTest.class));
    }

    // Test 10 - Pay for Lab Test (200 OK)
    // Verify that payment marks the lab test as paid.

    @Test
    void payForLabTestShouldReturn200() throws Exception {
        LabTest test = createLabTest();

        when(labTestRepository.findById("lab123"))
                .thenReturn(Optional.of(test));

        when(labTestRepository.save(any(LabTest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(
                        post("/api/lab/pay/lab123")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paid").value(true))
                .andExpect(jsonPath("$.hospitalId").value("hospital123"));

        verify(labTestRepository).save(any(LabTest.class));
        verify(notificationRepository).save(any());
    }

    // Test 11 - Delete Lab Test (200 OK)
    // Verify that the delete endpoint calls the repository.

    @Test
    void deleteLabTestShouldReturn200() throws Exception {
        mockHospitalToken();

        mockMvc.perform(
                        delete("/api/lab/delete/lab123")
                                .header("Authorization", "Bearer valid.hospital.token")
                )
                .andExpect(status().isOk());

        verify(labTestRepository).deleteById("lab123");
    }

    // Test 12 - Upload Report Without Payment (400 Bad Request)

    @Test
    void uploadReportWithoutPaymentShouldReturn400() throws Exception {
        mockHospitalToken();

        LabTest test = createLabTest();
        test.setPaid(false);

        when(labTestRepository.findById("lab123"))
                .thenReturn(Optional.of(test));

        mockMvc.perform(
                        multipart("/api/lab/upload-report-with-file/lab123")
                                .header("Authorization", "Bearer valid.hospital.token")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Payment required before uploading report"));

        verify(labTestRepository, never()).save(any(LabTest.class));
    }

    // Test 13 - Upload Report With File (200 OK)

    @Test
    void uploadReportWithFileShouldReturn200() throws Exception {
        mockHospitalToken();

        LabTest test = createLabTest();
        test.setPaid(true);

        when(labTestRepository.findById("lab123"))
                .thenReturn(Optional.of(test));

        when(fileUploadService.uploadFile(any()))
                .thenReturn("https://example.com/report.pdf");

        when(labTestRepository.save(any(LabTest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "report.pdf",
                "application/pdf",
                "Sample PDF".getBytes()
        );

        mockMvc.perform(
                        multipart("/api/lab/upload-report-with-file/lab123")
                                .file(file)
                                .header("Authorization", "Bearer valid.hospital.token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reportStatus").value("Uploaded"))
                .andExpect(jsonPath("$.data.status").value("Completed"))
                .andExpect(jsonPath("$.data.reportUrl")
                        .value("https://example.com/report.pdf"));

        verify(fileUploadService).uploadFile(any());
        verify(labTestRepository).save(any(LabTest.class));
    }

    // Test 14 - Download Report (200 OK)

    @Test
    void downloadReportShouldReturn200() throws Exception {
        LabTest test = createLabTest();
        test.setReportUrl("https://example.com/report.pdf");

        when(labTestRepository.findById("lab123"))
                .thenReturn(Optional.of(test));

        mockMvc.perform(
                        get("/api/lab/download-report/lab123")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url")
                        .value("https://example.com/report.pdf"));

        verify(labTestRepository).findById("lab123");
    }

    // Test 15 - Download Report - Not Uploaded (400 Bad Request)
    // Verify that a missing report URL produces a clear error response.

    @Test
    void downloadReportNotUploadedShouldReturn400() throws Exception {
        LabTest test = createLabTest();
        test.setReportUrl(null);

        when(labTestRepository.findById("lab123"))
                .thenReturn(Optional.of(test));

        mockMvc.perform(
                        get("/api/lab/download-report/lab123")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("No report uploaded"));

        verify(labTestRepository).findById("lab123");
    }
}
