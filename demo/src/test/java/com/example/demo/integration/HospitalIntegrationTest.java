
package com.example.demo.integration;

import com.example.demo.config.SecurityConfig;
import com.example.demo.controller.HospitalController;
import com.example.demo.model.Hospital;
import com.example.demo.security.JwtAuthenticationFilter;
import com.example.demo.security.JwtService;
import com.example.demo.service.HospitalService;
import com.example.demo.service.HospitalAnalyticsService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HospitalController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class HospitalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HospitalService hospitalService;

    @MockitoBean
    private HospitalAnalyticsService hospitalAnalyticsService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void getAllHospitalsWithValidJwtShouldReturn200() throws Exception {

        String token = "valid.hospital.token";

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token))
                .thenReturn("hospital@example.com");
        when(jwtService.extractRole(token))
                .thenReturn("HOSPITAL");

        when(hospitalService.getAllHospitals())
                .thenReturn(Collections.emptyList());

        mockMvc.perform(
                        get("/api/hospitals/all")
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(hospitalService).getAllHospitals();
    }

    @Test
    void getHospitalByIdShouldReturn200() throws Exception {

        String hospitalId = "hospital123";
        String token = "valid.hospital.token";

        Hospital hospital = new Hospital();
        hospital.setId(hospitalId);
        hospital.setName("Test Hospital");

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token))
                .thenReturn("hospital@example.com");
        when(jwtService.extractRole(token))
                .thenReturn("HOSPITAL");

        when(hospitalService.getHospitalById(hospitalId))
                .thenReturn(hospital);

        mockMvc.perform(
                        get("/api/hospitals/" + hospitalId)
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(hospitalId))
                .andExpect(jsonPath("$.name").value("Test Hospital"));

        verify(hospitalService).getHospitalById(hospitalId);
    }

    @Test
    void getHospitalByIdNotFoundShouldReturn404() throws Exception {

        String hospitalId = "invalidHospital123";
        String token = "valid.hospital.token";

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token))
                .thenReturn("hospital@example.com");
        when(jwtService.extractRole(token))
                .thenReturn("HOSPITAL");

        when(hospitalService.getHospitalById(hospitalId))
                .thenReturn(null);

        mockMvc.perform(
                        get("/api/hospitals/" + hospitalId)
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isNotFound());

        verify(hospitalService).getHospitalById(hospitalId);
    }

    @Test
    void getHospitalByIdServerErrorShouldReturn500() throws Exception {

        String hospitalId = "hospital123";
        String token = "valid.hospital.token";

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token))
                .thenReturn("hospital@example.com");
        when(jwtService.extractRole(token))
                .thenReturn("HOSPITAL");

        when(hospitalService.getHospitalById(hospitalId))
                .thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(
                        get("/api/hospitals/" + hospitalId)
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isInternalServerError());

        verify(hospitalService).getHospitalById(hospitalId);
    }

    @Test
    void getAllHospitalsServerErrorShouldReturn500() throws Exception {

        String token = "valid.hospital.token";

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token))
                .thenReturn("hospital@example.com");
        when(jwtService.extractRole(token))
                .thenReturn("HOSPITAL");

        when(hospitalService.getAllHospitals())
                .thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(
                        get("/api/hospitals/all")
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isInternalServerError());

        verify(hospitalService).getAllHospitals();
    }


    @Test
    void getHospitalProfileWithoutAuthenticationShouldReturn403() throws Exception {

        mockMvc.perform(
                        get("/api/hospitals/me")
                )
                .andExpect(status().isForbidden());

        org.mockito.Mockito.verifyNoInteractions(hospitalService);
    }


    @Test
    void getHospitalProfileWithValidAuthenticationShouldReturn200() throws Exception {

        String token = "valid.hospital.token";
        String email = "hospital@example.com";

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token)).thenReturn(email);
        when(jwtService.extractRole(token)).thenReturn("HOSPITAL");

        com.example.demo.dto.HospitalProfileDTO profile =
                new com.example.demo.dto.HospitalProfileDTO(
                        "hospital123",
                        "Test Hospital",
                        email,
                        "0771234567",
                        "Colombo",
                        "LIC123",
                        "Test Manager",
                        null,
                        null,
                        true,
                        false
                );

        when(hospitalService.getMyProfile(email))
                .thenReturn(profile);

        mockMvc.perform(
                        get("/api/hospitals/me")
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Hospital"))
                .andExpect(jsonPath("$.email").value(email));

        verify(hospitalService).getMyProfile(email);
    }

    @Test
    void getHospitalAnalyticsShouldReturn200() throws Exception {

        String hospitalId = "hospital123";
        String token = "valid.hospital.token";

        java.util.Map<String, Object> analytics =
                new java.util.HashMap<>();

        analytics.put("totalAppointments", 150);
        analytics.put("totalPatients", 80);
        analytics.put("totalDoctors", 12);

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token))
                .thenReturn("hospital@example.com");
        when(jwtService.extractRole(token))
                .thenReturn("HOSPITAL");

        when(hospitalAnalyticsService.getHospitalAnalytics(hospitalId))
                .thenReturn(analytics);

        mockMvc.perform(
                        get("/api/hospitals/" + hospitalId + "/analytics")
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAppointments").value(150))
                .andExpect(jsonPath("$.totalPatients").value(80))
                .andExpect(jsonPath("$.totalDoctors").value(12));

        verify(hospitalAnalyticsService)
                .getHospitalAnalytics(hospitalId);
    }

    @Test
    void getHospitalAnalyticsServerErrorShouldReturn500() throws Exception {

        String hospitalId = "hospital123";
        String token = "valid.hospital.token";

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token))
                .thenReturn("hospital@example.com");
        when(jwtService.extractRole(token))
                .thenReturn("HOSPITAL");

        when(hospitalAnalyticsService.getHospitalAnalytics(hospitalId))
                .thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(
                        get("/api/hospitals/" + hospitalId + "/analytics")
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("Failed to load analytics: Database connection failed"));

        verify(hospitalAnalyticsService)
                .getHospitalAnalytics(hospitalId);
    }

    @Test
    void requestHospitalVerificationCodeShouldReturn200() throws Exception {

        String token = "valid.hospital.token";
        String email = "hospital@example.com";

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token)).thenReturn(email);
        when(jwtService.extractRole(token)).thenReturn("HOSPITAL");

        when(hospitalService.requestVerificationCode(email))
                .thenReturn("Verification code sent successfully");

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .post("/api/hospitals/request-verification-code")
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Verification code sent successfully"));

        verify(hospitalService).requestVerificationCode(email);
    }


}
