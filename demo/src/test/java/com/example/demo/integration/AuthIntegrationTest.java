
package com.example.demo.integration;

import com.example.demo.config.SecurityConfig;
import com.example.demo.controller.AuthController;
import com.example.demo.security.JwtAuthenticationFilter;
import com.example.demo.security.JwtService;
import com.example.demo.service.AdminService;
import com.example.demo.service.DoctorService;
import com.example.demo.service.EmailService;
import com.example.demo.service.HospitalService;
import com.example.demo.service.PatientService;
import com.example.demo.controller.DoctorController;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({
        AuthController.class,
        DoctorController.class
})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private DoctorService doctorService;

    @MockitoBean
    private HospitalService hospitalService;

    @MockitoBean
    private PatientService patientService;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void loginEndpointShouldBeAccessibleWithoutJwtToken() throws Exception {

        String requestBody = """
                {
                    "email": "unknown@example.com",
                    "password": "WrongPassword123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Invalid Email or Password"));
    }


    @Test
    void doctorEndpointShouldRejectRequestWithoutJwtToken() throws Exception {

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .get("/api/doctor/test-protected")
                )
                .andExpect(status().isForbidden());
    }


    @Test
    void doctorEndpointShouldRejectInvalidJwtToken() throws Exception {

        org.mockito.Mockito.when(
                jwtService.isTokenValid("invalid.jwt.token")
        ).thenReturn(false);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .get("/api/doctor/test-protected")
                                .header("Authorization",
                                        "Bearer invalid.jwt.token")
                )
                .andExpect(status().isForbidden());

        org.mockito.Mockito.verify(jwtService)
                .isTokenValid("invalid.jwt.token");
    }


    @Test
    void doctorEndpointShouldAllowValidDoctorJwtToken() throws Exception {

        String token = "valid.doctor.token";

        org.mockito.Mockito.when(
                jwtService.isTokenValid(token)
        ).thenReturn(true);

        org.mockito.Mockito.when(
                jwtService.extractEmail(token)
        ).thenReturn("doctor@example.com");

        org.mockito.Mockito.when(
                jwtService.extractRole(token)
        ).thenReturn("DOCTOR");

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .get("/api/doctor/test-protected")
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(result ->
                        org.junit.jupiter.api.Assertions.assertNotEquals(
                                403,
                                result.getResponse().getStatus()
                        )
                );

        org.mockito.Mockito.verify(jwtService)
                .isTokenValid(token);

        org.mockito.Mockito.verify(jwtService)
                .extractEmail(token);

        org.mockito.Mockito.verify(jwtService)
                .extractRole(token);
    }

    @Test
    void doctorJwtShouldNotAccessAdminEndpoint() throws Exception {

        String token = "valid.doctor.token";

        org.mockito.Mockito.when(
                jwtService.isTokenValid(token)
        ).thenReturn(true);

        org.mockito.Mockito.when(
                jwtService.extractEmail(token)
        ).thenReturn("doctor@example.com");

        org.mockito.Mockito.when(
                jwtService.extractRole(token)
        ).thenReturn("DOCTOR");

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .get("/api/admin/test-protected")
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isForbidden());

        org.mockito.Mockito.verify(jwtService)
                .extractRole(token);
    }


    @Test
    void getDoctorProfileShouldReturn200() throws Exception {

        String doctorId = "doctor123";

        com.example.demo.model.Doctor doctor =
                new com.example.demo.model.Doctor();

        doctor.setId(doctorId);
        doctor.setFirstName("Kamal");
        doctor.setLastName("Perera");

        org.mockito.Mockito.when(
                doctorService.getDoctorById(doctorId)
        ).thenReturn(doctor);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .get("/api/auth/doctors/" + doctorId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(doctorId))
                .andExpect(jsonPath("$.firstName").value("Kamal"))
                .andExpect(jsonPath("$.lastName").value("Perera"));

        org.mockito.Mockito.verify(doctorService)
                .getDoctorById(doctorId);
    }


    @Test
    void doctorProfileEndpointShouldBeAccessibleWithoutJwt() throws Exception {

        String doctorId = "doctor456";

        com.example.demo.model.Doctor doctor =
                new com.example.demo.model.Doctor();

        doctor.setId(doctorId);
        doctor.setFirstName("Nimal");
        doctor.setLastName("Silva");

        org.mockito.Mockito.when(
                doctorService.getDoctorById(doctorId)
        ).thenReturn(doctor);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .get("/api/auth/doctors/" + doctorId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(doctorId))
                .andExpect(jsonPath("$.firstName").value("Nimal"));

        org.mockito.Mockito.verify(doctorService)
                .getDoctorById(doctorId);
    }









}
