
package com.example.demo.integration;

import com.example.demo.config.SecurityConfig;
import com.example.demo.controller.PatientController;
import com.example.demo.model.Patient;
import com.example.demo.security.JwtAuthenticationFilter;
import com.example.demo.security.JwtService;
import com.example.demo.service.PatientService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PatientController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class PatientIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatientService patientService;

    @MockitoBean
    private JwtService jwtService;

    private void mockValidPatientToken() {
        String token = "valid.patient.token";

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token))
                .thenReturn("patient@example.com");
        when(jwtService.extractRole(token))
                .thenReturn("PATIENT");
    }


    @Test
    void getPatientProfileShouldReturn200() throws Exception {

        mockValidPatientToken();

        Patient patient = new Patient();

        when(patientService.getPatientById("patient123"))
                .thenReturn(patient);

        mockMvc.perform(
                        get("/api/auth/patients/patient123")
                                .header("Authorization",
                                        "Bearer valid.patient.token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap());

        verify(patientService).getPatientById("patient123");
    }

    @Test
    void getPatientProfileNullShouldReturn200() throws Exception {

        mockValidPatientToken();

        when(patientService.getPatientById("invalidPatient"))
                .thenReturn(null);

        mockMvc.perform(
                        get("/api/auth/patients/invalidPatient")
                                .header("Authorization",
                                        "Bearer valid.patient.token")
                )
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(patientService).getPatientById("invalidPatient");
    }

    @Test
    void updatePatientProfileShouldReturn200() throws Exception {

        mockValidPatientToken();

        Patient updatedPatient = new Patient();

        when(patientService.updatePatient(
                eq("patient123"), any(Patient.class)))
                .thenReturn(updatedPatient);

        mockMvc.perform(
                        put("/api/auth/patients/patient123")
                                .header("Authorization",
                                        "Bearer valid.patient.token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap());

        verify(patientService).updatePatient(
                eq("patient123"), any(Patient.class));
    }

    @Test
    void updatePatientProfileNullShouldReturn200() throws Exception {

        mockValidPatientToken();

        when(patientService.updatePatient(
                eq("invalidPatient"), any(Patient.class)))
                .thenReturn(null);

        mockMvc.perform(
                        put("/api/auth/patients/invalidPatient")
                                .header("Authorization",
                                        "Bearer valid.patient.token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(patientService).updatePatient(
                eq("invalidPatient"), any(Patient.class));
    }


    @Test
    void updatePatientProfileInvalidJsonShouldReturn500() throws Exception {

        mockValidPatientToken();

        mockMvc.perform(
                        put("/api/auth/patients/patient123")
                                .header("Authorization",
                                        "Bearer valid.patient.token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{invalid-json}")
                )
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(
                        org.hamcrest.Matchers.startsWith("Server error:")
                ));

        verifyNoInteractions(patientService);
    }



    @Test
    void updatePatientProfileUnsupportedMediaTypeShouldReturn500()
            throws Exception {

        mockValidPatientToken();

        mockMvc.perform(
                        put("/api/auth/patients/patient123")
                                .header("Authorization",
                                        "Bearer valid.patient.token")
                                .contentType(MediaType.TEXT_PLAIN)
                                .content("invalid patient data")
                )
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(
                        org.hamcrest.Matchers.startsWith("Server error:")
                ));

        verifyNoInteractions(patientService);
    }

}
