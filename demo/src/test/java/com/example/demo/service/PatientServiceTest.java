package com.example.demo.service;

import com.example.demo.model.Patient;
import com.example.demo.repository.PatientRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PatientService patientService;

    @Test
    void loginPatient_WithValidCredentials_ShouldReturnPatient() {

        Patient patient = new Patient();
        patient.setEmail("patient@gmail.com");
        patient.setPassword("encoded-password");

        when(patientRepository.findByEmail("patient@gmail.com"))
                .thenReturn(Optional.of(patient));

        when(passwordEncoder.matches(
                "password123",
                "encoded-password"
        )).thenReturn(true);

        Patient result = patientService.loginPatient(
                "patient@gmail.com",
                "password123"
        );

        assertNotNull(result);
        assertSame(patient, result);
        assertEquals("patient@gmail.com", result.getEmail());

        verify(patientRepository)
                .findByEmail("patient@gmail.com");

        verify(passwordEncoder)
                .matches(
                        "password123",
                        "encoded-password"
                );
    }

    @Test
    void verifyOtp_WithValidOtp_ShouldVerifyPatient() {

        Patient patient = new Patient();
        patient.setEmail("patient@gmail.com");
        patient.setVerified(false);
        patient.setVerificationCode("123456");
        patient.setVerificationExpiry(
                new java.util.Date(
                        System.currentTimeMillis() + 5 * 60 * 1000
                )
        );

        when(patientRepository.findByEmail("patient@gmail.com"))
                .thenReturn(Optional.of(patient));

        String result = patientService.verifyOtp(
                "patient@gmail.com",
                "123456"
        );

        assertEquals(
                "Email verified successfully",
                result
        );

        assertTrue(patient.isVerified());
        assertNull(patient.getVerificationCode());
        assertNull(patient.getVerificationExpiry());

        verify(patientRepository)
                .save(patient);
    }

    @Test
    void resetPassword_WithValidToken_ShouldResetPassword() {

        Patient patient = new Patient();
        patient.setEmail("patient@gmail.com");
        patient.setPassword("old-encoded-password");
        patient.setResetToken("valid-token");
        patient.setResetTokenExpiry(
                LocalDateTime.now().plusMinutes(20)
        );

        when(patientRepository.findByResetToken("valid-token"))
                .thenReturn(Optional.of(patient));

        when(passwordEncoder.encode("newPassword123"))
                .thenReturn("new-encoded-password");

        boolean result = patientService.resetPassword(
                "valid-token",
                "newPassword123"
        );

        assertTrue(result);

        assertEquals(
                "new-encoded-password",
                patient.getPassword()
        );

        assertNull(patient.getResetToken());
        assertNull(patient.getResetTokenExpiry());

        verify(passwordEncoder)
                .encode("newPassword123");

        verify(patientRepository)
                .save(patient);
    }

    @Test
    void getPatientById_WithValidId_ShouldReturnPatient() {

        Patient patient = new Patient();
        patient.setId("PAT001");
        patient.setFirstName("Nimal");
        patient.setLastName("Perera");
        patient.setEmail("patient@gmail.com");

        when(patientRepository.findById("PAT001"))
                .thenReturn(Optional.of(patient));

        Patient result = patientService.getPatientById("PAT001");

        assertNotNull(result);
        assertSame(patient, result);

        assertEquals(
                "PAT001",
                result.getId()
        );

        assertEquals(
                "patient@gmail.com",
                result.getEmail()
        );

        verify(patientRepository)
                .findById("PAT001");
    }

    @Test
    void updatePatient_WithValidData_ShouldUpdateProfile() {

        Patient existingPatient = new Patient();
        existingPatient.setId("PAT001");
        existingPatient.setFirstName("Old");
        existingPatient.setLastName("Name");
        existingPatient.setEmail("patient@gmail.com");

        Patient updatedData = new Patient();
        updatedData.setTitle("Mr");
        updatedData.setFirstName("Nimal");
        updatedData.setLastName("Perera");
        updatedData.setContactNumber("0771234567");
        updatedData.setResidentialAddress("Colombo");
        updatedData.setProfileImage("profile.jpg");

        when(patientRepository.findById("PAT001"))
                .thenReturn(Optional.of(existingPatient));

        when(patientRepository.save(existingPatient))
                .thenReturn(existingPatient);

        Patient result = patientService.updatePatient(
                "PAT001",
                updatedData
        );

        assertNotNull(result);

        assertEquals("Mr", result.getTitle());
        assertEquals("Nimal", result.getFirstName());
        assertEquals("Perera", result.getLastName());
        assertEquals("0771234567", result.getContactNumber());
        assertEquals("Colombo", result.getResidentialAddress());
        assertEquals("profile.jpg", result.getProfileImage());

        // Email should not be changed by updatePatient()
        assertEquals(
                "patient@gmail.com",
                result.getEmail()
        );

        verify(patientRepository)
                .findById("PAT001");

        verify(patientRepository)
                .save(existingPatient);
    }

    @Test
    void changePassword_WithValidCurrentPassword_ShouldChangePassword() {

        Patient patient = new Patient();
        patient.setEmail("patient@gmail.com");
        patient.setPassword("old-encoded-password");

        when(patientRepository.findByEmail("patient@gmail.com"))
                .thenReturn(Optional.of(patient));

        when(passwordEncoder.matches(
                "oldPassword123",
                "old-encoded-password"
        )).thenReturn(true);

        when(passwordEncoder.encode("newPassword123"))
                .thenReturn("new-encoded-password");

        boolean result = patientService.changePassword(
                "patient@gmail.com",
                "oldPassword123",
                "newPassword123"
        );

        assertTrue(result);

        assertEquals(
                "new-encoded-password",
                patient.getPassword()
        );

        verify(passwordEncoder)
                .matches(
                        "oldPassword123",
                        "old-encoded-password"
                );

        verify(passwordEncoder)
                .encode("newPassword123");

        verify(patientRepository)
                .save(patient);
    }
}