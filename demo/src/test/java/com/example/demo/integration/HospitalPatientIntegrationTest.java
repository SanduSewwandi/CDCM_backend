
package com.example.demo.integration;

import com.example.demo.config.SecurityConfig;
import com.example.demo.controller.HospitalPatientController;
import com.example.demo.model.Appointment;
import com.example.demo.model.Doctor;
import com.example.demo.model.Patient;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.DoctorRepository;
import com.example.demo.repository.PatientRepository;
import com.example.demo.security.JwtAuthenticationFilter;
import com.example.demo.security.JwtService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HospitalPatientController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class HospitalPatientIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatientRepository patientRepository;

    @MockitoBean
    private AppointmentRepository appointmentRepository;

    @MockitoBean
    private DoctorRepository doctorRepository;

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

    private Patient createPatient() {
        Patient patient = new Patient();
        patient.setId("patient123");
        patient.setTitle("Mr");
        patient.setFirstName("Amal");
        patient.setLastName("Perera");
        patient.setEmail("amal@example.com");
        patient.setContactNumber("0771234567");
        return patient;
    }

    private Doctor createDoctor() {
        Doctor doctor = new Doctor();
        doctor.setId("doctor123");
        doctor.setTitle("Dr");
        doctor.setFirstName("John");
        doctor.setLastName("Silva");
        doctor.setHospitals(List.of("hospital123"));
        return doctor;
    }

    private Appointment createAppointment() {
        Appointment appointment = new Appointment();
        appointment.setId("appointment123");
        appointment.setPatientId("patient123");
        appointment.setDoctorId("doctor123");
        appointment.setHospitalId("hospital123");
        return appointment;
    }

    // Test 01 - Get All Patients (200 OK)
    // Verify that the hospital can retrieve registered patients.

    @Test
    void getAllPatientsShouldReturn200() throws Exception {
        mockHospitalToken();

        when(patientRepository.findAll())
                .thenReturn(List.of(createPatient()));

        mockMvc.perform(
                        get("/api/hospital/patients")
                                .header("Authorization", "Bearer valid.hospital.token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("patient123"))
                .andExpect(jsonPath("$[0].name").value("Mr Amal Perera"))
                .andExpect(jsonPath("$[0].email").value("amal@example.com"));

        verify(patientRepository).findAll();
    }

    // Test 02 - Get All Patients - Empty List (200 OK)

    @Test
    void getAllPatientsEmptyShouldReturn200() throws Exception {
        mockHospitalToken();

        when(patientRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(
                        get("/api/hospital/patients")
                                .header("Authorization", "Bearer valid.hospital.token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(patientRepository).findAll();
    }

    // Test 03 - Get All Patients - Server Error (500)

    @Test
    void getAllPatientsServerErrorShouldReturn500() throws Exception {
        mockHospitalToken();

        when(patientRepository.findAll())
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(
                        get("/api/hospital/patients")
                                .header("Authorization", "Bearer valid.hospital.token")
                )
                .andExpect(status().isInternalServerError());

        verify(patientRepository).findAll();
    }

    // Test 04 - Get Patient Appointments (200 OK)

    @Test
    void getPatientAppointmentsShouldReturn200() throws Exception {
        mockHospitalToken();

        when(patientRepository.findById("patient123"))
                .thenReturn(Optional.of(createPatient()));

        when(appointmentRepository.findAll())
                .thenReturn(List.of(createAppointment()));

        when(doctorRepository.findById("doctor123"))
                .thenReturn(Optional.of(createDoctor()));

        mockMvc.perform(
                        get("/api/hospital/patients/patient123/appointments")
                                .header("Authorization", "Bearer valid.hospital.token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientName").value("Mr Amal Perera"))
                .andExpect(jsonPath("$.email").value("amal@example.com"))
                .andExpect(jsonPath("$.appointments").isArray())
                .andExpect(jsonPath("$.appointments.length()").value(1))
                .andExpect(jsonPath("$.appointments[0].id")
                        .value("appointment123"))
                .andExpect(jsonPath("$.appointments[0].doctorName")
                        .value("Dr John Silva"));

        verify(patientRepository).findById("patient123");
        verify(appointmentRepository).findAll();
    }

    // Test 05 - Get Patient Appointments - Not Found (404)
    // Verify that an unknown patient returns Not Found.

    @Test
    void getPatientAppointmentsNotFoundShouldReturn404() throws Exception {
        mockHospitalToken();

        when(patientRepository.findById("invalidPatient"))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                        get("/api/hospital/patients/invalidPatient/appointments")
                                .header("Authorization", "Bearer valid.hospital.token")
                )
                .andExpect(status().isNotFound());

        verify(patientRepository).findById("invalidPatient");
        verifyNoInteractions(appointmentRepository);
    }

    // Test 06 - Get Patient Appointments - Server Error (500)

    @Test
    void getPatientAppointmentsServerErrorShouldReturn500() throws Exception {
        mockHospitalToken();

        when(patientRepository.findById("patient123"))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(
                        get("/api/hospital/patients/patient123/appointments")
                                .header("Authorization", "Bearer valid.hospital.token")
                )
                .andExpect(status().isInternalServerError());

        verify(patientRepository).findById("patient123");
    }

    // Test 07 - Get Patients by Hospital (200 OK)
    // Verify that patients with appointments at the hospital are returned.

    @Test
    void getPatientsByHospitalShouldReturn200() throws Exception {
        mockHospitalToken();

        when(doctorRepository.findAll())
                .thenReturn(List.of(createDoctor()));

        when(appointmentRepository.findAll())
                .thenReturn(List.of(createAppointment()));

        when(patientRepository.findById("patient123"))
                .thenReturn(Optional.of(createPatient()));

        when(doctorRepository.findById("doctor123"))
                .thenReturn(Optional.of(createDoctor()));

        mockMvc.perform(
                        get("/api/hospital/patients/hospital/hospital123")
                                .header("Authorization", "Bearer valid.hospital.token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("patient123"))
                .andExpect(jsonPath("$[0].patientName")
                        .value("Mr Amal Perera"))
                .andExpect(jsonPath("$[0].appointments.length()").value(1));

        verify(doctorRepository).findAll();
        verify(patientRepository).findById("patient123");
    }

    // Test 08 - Get Patients by Hospital - Empty List (200 OK)
    // Verify that a hospital without assigned doctors returns no patients.

    @Test
    void getPatientsByHospitalEmptyShouldReturn200() throws Exception {
        mockHospitalToken();

        when(doctorRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(
                        get("/api/hospital/patients/hospital/hospital123")
                                .header("Authorization", "Bearer valid.hospital.token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(doctorRepository).findAll();
        verifyNoInteractions(appointmentRepository, patientRepository);
    }

    // Test 09 - Get Patients by Hospital - Server Error (500)
    // Verify that a repository failure returns Internal Server Error.

    @Test
    void getPatientsByHospitalServerErrorShouldReturn500() throws Exception {
        mockHospitalToken();

        when(doctorRepository.findAll())
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(
                        get("/api/hospital/patients/hospital/hospital123")
                                .header("Authorization", "Bearer valid.hospital.token")
                )
                .andExpect(status().isInternalServerError());

        verify(doctorRepository).findAll();
    }
}
