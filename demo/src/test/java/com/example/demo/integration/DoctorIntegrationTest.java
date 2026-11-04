
package com.example.demo.integration;

import com.example.demo.config.SecurityConfig;
import com.example.demo.controller.DoctorController;
import com.example.demo.dto.DoctorAccountDTO;
import com.example.demo.model.Doctor;
import com.example.demo.security.JwtAuthenticationFilter;
import com.example.demo.security.JwtService;
import com.example.demo.service.DoctorService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DoctorController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class DoctorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DoctorService doctorService;

    @MockitoBean
    private JwtService jwtService;

    private void mockValidDoctorToken() {

        String token = "valid.doctor.token";

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token))
                .thenReturn("doctor@example.com");
        when(jwtService.extractRole(token))
                .thenReturn("DOCTOR");
    }

    @Test
    void getDoctorProfileShouldReturn200() throws Exception {

        mockValidDoctorToken();

        Doctor doctor = new Doctor();
        doctor.setId("doctor123");
        doctor.setFirstName("John");
        doctor.setLastName("Perera");
        doctor.setSpecialization("Cardiology");

        when(doctorService.getDoctorById("doctor123"))
                .thenReturn(doctor);

        mockMvc.perform(
                        get("/api/auth/doctors/doctor123")
                                .header("Authorization", "Bearer valid.doctor.token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("doctor123"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.specialization").value("Cardiology"));

        verify(doctorService).getDoctorById("doctor123");
    }

    // Test 02 - Get Doctor Profile

    @Test
    void getDoctorProfileNullShouldReturn200() throws Exception {

        mockValidDoctorToken();

        when(doctorService.getDoctorById("invalidDoctor"))
                .thenReturn(null);

        mockMvc.perform(
                        get("/api/auth/doctors/invalidDoctor")
                                .header("Authorization", "Bearer valid.doctor.token")
                )
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(doctorService).getDoctorById("invalidDoctor");
    }

    // Verify that a doctor profile update returns the updated doctor.

    @Test
    void updateDoctorProfileShouldReturn200() throws Exception {

        mockValidDoctorToken();

        Doctor doctor = new Doctor();
        doctor.setId("doctor123");
        doctor.setFirstName("John");
        doctor.setLastName("Perera");

        when(doctorService.updateDoctor(
                eq("doctor123"), any(Doctor.class)))
                .thenReturn(doctor);

        mockMvc.perform(
                        put("/api/auth/doctors/doctor123")
                                .header("Authorization", "Bearer valid.doctor.token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"firstName\":\"John\",\"lastName\":\"Perera\"}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("doctor123"))
                .andExpect(jsonPath("$.firstName").value("John"));

        verify(doctorService).updateDoctor(
                eq("doctor123"), any(Doctor.class));
    }

    // Test 04 - Update Doctor Profile - Null Result (200 OK)

    @Test
    void updateDoctorProfileNullShouldReturn200() throws Exception {

        mockValidDoctorToken();

        when(doctorService.updateDoctor(
                eq("invalidDoctor"), any(Doctor.class)))
                .thenReturn(null);

        mockMvc.perform(
                        put("/api/auth/doctors/invalidDoctor")
                                .header("Authorization", "Bearer valid.doctor.token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(doctorService).updateDoctor(
                eq("invalidDoctor"), any(Doctor.class));
    }

    // Verify that doctor account details are returned successfully.

    @Test
    void getDoctorAccountShouldReturn200() throws Exception {

        mockValidDoctorToken();

        DoctorAccountDTO account = new DoctorAccountDTO();

        when(doctorService.getDoctorAccount("doctor123"))
                .thenReturn(account);

        mockMvc.perform(
                        get("/api/auth/doctors/doctor123/account")
                                .header("Authorization", "Bearer valid.doctor.token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap());

        verify(doctorService).getDoctorAccount("doctor123");
    }

    // Test 06 - Update Doctor Account (200 OK)

    @Test
    void updateDoctorAccountShouldReturn200() throws Exception {

        mockValidDoctorToken();

        DoctorAccountDTO account = new DoctorAccountDTO();

        when(doctorService.updateDoctorAccount(
                eq("doctor123"), any(DoctorAccountDTO.class)))
                .thenReturn(account);

        mockMvc.perform(
                        put("/api/auth/doctors/doctor123/account")
                                .header("Authorization", "Bearer valid.doctor.token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap());

        verify(doctorService).updateDoctorAccount(
                eq("doctor123"), any(DoctorAccountDTO.class));
    }

    // Test 07 - Get All Doctors (200 OK)

    @Test
    void getAllDoctorsShouldReturn200() throws Exception {

        mockValidDoctorToken();

        Doctor doctor1 = new Doctor();
        doctor1.setId("doctor123");
        doctor1.setFirstName("John");
        doctor1.setLastName("Perera");
        doctor1.setSpecialization("Cardiology");

        Doctor doctor2 = new Doctor();
        doctor2.setId("doctor456");
        doctor2.setFirstName("Amal");
        doctor2.setLastName("Silva");
        doctor2.setSpecialization("Neurology");

        when(doctorService.getAllDoctors())
                .thenReturn(List.of(doctor1, doctor2));

        mockMvc.perform(
                        get("/api/auth/doctors")
                                .header("Authorization", "Bearer valid.doctor.token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("doctor123"))
                .andExpect(jsonPath("$[0].fullName").value("John Perera"))
                .andExpect(jsonPath("$[0].specialization").value("Cardiology"))
                .andExpect(jsonPath("$[1].id").value("doctor456"))
                .andExpect(jsonPath("$[1].fullName").value("Amal Silva"))
                .andExpect(jsonPath("$[1].specialization").value("Neurology"));

        verify(doctorService).getAllDoctors();
    }

    // Test 08 - Get All Doctors - Empty List (200 OK)
    // Verify that an empty doctor list returns 200 OK.

    @Test
    void getAllDoctorsEmptyShouldReturn200() throws Exception {

        mockValidDoctorToken();

        when(doctorService.getAllDoctors())
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/auth/doctors")
                                .header("Authorization", "Bearer valid.doctor.token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(doctorService).getAllDoctors();
    }

    // Test 09 - Get Doctors by Hospital (200 OK)
    // Verify that doctors assigned to a hospital are returned successfully.

    @Test
    void getDoctorsByHospitalShouldReturn200() throws Exception {

        mockValidDoctorToken();

        String hospitalId = "hospital123";

        Doctor doctor = new Doctor();
        doctor.setId("doctor123");
        doctor.setFirstName("John");
        doctor.setLastName("Perera");
        doctor.setSpecialization("Cardiology");

        when(doctorService.getDoctorsByHospital(hospitalId))
                .thenReturn(List.of(doctor));

        mockMvc.perform(
                        get("/api/auth/doctors/hospital/" + hospitalId)
                                .header("Authorization", "Bearer valid.doctor.token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("doctor123"))
                .andExpect(jsonPath("$[0].fullName").value("John Perera"))
                .andExpect(jsonPath("$[0].specialization").value("Cardiology"));

        verify(doctorService).getDoctorsByHospital(hospitalId);
    }

    // Test 10 - Get Doctors by Hospital - Empty List (200 OK)
    // Verify that a hospital with no assigned doctors returns an empty list.

    @Test
    void getDoctorsByHospitalEmptyShouldReturn200() throws Exception {

        mockValidDoctorToken();

        String hospitalId = "hospital123";

        when(doctorService.getDoctorsByHospital(hospitalId))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/auth/doctors/hospital/" + hospitalId)
                                .header("Authorization", "Bearer valid.doctor.token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(doctorService).getDoctorsByHospital(hospitalId);
    }
}
