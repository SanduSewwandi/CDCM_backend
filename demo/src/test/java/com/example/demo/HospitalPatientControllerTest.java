package com.example.demo;

import com.example.demo.controller.HospitalPatientController;
import com.example.demo.dto.PatientWithAppointmentDTO;
import com.example.demo.model.Appointment;
import com.example.demo.model.Doctor;
import com.example.demo.model.Patient;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.DoctorRepository;
import com.example.demo.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HospitalPatientControllerTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @InjectMocks
    private HospitalPatientController controller;

    private Doctor doctorA;
    private Patient patient1;
    private Patient patient2;

    @BeforeEach
    void setUp() {
        // Doctor associated with both Hospital A and Hospital B
        doctorA = new Doctor();
        doctorA.setId("docA");
        doctorA.setTitle("Dr.");
        doctorA.setFirstName("John");
        doctorA.setLastName("Smith");
        doctorA.setHospitals(new ArrayList<>(List.of("hospA", "hospB")));

        // Patient 1
        patient1 = new Patient();
        patient1.setId("pat1");
        patient1.setTitle("Mr.");
        patient1.setFirstName("Alice");
        patient1.setLastName("Walker");
        patient1.setEmail("alice@test.com");
        patient1.setContactNumber("0771234567");
        patient1.setNicOrPassport("199012345678");

        // Patient 2
        patient2 = new Patient();
        patient2.setId("pat2");
        patient2.setTitle("Ms.");
        patient2.setFirstName("Bob");
        patient2.setLastName("Taylor");
        patient2.setEmail("bob@test.com");
        patient2.setContactNumber("0777654321");
        patient2.setNicOrPassport("199512345678");
    }

    @Test
    @DisplayName("Appointments booked at Hospital A are included for Hospital A and excluded for Hospital B")
    void testHospitalAIncludesOnlyHospitalAAppointments() {
        Appointment apptAtHospA = new Appointment();
        apptAtHospA.setId("appt-1");
        apptAtHospA.setAppointmentNumber("APT-001");
        apptAtHospA.setDoctorId("docA");
        apptAtHospA.setHospitalId("hospA");
        apptAtHospA.setPatientId("pat1");
        apptAtHospA.setDate("2026-10-30");
        apptAtHospA.setTime("09:00 - 10:00");
        apptAtHospA.setStatus("CONFIRMED");

        Appointment apptAtHospB = new Appointment();
        apptAtHospB.setId("appt-2");
        apptAtHospB.setAppointmentNumber("APT-002");
        apptAtHospB.setDoctorId("docA");
        apptAtHospB.setHospitalId("hospB");
        apptAtHospB.setPatientId("pat2");
        apptAtHospB.setDate("2026-10-31");
        apptAtHospB.setTime("14:00 - 15:00");
        apptAtHospB.setStatus("CONFIRMED");

        when(doctorRepository.findAll()).thenReturn(List.of(doctorA));
        when(appointmentRepository.findAll()).thenReturn(List.of(apptAtHospA, apptAtHospB));
        when(patientRepository.findById("pat1")).thenReturn(Optional.of(patient1));

        // When Hospital A requests patients
        ResponseEntity<List<PatientWithAppointmentDTO>> responseA = controller.getPatientsByHospital("hospA");

        assertNotNull(responseA);
        assertEquals(200, responseA.getStatusCode().value());
        List<PatientWithAppointmentDTO> patientsA = responseA.getBody();
        assertNotNull(patientsA);
        // Only Patient 1 who has an appointment at Hospital A should be included
        assertEquals(1, patientsA.size());
        assertEquals("pat1", patientsA.get(0).getId());
        assertEquals(1, patientsA.get(0).getAppointments().size());
        assertEquals("appt-1", patientsA.get(0).getAppointments().get(0).getId());
    }

    @Test
    @DisplayName("Appointments booked at Hospital B are included for Hospital B and excluded for Hospital A")
    void testHospitalBIncludesOnlyHospitalBAppointments() {
        Appointment apptAtHospA = new Appointment();
        apptAtHospA.setId("appt-1");
        apptAtHospA.setAppointmentNumber("APT-001");
        apptAtHospA.setDoctorId("docA");
        apptAtHospA.setHospitalId("hospA");
        apptAtHospA.setPatientId("pat1");
        apptAtHospA.setStatus("CONFIRMED");

        Appointment apptAtHospB = new Appointment();
        apptAtHospB.setId("appt-2");
        apptAtHospB.setAppointmentNumber("APT-002");
        apptAtHospB.setDoctorId("docA");
        apptAtHospB.setHospitalId("hospB");
        apptAtHospB.setPatientId("pat2");
        apptAtHospB.setStatus("CONFIRMED");

        when(doctorRepository.findAll()).thenReturn(List.of(doctorA));
        when(appointmentRepository.findAll()).thenReturn(List.of(apptAtHospA, apptAtHospB));
        when(patientRepository.findById("pat2")).thenReturn(Optional.of(patient2));

        // When Hospital B requests patients
        ResponseEntity<List<PatientWithAppointmentDTO>> responseB = controller.getPatientsByHospital("hospB");

        assertNotNull(responseB);
        assertEquals(200, responseB.getStatusCode().value());
        List<PatientWithAppointmentDTO> patientsB = responseB.getBody();
        assertNotNull(patientsB);
        // Only Patient 2 who has an appointment at Hospital B should be included
        assertEquals(1, patientsB.size());
        assertEquals("pat2", patientsB.get(0).getId());
        assertEquals(1, patientsB.get(0).getAppointments().size());
        assertEquals("appt-2", patientsB.get(0).getAppointments().get(0).getId());
    }

    @Test
    @DisplayName("Cancelled and refunded appointments at Hospital A remain visible for Hospital A")
    void testCancelledAndRefundedAppointmentsRemainVisibleForOwnHospital() {
        Appointment cancelledAppt = new Appointment();
        cancelledAppt.setId("appt-cancelled");
        cancelledAppt.setAppointmentNumber("APT-003");
        cancelledAppt.setDoctorId("docA");
        cancelledAppt.setHospitalId("hospA");
        cancelledAppt.setPatientId("pat1");
        cancelledAppt.setStatus("CANCELLED");
        cancelledAppt.setRefundStatus("REFUNDED");

        when(doctorRepository.findAll()).thenReturn(List.of(doctorA));
        when(appointmentRepository.findAll()).thenReturn(List.of(cancelledAppt));
        when(patientRepository.findById("pat1")).thenReturn(Optional.of(patient1));

        ResponseEntity<List<PatientWithAppointmentDTO>> response = controller.getPatientsByHospital("hospA");

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        List<PatientWithAppointmentDTO> list = response.getBody();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("CANCELLED", list.get(0).getAppointments().get(0).getStatus());
    }

    @Test
    @DisplayName("Handles null and unassigned hospital appointments safely without error")
    void testHandlesNullHospitalAndDoctorGracefully() {
        Appointment orphanAppt = new Appointment();
        orphanAppt.setId("appt-orphan");
        orphanAppt.setDoctorId(null);
        orphanAppt.setHospitalId(null);
        orphanAppt.setPatientId("pat1");

        Appointment docWithoutHosp = new Appointment();
        docWithoutHosp.setId("appt-no-hosp");
        docWithoutHosp.setDoctorId("docA");
        docWithoutHosp.setHospitalId(null);
        docWithoutHosp.setPatientId("pat1");

        when(doctorRepository.findAll()).thenReturn(List.of(doctorA));
        when(appointmentRepository.findAll()).thenReturn(List.of(orphanAppt, docWithoutHosp));

        ResponseEntity<List<PatientWithAppointmentDTO>> response = controller.getPatientsByHospital("hospA");

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        List<PatientWithAppointmentDTO> list = response.getBody();
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }
}
