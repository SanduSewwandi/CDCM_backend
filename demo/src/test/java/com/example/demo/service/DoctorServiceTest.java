package com.example.demo.service;

import com.example.demo.model.Doctor;
import com.example.demo.repository.DoctorRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Date;
import java.util.ArrayList;

@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private DoctorService doctorService;


    @Test
    void loginDoctor_WithValidCredentials_ShouldReturnDoctor() {

        Doctor doctor = new Doctor();
        doctor.setId("DOC001");
        doctor.setEmail("doctor@gmail.com");
        doctor.setPassword("encoded-password");
        doctor.setVerified(true);

        when(doctorRepository.findByEmail(
                "doctor@gmail.com"
        )).thenReturn(Optional.of(doctor));

        when(passwordEncoder.matches(
                "password123",
                "encoded-password"
        )).thenReturn(true);

        Doctor result = doctorService.loginDoctor(
                "doctor@gmail.com",
                "password123"
        );

        assertNotNull(result);
        assertEquals("DOC001", result.getId());
        assertEquals("doctor@gmail.com", result.getEmail());
        assertTrue(result.isVerified());

        verify(doctorRepository).findByEmail(
                "doctor@gmail.com"
        );

        verify(passwordEncoder).matches(
                "password123",
                "encoded-password"
        );
    }

    @Test
    void loginDoctor_WithWrongPassword_ShouldReturnNull() {

        Doctor doctor = new Doctor();
        doctor.setEmail("doctor@gmail.com");
        doctor.setPassword("encoded-password");
        doctor.setVerified(true);

        when(doctorRepository.findByEmail(
                "doctor@gmail.com"
        )).thenReturn(Optional.of(doctor));

        when(passwordEncoder.matches(
                "wrongPassword",
                "encoded-password"
        )).thenReturn(false);

        Doctor result = doctorService.loginDoctor(
                "doctor@gmail.com",
                "wrongPassword"
        );

        assertNull(result);

        verify(doctorRepository).findByEmail(
                "doctor@gmail.com"
        );

        verify(passwordEncoder).matches(
                "wrongPassword",
                "encoded-password"
        );
    }

    @Test
    void verifyOtp_WithValidOtp_ShouldVerifyDoctor() {

        Doctor doctor = new Doctor();
        doctor.setEmail("doctor@gmail.com");
        doctor.setVerified(false);
        doctor.setVerificationCode("123456");
        doctor.setVerificationExpiry(
                new Date(System.currentTimeMillis() + 300000)
        );

        when(doctorRepository.findByEmail(
                "doctor@gmail.com"
        )).thenReturn(Optional.of(doctor));

        String result = doctorService.verifyOtp(
                "doctor@gmail.com",
                "123456"
        );

        assertEquals("Email verified successfully", result);
        assertTrue(doctor.isVerified());
        assertNull(doctor.getVerificationCode());
        assertNull(doctor.getVerificationExpiry());

        verify(doctorRepository).save(doctor);
    }

    @Test
    void getDoctorById_WithValidId_ShouldReturnDoctor() {

        Doctor doctor = new Doctor();
        doctor.setId("DOC001");
        doctor.setFirstName("Test");
        doctor.setLastName("Doctor");
        doctor.setEmail("doctor@gmail.com");

        when(doctorRepository.findById("DOC001"))
                .thenReturn(Optional.of(doctor));

        Doctor result = doctorService.getDoctorById("DOC001");

        assertNotNull(result);
        assertEquals("DOC001", result.getId());
        assertEquals("Test", result.getFirstName());
        assertEquals("doctor@gmail.com", result.getEmail());

        verify(doctorRepository).findById("DOC001");
    }

    @Test
    void assignDoctorToHospital_ShouldAssignHospitalSuccessfully() {

        Doctor doctor = new Doctor();
        doctor.setId("DOC001");
        doctor.setHospitals(new ArrayList<>());

        when(doctorRepository.findById("DOC001"))
                .thenReturn(Optional.of(doctor));

        when(doctorRepository.save(doctor))
                .thenReturn(doctor);

        Doctor result = doctorService.assignDoctorToHospital(
                "DOC001",
                "HOS001"
        );

        assertNotNull(result);
        assertTrue(result.getHospitals().contains("HOS001"));
        assertEquals(1, result.getHospitals().size());

        verify(doctorRepository).findById("DOC001");
        verify(doctorRepository).save(doctor);
    }

    @Test
    void removeDoctorFromHospital_ShouldRemoveHospitalSuccessfully() {

        Doctor doctor = new Doctor();
        doctor.setId("DOC001");

        ArrayList<String> hospitals = new ArrayList<>();
        hospitals.add("HOS001");
        hospitals.add("HOS002");

        doctor.setHospitals(hospitals);

        when(doctorRepository.findById("DOC001"))
                .thenReturn(Optional.of(doctor));

        when(doctorRepository.save(doctor))
                .thenReturn(doctor);

        Doctor result = doctorService.removeDoctorFromHospital(
                "DOC001",
                "HOS001"
        );

        assertNotNull(result);

        assertFalse(
                result.getHospitals().contains("HOS001")
        );

        assertTrue(
                result.getHospitals().contains("HOS002")
        );

        assertEquals(1, result.getHospitals().size());

        verify(doctorRepository).findById("DOC001");
        verify(doctorRepository).save(doctor);
    }
}