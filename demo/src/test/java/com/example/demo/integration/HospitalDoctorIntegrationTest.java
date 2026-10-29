
package com.example.demo.integration;

import com.example.demo.config.SecurityConfig;
import com.example.demo.controller.HospitalDoctorController;
import com.example.demo.model.Doctor;
import com.example.demo.security.JwtAuthenticationFilter;
import com.example.demo.security.JwtService;
import com.example.demo.service.DoctorService;
import com.example.demo.service.HospitalService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HospitalDoctorController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class HospitalDoctorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DoctorService doctorService;

    @MockitoBean
    private HospitalService hospitalService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void searchRegisteredDoctorsShouldReturn200() throws Exception {

        String token = "valid.hospital.token";
        String keyword = "Cardiology";

        Doctor doctor = new Doctor();
        doctor.setFirstName("John");
        doctor.setLastName("Perera");
        doctor.setSpecialization("Cardiology");

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token))
                .thenReturn("hospital@example.com");
        when(jwtService.extractRole(token))
                .thenReturn("HOSPITAL");

        when(doctorService.searchDoctors(keyword))
                .thenReturn(List.of(doctor));

        mockMvc.perform(
                        get("/api/hospital/doctors/search")
                                .param("keyword", keyword)
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[0].specialization").value("Cardiology"));

        verify(doctorService).searchDoctors(keyword);
    }


    @Test
    void searchDoctorsWithoutKeywordShouldReturn200() throws Exception {

        String token = "valid.hospital.token";

        Doctor doctor1 = new Doctor();
        doctor1.setFirstName("John");
        doctor1.setLastName("Perera");
        doctor1.setSpecialization("Cardiology");

        Doctor doctor2 = new Doctor();
        doctor2.setFirstName("Amal");
        doctor2.setLastName("Silva");
        doctor2.setSpecialization("Neurology");

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token))
                .thenReturn("hospital@example.com");
        when(jwtService.extractRole(token))
                .thenReturn("HOSPITAL");

        when(doctorService.searchDoctors(null))
                .thenReturn(List.of(doctor1, doctor2));

        mockMvc.perform(
                        get("/api/hospital/doctors/search")
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[0].specialization").value("Cardiology"))
                .andExpect(jsonPath("$[1].firstName").value("Amal"))
                .andExpect(jsonPath("$[1].specialization").value("Neurology"));

        verify(doctorService).searchDoctors(null);
    }

    @Test
    void searchDoctorsServerErrorShouldReturn500() throws Exception {

        String token = "valid.hospital.token";
        String keyword = "Cardiology";

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token))
                .thenReturn("hospital@example.com");
        when(jwtService.extractRole(token))
                .thenReturn("HOSPITAL");

        when(doctorService.searchDoctors(keyword))
                .thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(
                        get("/api/hospital/doctors/search")
                                .param("keyword", keyword)
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(doctorService).searchDoctors(keyword);
    }

    @Test
    void assignDoctorToHospitalShouldReturn200() throws Exception {

        String token = "valid.hospital.token";
        String doctorId = "doctor123";
        String hospitalId = "hospital123";

        Doctor doctor = new Doctor();
        doctor.setId(doctorId);
        doctor.setFirstName("John");
        doctor.setLastName("Perera");
        doctor.setSpecialization("Cardiology");
        doctor.setHospitals(List.of(hospitalId));

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token))
                .thenReturn("hospital@example.com");
        when(jwtService.extractRole(token))
                .thenReturn("HOSPITAL");

        when(doctorService.assignDoctorToHospital(doctorId, hospitalId))
                .thenReturn(doctor);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .put("/api/hospital/doctors/"
                                        + doctorId + "/assign/" + hospitalId)
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(doctorId))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.specialization").value("Cardiology"))
                .andExpect(jsonPath("$.hospitals[0]").value(hospitalId));

        verify(doctorService)
                .assignDoctorToHospital(doctorId, hospitalId);
    }

    @Test
    void assignDoctorToHospitalNotFoundShouldReturn404() throws Exception {

        String token = "valid.hospital.token";
        String doctorId = "invalidDoctor123";
        String hospitalId = "hospital123";

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token))
                .thenReturn("hospital@example.com");
        when(jwtService.extractRole(token))
                .thenReturn("HOSPITAL");

        when(doctorService.assignDoctorToHospital(doctorId, hospitalId))
                .thenReturn(null);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .put("/api/hospital/doctors/"
                                        + doctorId + "/assign/" + hospitalId)
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Doctor or Hospital not found"));

        verify(doctorService)
                .assignDoctorToHospital(doctorId, hospitalId);
    }

    @Test
    void assignDoctorToHospitalServerErrorShouldReturn500() throws Exception {

        String token = "valid.hospital.token";
        String doctorId = "doctor123";
        String hospitalId = "hospital123";

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token))
                .thenReturn("hospital@example.com");
        when(jwtService.extractRole(token))
                .thenReturn("HOSPITAL");

        when(doctorService.assignDoctorToHospital(doctorId, hospitalId))
                .thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .put("/api/hospital/doctors/"
                                        + doctorId + "/assign/" + hospitalId)
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("Backend error: Database connection failed"));

        verify(doctorService)
                .assignDoctorToHospital(doctorId, hospitalId);
    }

    @Test
    void getDoctorsAssignedToHospitalShouldReturn200() throws Exception {

        String token = "valid.hospital.token";
        String hospitalId = "hospital123";

        Doctor doctor1 = new Doctor();
        doctor1.setId("doctor123");
        doctor1.setFirstName("John");
        doctor1.setLastName("Perera");
        doctor1.setSpecialization("Cardiology");
        doctor1.setHospitals(List.of(hospitalId));

        Doctor doctor2 = new Doctor();
        doctor2.setId("doctor456");
        doctor2.setFirstName("Amal");
        doctor2.setLastName("Silva");
        doctor2.setSpecialization("Neurology");
        doctor2.setHospitals(List.of(hospitalId));

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token))
                .thenReturn("hospital@example.com");
        when(jwtService.extractRole(token))
                .thenReturn("HOSPITAL");

        when(doctorService.getDoctorsByHospital(hospitalId))
                .thenReturn(List.of(doctor1, doctor2));

        mockMvc.perform(
                        get("/api/hospital/doctors/hospital/" + hospitalId)
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("doctor123"))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[0].specialization").value("Cardiology"))
                .andExpect(jsonPath("$[1].id").value("doctor456"))
                .andExpect(jsonPath("$[1].firstName").value("Amal"))
                .andExpect(jsonPath("$[1].specialization").value("Neurology"));

        verify(doctorService).getDoctorsByHospital(hospitalId);
    }

    @Test
    void getHospitalDoctorsServerErrorShouldReturn500() throws Exception {

        String token = "valid.hospital.token";
        String hospitalId = "hospital123";

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token))
                .thenReturn("hospital@example.com");
        when(jwtService.extractRole(token))
                .thenReturn("HOSPITAL");

        when(doctorService.getDoctorsByHospital(hospitalId))
                .thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(
                        get("/api/hospital/doctors/hospital/" + hospitalId)
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("Backend error: Database connection failed"));

        verify(doctorService).getDoctorsByHospital(hospitalId);
    }

    @Test
    void removeDoctorFromHospitalShouldReturn200() throws Exception {

        String token = "valid.hospital.token";
        String doctorId = "doctor123";
        String hospitalId = "hospital123";

        Doctor doctor = new Doctor();
        doctor.setId(doctorId);
        doctor.setFirstName("John");
        doctor.setLastName("Perera");
        doctor.setSpecialization("Cardiology");
        doctor.setHospitals(List.of());

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token))
                .thenReturn("hospital@example.com");
        when(jwtService.extractRole(token))
                .thenReturn("HOSPITAL");

        when(doctorService.removeDoctorFromHospital(doctorId, hospitalId))
                .thenReturn(doctor);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .delete("/api/hospital/doctors/"
                                        + doctorId + "/remove/" + hospitalId)
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(doctorId))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.specialization").value("Cardiology"))
                .andExpect(jsonPath("$.hospitals").isArray())
                .andExpect(jsonPath("$.hospitals.length()").value(0));

        verify(doctorService)
                .removeDoctorFromHospital(doctorId, hospitalId);
    }

    @Test
    void removeDoctorFromHospitalNotFoundShouldReturn404() throws Exception {

        String token = "valid.hospital.token";
        String doctorId = "invalidDoctor123";
        String hospitalId = "hospital123";

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token))
                .thenReturn("hospital@example.com");
        when(jwtService.extractRole(token))
                .thenReturn("HOSPITAL");

        when(doctorService.removeDoctorFromHospital(doctorId, hospitalId))
                .thenReturn(null);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .delete("/api/hospital/doctors/"
                                        + doctorId + "/remove/" + hospitalId)
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("Doctor or Hospital not found"));

        verify(doctorService)
                .removeDoctorFromHospital(doctorId, hospitalId);
    }

    @Test
    void removeDoctorFromHospitalServerErrorShouldReturn500() throws Exception {

        String token = "valid.hospital.token";
        String doctorId = "doctor123";
        String hospitalId = "hospital123";

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token))
                .thenReturn("hospital@example.com");
        when(jwtService.extractRole(token))
                .thenReturn("HOSPITAL");

        when(doctorService.removeDoctorFromHospital(doctorId, hospitalId))
                .thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .delete("/api/hospital/doctors/"
                                        + doctorId + "/remove/" + hospitalId)
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("Backend error: Database connection failed"));

        verify(doctorService)
                .removeDoctorFromHospital(doctorId, hospitalId);
    }

    @Test
    void getAllAssignedDoctorsShouldReturn200() throws Exception {

        String token = "valid.hospital.token";

        Doctor doctor1 = new Doctor();
        doctor1.setId("doctor123");
        doctor1.setFirstName("John");
        doctor1.setLastName("Perera");
        doctor1.setSpecialization("Cardiology");
        doctor1.setHospitals(List.of("hospital123"));

        Doctor doctor2 = new Doctor();
        doctor2.setId("doctor456");
        doctor2.setFirstName("Amal");
        doctor2.setLastName("Silva");
        doctor2.setSpecialization("Neurology");
        doctor2.setHospitals(List.of("hospital456"));

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token))
                .thenReturn("hospital@example.com");
        when(jwtService.extractRole(token))
                .thenReturn("HOSPITAL");

        when(doctorService.getAllAssignedDoctors())
                .thenReturn(List.of(doctor1, doctor2));

        mockMvc.perform(
                        get("/api/hospital/doctors/assigned-all")
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("doctor123"))
                .andExpect(jsonPath("$[0].firstName").value("John"))
                .andExpect(jsonPath("$[0].specialization").value("Cardiology"))
                .andExpect(jsonPath("$[0].hospitals[0]").value("hospital123"))
                .andExpect(jsonPath("$[1].id").value("doctor456"))
                .andExpect(jsonPath("$[1].firstName").value("Amal"))
                .andExpect(jsonPath("$[1].specialization").value("Neurology"))
                .andExpect(jsonPath("$[1].hospitals[0]").value("hospital456"));

        verify(doctorService).getAllAssignedDoctors();
    }

    @Test
    void getAllAssignedDoctorsServerErrorShouldReturn500() throws Exception {

        String token = "valid.hospital.token";

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token))
                .thenReturn("hospital@example.com");
        when(jwtService.extractRole(token))
                .thenReturn("HOSPITAL");

        when(doctorService.getAllAssignedDoctors())
                .thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(
                        get("/api/hospital/doctors/assigned-all")
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(doctorService).getAllAssignedDoctors();
    }

    @Test
    void getDoctorSpecializationsShouldReturn200() throws Exception {

        String token = "valid.hospital.token";

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token))
                .thenReturn("hospital@example.com");
        when(jwtService.extractRole(token))
                .thenReturn("HOSPITAL");

        when(doctorService.getUniqueSpecializations())
                .thenReturn(List.of(
                        "Cardiology",
                        "Neurology",
                        "Dermatology"
                ));

        mockMvc.perform(
                        get("/api/hospital/doctors/specializations")
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0]").value("Cardiology"))
                .andExpect(jsonPath("$[1]").value("Neurology"))
                .andExpect(jsonPath("$[2]").value("Dermatology"));

        verify(doctorService).getUniqueSpecializations();
    }

}
