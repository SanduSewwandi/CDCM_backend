package com.example.demo.controller;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.model.Patient;
import com.example.demo.security.JwtService;
import com.example.demo.service.AdminService;
import com.example.demo.service.DoctorService;
import com.example.demo.service.HospitalService;
import com.example.demo.service.PatientService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.demo.model.Doctor;
import com.example.demo.model.Hospital;
import com.example.demo.model.Patient;
import com.example.demo.model.Admin;

import java.util.Map;
import java.util.HashMap;

import com.example.demo.dto.ChangePasswordRequest;
import java.security.Principal;


@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private PatientService patientService;

    @Mock
    private DoctorService doctorService;

    @Mock
    private HospitalService hospitalService;

    @Mock
    private AdminService adminService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthController authController;

    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest();
        loginRequest.setEmail("patient@gmail.com");
        loginRequest.setPassword("password123");
    }

    @Test
    void login_WithValidVerifiedPatient_ShouldReturn200() {

        Patient patient = new Patient();

        patient.setId("PAT001");
        patient.setEmail("patient@gmail.com");
        patient.setFirstName("Test");
        patient.setLastName("Patient");
        patient.setVerified(true);

        when(adminService.loginAdmin(
                "patient@gmail.com",
                "password123"
        )).thenReturn(null);

        when(hospitalService.loginHospital(
                "patient@gmail.com",
                "password123"
        )).thenReturn(null);

        when(doctorService.loginDoctor(
                "patient@gmail.com",
                "password123"
        )).thenReturn(null);

        when(patientService.loginPatient(
                "patient@gmail.com",
                "password123"
        )).thenReturn(patient);

        when(jwtService.generateToken(
                "patient@gmail.com",
                "PATIENT"
        )).thenReturn("mock-jwt-token");

        ResponseEntity<LoginResponse> response =
                authController.login(loginRequest);

        assertEquals(200, response.getStatusCode().value());

        assertNotNull(response.getBody());

        assertEquals(
                "Login Successful",
                response.getBody().getMessage()
        );

        assertEquals(
                "PATIENT",
                response.getBody().getRole()
        );

        assertEquals(
                "PAT001",
                response.getBody().getUserId()
        );

        assertEquals(
                "mock-jwt-token",
                response.getBody().getToken()
        );

        verify(patientService).loginPatient(
                "patient@gmail.com",
                "password123"
        );

        verify(jwtService).generateToken(
                "patient@gmail.com",
                "PATIENT"
        );
    }

    @Test
    void login_WithUnverifiedPatient_ShouldReturn403() {

        Patient patient = new Patient();

        patient.setId("PAT002");
        patient.setEmail("unverified@gmail.com");
        patient.setFirstName("Test");
        patient.setLastName("Patient");
        patient.setVerified(false);

        LoginRequest request = new LoginRequest();
        request.setEmail("unverified@gmail.com");
        request.setPassword("password123");

        when(adminService.loginAdmin(
                "unverified@gmail.com",
                "password123"
        )).thenReturn(null);

        when(hospitalService.loginHospital(
                "unverified@gmail.com",
                "password123"
        )).thenReturn(null);

        when(doctorService.loginDoctor(
                "unverified@gmail.com",
                "password123"
        )).thenReturn(null);

        when(patientService.loginPatient(
                "unverified@gmail.com",
                "password123"
        )).thenReturn(patient);

        ResponseEntity<LoginResponse> response =
                authController.login(request);

        assertEquals(403, response.getStatusCode().value());

        assertNotNull(response.getBody());

        assertEquals(
                "Please verify your email first",
                response.getBody().getMessage()
        );

        assertNull(response.getBody().getToken());

        verify(jwtService, never()).generateToken(
                anyString(),
                anyString()
        );
    }

    @Test
    void login_WithInvalidCredentials_ShouldReturn401() {

        LoginRequest request = new LoginRequest();
        request.setEmail("wrong@gmail.com");
        request.setPassword("wrongpassword");

        when(adminService.loginAdmin(
                "wrong@gmail.com",
                "wrongpassword"
        )).thenReturn(null);

        when(hospitalService.loginHospital(
                "wrong@gmail.com",
                "wrongpassword"
        )).thenReturn(null);

        when(doctorService.loginDoctor(
                "wrong@gmail.com",
                "wrongpassword"
        )).thenReturn(null);

        when(patientService.loginPatient(
                "wrong@gmail.com",
                "wrongpassword"
        )).thenReturn(null);

        ResponseEntity<LoginResponse> response =
                authController.login(request);

        assertEquals(401, response.getStatusCode().value());

        assertNotNull(response.getBody());

        assertEquals(
                "Invalid Email or Password",
                response.getBody().getMessage()
        );

        assertNull(response.getBody().getToken());

        verify(jwtService, never()).generateToken(
                anyString(),
                anyString()
        );
    }

    @Test
    void login_WithValidVerifiedDoctor_ShouldReturn200() {

        Doctor doctor = new Doctor();

        doctor.setId("DOC001");
        doctor.setEmail("doctor@gmail.com");
        doctor.setTitle("Dr.");
        doctor.setFirstName("Test");
        doctor.setLastName("Doctor");
        doctor.setVerified(true);

        LoginRequest request = new LoginRequest();
        request.setEmail("doctor@gmail.com");
        request.setPassword("password123");

        when(adminService.loginAdmin(
                "doctor@gmail.com",
                "password123"
        )).thenReturn(null);

        when(hospitalService.loginHospital(
                "doctor@gmail.com",
                "password123"
        )).thenReturn(null);

        when(doctorService.loginDoctor(
                "doctor@gmail.com",
                "password123"
        )).thenReturn(doctor);

        when(jwtService.generateToken(
                "doctor@gmail.com",
                "DOCTOR"
        )).thenReturn("mock-doctor-token");

        ResponseEntity<LoginResponse> response =
                authController.login(request);

        assertEquals(200, response.getStatusCode().value());

        assertNotNull(response.getBody());

        assertEquals(
                "Login Successful",
                response.getBody().getMessage()
        );

        assertEquals(
                "DOCTOR",
                response.getBody().getRole()
        );

        assertEquals(
                "DOC001",
                response.getBody().getUserId()
        );

        assertEquals(
                "mock-doctor-token",
                response.getBody().getToken()
        );

        assertTrue(response.getBody().isVerified());

        verify(doctorService).loginDoctor(
                "doctor@gmail.com",
                "password123"
        );

        verify(jwtService).generateToken(
                "doctor@gmail.com",
                "DOCTOR"
        );

        verify(patientService, never()).loginPatient(
                anyString(),
                anyString()
        );
    }

    @Test
    void login_WithUnverifiedDoctor_ShouldReturn403() {

        Doctor doctor = new Doctor();

        doctor.setId("DOC002");
        doctor.setEmail("unverifieddoctor@gmail.com");
        doctor.setTitle("Dr.");
        doctor.setFirstName("Test");
        doctor.setLastName("Doctor");
        doctor.setVerified(false);

        LoginRequest request = new LoginRequest();
        request.setEmail("unverifieddoctor@gmail.com");
        request.setPassword("password123");

        when(adminService.loginAdmin(
                "unverifieddoctor@gmail.com",
                "password123"
        )).thenReturn(null);

        when(hospitalService.loginHospital(
                "unverifieddoctor@gmail.com",
                "password123"
        )).thenReturn(null);

        when(doctorService.loginDoctor(
                "unverifieddoctor@gmail.com",
                "password123"
        )).thenReturn(doctor);

        ResponseEntity<LoginResponse> response =
                authController.login(request);

        assertEquals(403, response.getStatusCode().value());

        assertNotNull(response.getBody());

        assertEquals(
                "Please verify your email first",
                response.getBody().getMessage()
        );

        assertNull(response.getBody().getToken());

        verify(jwtService, never()).generateToken(
                anyString(),
                anyString()
        );

        verify(patientService, never()).loginPatient(
                anyString(),
                anyString()
        );
    }

    @Test
    void login_WithValidHospital_ShouldReturn200() {

        Hospital hospital = new Hospital();

        hospital.setId("HOS001");
        hospital.setEmail("hospital@gmail.com");
        hospital.setName("Test Hospital");
        hospital.setVerified(true);
        hospital.setMustChangePassword(false);

        LoginRequest request = new LoginRequest();
        request.setEmail("hospital@gmail.com");
        request.setPassword("password123");

        when(adminService.loginAdmin(
                "hospital@gmail.com",
                "password123"
        )).thenReturn(null);

        when(hospitalService.loginHospital(
                "hospital@gmail.com",
                "password123"
        )).thenReturn(hospital);

        when(jwtService.generateToken(
                "hospital@gmail.com",
                "HOSPITAL"
        )).thenReturn("mock-hospital-token");

        ResponseEntity<LoginResponse> response =
                authController.login(request);

        assertEquals(200, response.getStatusCode().value());

        assertNotNull(response.getBody());

        assertEquals(
                "Login Successful",
                response.getBody().getMessage()
        );

        assertEquals(
                "HOSPITAL",
                response.getBody().getRole()
        );

        assertEquals(
                "HOS001",
                response.getBody().getUserId()
        );

        assertEquals(
                "Test Hospital",
                response.getBody().getName()
        );

        assertEquals(
                "hospital@gmail.com",
                response.getBody().getEmail()
        );

        assertEquals(
                "mock-hospital-token",
                response.getBody().getToken()
        );

        assertTrue(response.getBody().isVerified());

        assertFalse(response.getBody().isMustChangePassword());

        verify(hospitalService).loginHospital(
                "hospital@gmail.com",
                "password123"
        );

        verify(jwtService).generateToken(
                "hospital@gmail.com",
                "HOSPITAL"
        );

        verify(doctorService, never()).loginDoctor(
                anyString(),
                anyString()
        );

        verify(patientService, never()).loginPatient(
                anyString(),
                anyString()
        );
    }

    @Test
    void login_WithValidAdmin_ShouldReturn200() {

        Admin admin = new Admin();

        admin.setId("ADM001");
        admin.setEmail("admin@gmail.com");
        admin.setName("Test Admin");

        LoginRequest request = new LoginRequest();
        request.setEmail("admin@gmail.com");
        request.setPassword("password123");

        when(adminService.loginAdmin(
                "admin@gmail.com",
                "password123"
        )).thenReturn(admin);

        when(jwtService.generateToken(
                "admin@gmail.com",
                "ADMIN"
        )).thenReturn("mock-admin-token");

        ResponseEntity<LoginResponse> response =
                authController.login(request);

        assertEquals(200, response.getStatusCode().value());

        assertNotNull(response.getBody());

        assertEquals(
                "Login Successful",
                response.getBody().getMessage()
        );

        assertEquals(
                "ADMIN",
                response.getBody().getRole()
        );

        assertEquals(
                "ADM001",
                response.getBody().getUserId()
        );

        assertEquals(
                "Test Admin",
                response.getBody().getName()
        );

        assertEquals(
                "mock-admin-token",
                response.getBody().getToken()
        );

        verify(adminService).loginAdmin(
                "admin@gmail.com",
                "password123"
        );

        verify(jwtService).generateToken(
                "admin@gmail.com",
                "ADMIN"
        );

        verify(hospitalService, never()).loginHospital(
                anyString(),
                anyString()
        );

        verify(doctorService, never()).loginDoctor(
                anyString(),
                anyString()
        );

        verify(patientService, never()).loginPatient(
                anyString(),
                anyString()
        );
    }

    @Test
    void verifyEmail_WithValidPatientOtp_ShouldReturn200() {

        when(patientService.verifyOtp(
                "patient@gmail.com",
                "123456"
        )).thenReturn("Email verified successfully");

        java.util.Map<String, String> request = new java.util.HashMap<>();
        request.put("email", "patient@gmail.com");
        request.put("code", "123456");
        request.put("role", "PATIENT");

        ResponseEntity<?> response =
                authController.verifyEmail(request);

        assertEquals(200, response.getStatusCode().value());

        assertNotNull(response.getBody());

        assertEquals(
                java.util.Map.of(
                        "message",
                        "Email verified successfully"
                ),
                response.getBody()
        );

        verify(patientService).verifyOtp(
                "patient@gmail.com",
                "123456"
        );

        verify(doctorService, never()).verifyOtp(
                anyString(),
                anyString()
        );

        verify(hospitalService, never()).verifyOtp(
                anyString(),
                anyString()
        );
    }

    @Test
    void verifyEmail_WithInvalidPatientOtp_ShouldReturn400() {

        when(patientService.verifyOtp(
                "patient@gmail.com",
                "999999"
        )).thenReturn("Invalid or expired verification code");

        Map<String, String> request = new HashMap<>();
        request.put("email", "patient@gmail.com");
        request.put("code", "999999");
        request.put("role", "PATIENT");

        ResponseEntity<?> response =
                authController.verifyEmail(request);

        assertEquals(400, response.getStatusCode().value());

        assertNotNull(response.getBody());

        assertEquals(
                Map.of(
                        "message",
                        "Invalid or expired verification code"
                ),
                response.getBody()
        );

        verify(patientService).verifyOtp(
                "patient@gmail.com",
                "999999"
        );

        verify(doctorService, never()).verifyOtp(
                anyString(),
                anyString()
        );

        verify(hospitalService, never()).verifyOtp(
                anyString(),
                anyString()
        );
    }

    @Test
    void verifyEmail_WithValidDoctorOtp_ShouldReturn200() {

        when(doctorService.verifyOtp(
                "doctor@gmail.com",
                "123456"
        )).thenReturn("Email verified successfully");

        Map<String, String> request = new HashMap<>();
        request.put("email", "doctor@gmail.com");
        request.put("code", "123456");
        request.put("role", "DOCTOR");

        ResponseEntity<?> response =
                authController.verifyEmail(request);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        assertEquals(
                Map.of("message", "Email verified successfully"),
                response.getBody()
        );

        verify(doctorService).verifyOtp(
                "doctor@gmail.com",
                "123456"
        );

        verify(patientService, never()).verifyOtp(
                anyString(),
                anyString()
        );

        verify(hospitalService, never()).verifyOtp(
                anyString(),
                anyString()
        );
    }

    @Test
    void verifyEmail_WithValidHospitalOtp_ShouldReturn200() {

        when(hospitalService.verifyOtp(
                "hospital@gmail.com",
                "123456"
        )).thenReturn("Email verified successfully");

        Map<String, String> request = new HashMap<>();
        request.put("email", "hospital@gmail.com");
        request.put("code", "123456");
        request.put("role", "HOSPITAL");

        ResponseEntity<?> response =
                authController.verifyEmail(request);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        assertEquals(
                Map.of("message", "Email verified successfully"),
                response.getBody()
        );

        verify(hospitalService).verifyOtp(
                "hospital@gmail.com",
                "123456"
        );

        verify(patientService, never()).verifyOtp(
                anyString(),
                anyString()
        );

        verify(doctorService, never()).verifyOtp(
                anyString(),
                anyString()
        );
    }

    @Test
    void forgotPassword_WithValidPatientEmail_ShouldReturn200() {

        when(patientService.sendPasswordResetEmail(
                "patient@gmail.com"
        )).thenReturn(true);

        ResponseEntity<?> response =
                authController.forgotPassword(
                        "patient@gmail.com",
                        "PATIENT"
                );

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        assertEquals(
                Map.of("message", "Password reset email sent"),
                response.getBody()
        );

        verify(patientService).sendPasswordResetEmail(
                "patient@gmail.com"
        );

        verify(doctorService, never())
                .sendPasswordResetEmail(anyString());

        verify(hospitalService, never())
                .sendPasswordResetEmail(anyString());
    }

    @Test
    void forgotPassword_WithUnknownPatientEmail_ShouldReturn400() {

        when(patientService.sendPasswordResetEmail(
                "unknown@gmail.com"
        )).thenReturn(false);

        ResponseEntity<?> response =
                authController.forgotPassword(
                        "unknown@gmail.com",
                        "PATIENT"
                );

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());

        assertEquals(
                Map.of("message", "Email not found"),
                response.getBody()
        );

        verify(patientService).sendPasswordResetEmail(
                "unknown@gmail.com"
        );

        verify(doctorService, never())
                .sendPasswordResetEmail(anyString());

        verify(hospitalService, never())
                .sendPasswordResetEmail(anyString());
    }

    @Test
    void forgotPassword_WithValidDoctorEmail_ShouldReturn200() {

        when(doctorService.sendPasswordResetEmail(
                "doctor@gmail.com"
        )).thenReturn(true);

        ResponseEntity<?> response =
                authController.forgotPassword(
                        "doctor@gmail.com",
                        "DOCTOR"
                );

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        assertEquals(
                Map.of("message", "Password reset email sent"),
                response.getBody()
        );

        verify(doctorService).sendPasswordResetEmail(
                "doctor@gmail.com"
        );

        verify(patientService, never())
                .sendPasswordResetEmail(anyString());

        verify(hospitalService, never())
                .sendPasswordResetEmail(anyString());
    }

    @Test
    void forgotPassword_WithValidHospitalEmail_ShouldReturn200() {

        when(hospitalService.sendPasswordResetEmail(
                "hospital@gmail.com"
        )).thenReturn(true);

        ResponseEntity<?> response =
                authController.forgotPassword(
                        "hospital@gmail.com",
                        "HOSPITAL"
                );

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        assertEquals(
                Map.of("message", "Password reset email sent"),
                response.getBody()
        );

        verify(hospitalService).sendPasswordResetEmail(
                "hospital@gmail.com"
        );

        verify(patientService, never())
                .sendPasswordResetEmail(anyString());

        verify(doctorService, never())
                .sendPasswordResetEmail(anyString());
    }

    @Test
    void resetPassword_WithValidPatientToken_ShouldReturn200() {

        when(patientService.resetPassword(
                "valid-token",
                "newPassword123"
        )).thenReturn(true);

        Map<String, String> request = new HashMap<>();
        request.put("token", "valid-token");
        request.put("newPassword", "newPassword123");
        request.put("role", "PATIENT");

        ResponseEntity<?> response =
                authController.resetPassword(request);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        assertEquals(
                "Password reset successful",
                response.getBody()
        );

        verify(patientService).resetPassword(
                "valid-token",
                "newPassword123"
        );

        verify(doctorService, never())
                .resetPassword(anyString(), anyString());

        verify(hospitalService, never())
                .resetPassword(anyString(), anyString());
    }

    @Test
    void resetPassword_WithInvalidPatientToken_ShouldReturn400() {

        when(patientService.resetPassword(
                "invalid-token",
                "newPassword123"
        )).thenReturn(false);

        Map<String, String> request = new HashMap<>();
        request.put("token", "invalid-token");
        request.put("newPassword", "newPassword123");
        request.put("role", "PATIENT");

        ResponseEntity<?> response =
                authController.resetPassword(request);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());

        assertEquals(
                "Invalid or expired token",
                response.getBody()
        );

        verify(patientService).resetPassword(
                "invalid-token",
                "newPassword123"
        );

        verify(doctorService, never())
                .resetPassword(anyString(), anyString());

        verify(hospitalService, never())
                .resetPassword(anyString(), anyString());
    }

    @Test
    void changePassword_WithValidPatientDetails_ShouldReturn200() {

        Principal principal = () -> "patient@gmail.com";

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword123");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");

        ResponseEntity<?> response =
                authController.changePassword(principal, request);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());

        assertEquals(
                Map.of("message", "Password changed successfully"),
                response.getBody()
        );

        verify(patientService).changePassword(
                "patient@gmail.com",
                "oldPassword123",
                "newPassword123"
        );

        verify(doctorService, never()).changePassword(
                anyString(),
                anyString(),
                anyString()
        );

        verify(hospitalService, never()).changePassword(
                anyString(),
                anyString(),
                anyString()
        );
    }

    @Test
    void changePassword_WithWrongCurrentPassword_ShouldReturn400() {

        Principal principal = () -> "patient@gmail.com";

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongPassword");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");

        doThrow(new RuntimeException("Current password does not match"))
                .when(patientService)
                .changePassword(
                        "patient@gmail.com",
                        "wrongPassword",
                        "newPassword123"
                );

        ResponseEntity<?> response =
                authController.changePassword(principal, request);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());

        assertEquals(
                Map.of("message", "Current password does not match"),
                response.getBody()
        );

        verify(patientService).changePassword(
                "patient@gmail.com",
                "wrongPassword",
                "newPassword123"
        );

        verify(doctorService, never()).changePassword(
                anyString(),
                anyString(),
                anyString()
        );

        verify(hospitalService, never()).changePassword(
                anyString(),
                anyString(),
                anyString()
        );
    }

    @Test
    void changePassword_WithPasswordMismatch_ShouldReturn400() {

        Principal principal = () -> "patient@gmail.com";

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword123");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("differentPassword123");

        ResponseEntity<?> response =
                authController.changePassword(principal, request);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());

        assertEquals(
                Map.of(
                        "message",
                        "New password and confirmation do not match"
                ),
                response.getBody()
        );

        verify(patientService, never()).changePassword(
                anyString(),
                anyString(),
                anyString()
        );

        verify(doctorService, never()).changePassword(
                anyString(),
                anyString(),
                anyString()
        );

        verify(hospitalService, never()).changePassword(
                anyString(),
                anyString(),
                anyString()
        );
    }

    @Test
    void changePassword_WithoutAuthentication_ShouldReturn401() {

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword123");
        request.setNewPassword("newPassword123");
        request.setConfirmPassword("newPassword123");

        ResponseEntity<?> response =
                authController.changePassword(null, request);

        assertEquals(401, response.getStatusCode().value());
        assertNotNull(response.getBody());

        assertEquals(
                Map.of("message", "Authentication required"),
                response.getBody()
        );

        verify(patientService, never()).changePassword(
                anyString(),
                anyString(),
                anyString()
        );

        verify(doctorService, never()).changePassword(
                anyString(),
                anyString(),
                anyString()
        );

        verify(hospitalService, never()).changePassword(
                anyString(),
                anyString(),
                anyString()
        );
    }
}