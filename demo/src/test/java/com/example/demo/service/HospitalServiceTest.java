package com.example.demo.service;

import com.example.demo.model.Hospital;
import com.example.demo.repository.HospitalRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.demo.dto.HospitalRegisterRequest;
import com.example.demo.dto.HospitalProfileDTO;

@ExtendWith(MockitoExtension.class)
class HospitalServiceTest {

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private HospitalService hospitalService;

    @Test
    void loginHospital_WithValidCredentials_ShouldReturnHospital() {

        Hospital hospital = new Hospital();
        hospital.setEmail("hospital@gmail.com");
        hospital.setPassword("encoded-password");

        when(hospitalRepository.findByEmail("hospital@gmail.com"))
                .thenReturn(Optional.of(hospital));

        when(passwordEncoder.matches(
                "password123",
                "encoded-password"
        )).thenReturn(true);

        Hospital result = hospitalService.loginHospital(
                "hospital@gmail.com",
                "password123"
        );

        assertNotNull(result);
        assertSame(hospital, result);
        assertEquals(
                "hospital@gmail.com",
                result.getEmail()
        );

        verify(hospitalRepository)
                .findByEmail("hospital@gmail.com");

        verify(passwordEncoder)
                .matches(
                        "password123",
                        "encoded-password"
                );
    }

    @Test
    void registerHospital_WithValidDetails_ShouldRegisterHospital() {

        HospitalRegisterRequest request = new HospitalRegisterRequest();

        request.setName("City Hospital");
        request.setEmail("HOSPITAL@gmail.com");
        request.setContactNumber("0112345678");
        request.setAddress("Colombo");
        request.setLicenseNumber("LIC001");
        request.setManagerName("Nimal Perera");
        request.setPassword("tempPassword123");

        when(hospitalRepository.findByEmail("hospital@gmail.com"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("tempPassword123"))
                .thenReturn("encoded-password");

        when(hospitalRepository.save(any(Hospital.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Hospital result =
                hospitalService.registerHospital(request);

        assertNotNull(result);

        assertEquals(
                "hospital@gmail.com",
                result.getEmail()
        );

        assertEquals(
                "encoded-password",
                result.getPassword()
        );

        assertFalse(result.isVerified());
        assertTrue(result.isMustChangePassword());

        assertNull(result.getVerificationCode());
        assertNull(result.getVerificationExpiry());

        verify(hospitalRepository)
                .save(any(Hospital.class));

        verify(emailService)
                .sendHospitalWelcomeEmail(
                        "hospital@gmail.com",
                        "City Hospital",
                        "tempPassword123"
                );
    }

    @Test
    void requestVerificationCode_WithUnverifiedHospital_ShouldSendCode() {

        Hospital hospital = new Hospital();
        hospital.setEmail("hospital@gmail.com");
        hospital.setVerified(false);
        hospital.setVerificationLastSentAt(null);

        when(hospitalRepository.findByEmail("hospital@gmail.com"))
                .thenReturn(Optional.of(hospital));

        String result =
                hospitalService.requestVerificationCode(
                        "hospital@gmail.com"
                );

        assertEquals(
                "Verification code sent successfully",
                result
        );

        assertNotNull(hospital.getVerificationCode());
        assertEquals(
                6,
                hospital.getVerificationCode().length()
        );

        assertNotNull(hospital.getVerificationExpiry());
        assertNotNull(hospital.getVerificationLastSentAt());

        verify(hospitalRepository)
                .save(hospital);

        verify(emailService)
                .sendOtp(
                        eq("hospital@gmail.com"),
                        eq(hospital.getVerificationCode())
                );
    }

    @Test
    void verifyOtp_WithValidOtp_ShouldVerifyHospital() {

        Hospital hospital = new Hospital();
        hospital.setEmail("hospital@gmail.com");
        hospital.setVerified(false);
        hospital.setVerificationCode("123456");
        hospital.setVerificationExpiry(
                new java.util.Date(
                        System.currentTimeMillis() + 5 * 60 * 1000
                )
        );
        hospital.setVerificationLastSentAt(new java.util.Date());

        when(hospitalRepository.findByEmail("hospital@gmail.com"))
                .thenReturn(Optional.of(hospital));

        String result = hospitalService.verifyOtp(
                "hospital@gmail.com",
                "123456"
        );

        assertEquals(
                "Email verified successfully",
                result
        );

        assertTrue(hospital.isVerified());
        assertNull(hospital.getVerificationCode());
        assertNull(hospital.getVerificationExpiry());
        assertNull(hospital.getVerificationLastSentAt());

        verify(hospitalRepository)
                .save(hospital);
    }

    @Test
    void changeFirstLoginPassword_WithValidHospital_ShouldChangePassword() {

        Hospital hospital = new Hospital();
        hospital.setEmail("hospital@gmail.com");
        hospital.setPassword("temporary-encoded-password");
        hospital.setVerified(true);
        hospital.setMustChangePassword(true);

        when(hospitalRepository.findByEmail("hospital@gmail.com"))
                .thenReturn(Optional.of(hospital));

        when(passwordEncoder.encode("newPassword123"))
                .thenReturn("new-encoded-password");

        when(hospitalRepository.save(hospital))
                .thenReturn(hospital);

        Hospital result =
                hospitalService.changeFirstLoginPassword(
                        "hospital@gmail.com",
                        "newPassword123"
                );

        assertNotNull(result);

        assertEquals(
                "new-encoded-password",
                result.getPassword()
        );

        assertFalse(result.isMustChangePassword());
        assertTrue(result.isVerified());

        verify(passwordEncoder)
                .encode("newPassword123");

        verify(hospitalRepository)
                .save(hospital);
    }

    @Test
    void updateMyProfile_WithValidData_ShouldUpdateHospitalProfile() {

        Hospital hospital = new Hospital();
        hospital.setId("HOS001");
        hospital.setName("Old Hospital");
        hospital.setEmail("hospital@gmail.com");
        hospital.setVerified(true);
        hospital.setMustChangePassword(false);

        HospitalProfileDTO request = new HospitalProfileDTO(
                "HOS001",
                "New City Hospital",
                "hospital@gmail.com",
                "0112345678",
                "Colombo",
                "LIC001",
                "Nimal Perera",
                "Colombo 07",
                "profile.jpg",
                true,
                false
        );

        when(hospitalRepository.findByEmail("hospital@gmail.com"))
                .thenReturn(Optional.of(hospital));

        when(hospitalRepository.save(hospital))
                .thenReturn(hospital);

        HospitalProfileDTO result =
                hospitalService.updateMyProfile(
                        "hospital@gmail.com",
                        request
                );

        assertNotNull(result);

        assertEquals(
                "New City Hospital",
                result.getName()
        );

        assertEquals(
                "0112345678",
                result.getContactNumber()
        );

        assertEquals(
                "Colombo",
                result.getAddress()
        );

        assertEquals(
                "Nimal Perera",
                result.getManagerName()
        );

        assertEquals(
                "Colombo 07",
                result.getLocation()
        );

        assertEquals(
                "profile.jpg",
                result.getProfileImage()
        );

        verify(hospitalRepository)
                .save(hospital);
    }
}