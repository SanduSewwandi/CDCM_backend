
package com.example.demo.integration;

import com.example.demo.config.SecurityConfig;
import com.example.demo.controller.AppointmentController;
import com.example.demo.model.Appointment;
import com.example.demo.security.JwtAuthenticationFilter;
import com.example.demo.security.JwtService;
import com.example.demo.service.AppointmentService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AppointmentController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AppointmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppointmentService appointmentService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void bookAppointmentShouldReturn200() throws Exception {

        Appointment savedAppointment = new Appointment();

        when(appointmentService.bookAppointment(any(Appointment.class)))
                .thenReturn(savedAppointment);

        mockMvc.perform(post("/api/appointments/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        verify(appointmentService)
                .bookAppointment(any(Appointment.class));
    }


    @Test
    void getPatientAppointmentsShouldReturn200() throws Exception {

        String patientId = "patient123";

        Appointment appointment = new Appointment();

        when(appointmentService.getAppointmentsForPatient(patientId))
                .thenReturn(java.util.List.of(appointment));

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .get("/api/appointments/patient/" + patientId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(appointmentService)
                .getAppointmentsForPatient(patientId);
    }


    @Test
    void getScheduleAppointmentsShouldReturn200() throws Exception {

        String scheduleId = "schedule123";

        Appointment appointment = new Appointment();

        when(appointmentService.getAppointmentsBySchedule(scheduleId))
                .thenReturn(java.util.List.of(appointment));

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .get("/api/appointments/schedule/" + scheduleId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(appointmentService)
                .getAppointmentsBySchedule(scheduleId);
    }

    @Test
    void getDoctorAppointmentsShouldReturn200() throws Exception {

        String doctorId = "doctor123";

        when(appointmentService.getAppointmentsByDoctor(doctorId))
                .thenReturn(java.util.Collections.emptyList());

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .get("/api/appointments/doctor/" + doctorId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(appointmentService)
                .getAppointmentsByDoctor(doctorId);
    }


    @Test
    void getHospitalAppointmentsShouldReturn200() throws Exception {

        String hospitalId = "hospital123";

        when(appointmentService.getAppointmentsByHospital(hospitalId))
                .thenReturn(java.util.Collections.emptyList());

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .get("/api/appointments/hospital/" + hospitalId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(appointmentService)
                .getAppointmentsByHospital(hospitalId);
    }

    @Test
    void cancelPendingAppointmentShouldReturn200() throws Exception {

        String appointmentId = "appointment123";

        Appointment cancelledAppointment = new Appointment();

        when(appointmentService.cancelPendingAppointment(appointmentId))
                .thenReturn(cancelledAppointment);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .put("/api/appointments/cancel-pending/" + appointmentId)
                )
                .andExpect(status().isOk());

        verify(appointmentService)
                .cancelPendingAppointment(appointmentId);
    }

    @Test
    void patientCancellationWithoutJwtShouldBeForbidden() throws Exception {

        String appointmentId = "appointment123";

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .put("/api/appointments/" + appointmentId + "/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reason\":\"Unable to attend\"}")
                )
                .andExpect(status().isForbidden());

        org.mockito.Mockito.verify(
                appointmentService,
                org.mockito.Mockito.never()
        ).cancelAppointmentByPatient(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void patientCancellationWithValidJwtShouldReturn200() throws Exception {

        String token = "valid.patient.token";
        String appointmentId = "appointment123";
        String patientEmail = "patient@example.com";
        String reason = "Unable to attend";

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token)).thenReturn(patientEmail);
        when(jwtService.extractRole(token)).thenReturn("PATIENT");

        Appointment cancelledAppointment = new Appointment();

        when(appointmentService.cancelAppointmentByPatient(
                appointmentId,
                patientEmail,
                reason
        )).thenReturn(cancelledAppointment);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .put("/api/appointments/" + appointmentId + "/cancel")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reason\":\"Unable to attend\"}")
                )
                .andExpect(status().isOk());

        verify(appointmentService)
                .cancelAppointmentByPatient(
                        appointmentId,
                        patientEmail,
                        reason
                );
    }

    @Test
    void doctorJwtShouldNotCancelPatientAppointment() throws Exception {

        String token = "valid.doctor.token";
        String appointmentId = "appointment123";

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token))
                .thenReturn("doctor@example.com");
        when(jwtService.extractRole(token))
                .thenReturn("DOCTOR");

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .put("/api/appointments/" + appointmentId + "/cancel")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reason\":\"Unable to attend\"}")
                )
                .andExpect(status().isForbidden());

        org.mockito.Mockito.verify(
                appointmentService,
                org.mockito.Mockito.never()
        ).cancelAppointmentByPatient(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.nullable(String.class)
        );
    }

    @Test
    void hospitalCancellationWithValidJwtShouldReturn200() throws Exception {

        String token = "valid.hospital.token";
        String appointmentId = "appointment123";
        String hospitalEmail = "hospital@example.com";
        String reason = "Doctor unavailable";

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token)).thenReturn(hospitalEmail);
        when(jwtService.extractRole(token)).thenReturn("HOSPITAL");

        Appointment cancelledAppointment = new Appointment();

        when(appointmentService.cancelAppointmentByHospital(
                appointmentId,
                hospitalEmail,
                reason
        )).thenReturn(cancelledAppointment);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .put("/api/appointments/" + appointmentId + "/hospital-cancel")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reason\":\"Doctor unavailable\"}")
                )
                .andExpect(status().isOk());

        verify(appointmentService)
                .cancelAppointmentByHospital(
                        appointmentId,
                        hospitalEmail,
                        reason
                );
    }

    @Test
    void patientJwtShouldNotCancelAppointmentAsHospital() throws Exception {

        String token = "valid.patient.token";
        String appointmentId = "appointment123";

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token))
                .thenReturn("patient@example.com");
        when(jwtService.extractRole(token))
                .thenReturn("PATIENT");

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .put("/api/appointments/" + appointmentId + "/hospital-cancel")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"reason\":\"Doctor unavailable\"}")
                )
                .andExpect(status().isForbidden());

        org.mockito.Mockito.verify(
                appointmentService,
                org.mockito.Mockito.never()
        ).cancelAppointmentByHospital(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.nullable(String.class)
        );
    }

    @Test
    void patientRefundPreviewWithValidJwtShouldReturn200() throws Exception {

        String token = "valid.patient.token";
        String appointmentId = "appointment123";
        String patientEmail = "patient@example.com";

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token)).thenReturn(patientEmail);
        when(jwtService.extractRole(token)).thenReturn("PATIENT");

        com.example.demo.dto.RefundPreviewDTO preview =
                new com.example.demo.dto.RefundPreviewDTO();

        when(appointmentService.getCancellationRefundPreview(
                appointmentId,
                patientEmail
        )).thenReturn(preview);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .get("/api/appointments/" + appointmentId + "/cancel-preview")
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk());

        verify(appointmentService)
                .getCancellationRefundPreview(
                        appointmentId,
                        patientEmail
                );
    }

    @Test
    void refundPreviewWithoutJwtShouldBeForbidden() throws Exception {

        String appointmentId = "appointment123";

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .get("/api/appointments/" + appointmentId + "/cancel-preview")
                )
                .andExpect(status().isForbidden());

        org.mockito.Mockito.verify(
                appointmentService,
                org.mockito.Mockito.never()
        ).getCancellationRefundPreview(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }


    @Test
    void hospitalPendingRefundsWithValidJwtShouldReturn200() throws Exception {

        String token = "valid.hospital.token";
        String hospitalId = "hospital123";
        String hospitalEmail = "hospital@example.com";

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token)).thenReturn(hospitalEmail);
        when(jwtService.extractRole(token)).thenReturn("HOSPITAL");

        when(appointmentService.getPendingRefundsForHospital(
                hospitalId,
                hospitalEmail
        )).thenReturn(java.util.Collections.emptyList());

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .get("/api/appointments/hospital/"
                                        + hospitalId + "/pending-refunds")
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(appointmentService)
                .getPendingRefundsForHospital(
                        hospitalId,
                        hospitalEmail
                );
    }

    @Test
    void patientJwtShouldNotAccessHospitalPendingRefunds() throws Exception {

        String token = "valid.patient.token";
        String hospitalId = "hospital123";

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token))
                .thenReturn("patient@example.com");
        when(jwtService.extractRole(token))
                .thenReturn("PATIENT");

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .get("/api/appointments/hospital/"
                                        + hospitalId + "/pending-refunds")
                                .header("Authorization", "Bearer " + token)
                )
                .andExpect(status().isForbidden());

        org.mockito.Mockito.verify(
                appointmentService,
                org.mockito.Mockito.never()
        ).getPendingRefundsForHospital(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }


    @Test
    void hospitalRefundConfirmationWithValidJwtShouldReturn200() throws Exception {

        String token = "valid.hospital.token";
        String appointmentId = "appointment123";
        String hospitalEmail = "hospital@example.com";
        String transactionRef = "TXN123456";
        String notes = "Refund transferred successfully";

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token)).thenReturn(hospitalEmail);
        when(jwtService.extractRole(token)).thenReturn("HOSPITAL");

        Appointment updatedAppointment = new Appointment();

        when(appointmentService.confirmRefundTransfer(
                appointmentId,
                hospitalEmail,
                transactionRef,
                notes
        )).thenReturn(updatedAppointment);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .put("/api/appointments/" + appointmentId + "/confirm-refund")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "transactionRef": "TXN123456",
                                        "notes": "Refund transferred successfully"
                                    }
                                    """)
                )
                .andExpect(status().isOk());

        verify(appointmentService)
                .confirmRefundTransfer(
                        appointmentId,
                        hospitalEmail,
                        transactionRef,
                        notes
                );
    }

    @Test
    void patientJwtShouldNotConfirmHospitalRefund() throws Exception {

        String token = "valid.patient.token";
        String appointmentId = "appointment123";

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token))
                .thenReturn("patient@example.com");
        when(jwtService.extractRole(token))
                .thenReturn("PATIENT");

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .put("/api/appointments/" + appointmentId + "/confirm-refund")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "transactionRef": "TXN123456",
                                        "notes": "Refund confirmed"
                                    }
                                    """)
                )
                .andExpect(status().isForbidden());

        org.mockito.Mockito.verify(
                appointmentService,
                org.mockito.Mockito.never()
        ).confirmRefundTransfer(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.nullable(String.class),
                org.mockito.ArgumentMatchers.nullable(String.class)
        );
    }

    @Test
    void hospitalRefundConfirmationConflictShouldReturn409() throws Exception {

        String token = "valid.hospital.token";
        String appointmentId = "appointment123";
        String hospitalEmail = "hospital@example.com";

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token)).thenReturn(hospitalEmail);
        when(jwtService.extractRole(token)).thenReturn("HOSPITAL");

        when(appointmentService.confirmRefundTransfer(
                appointmentId,
                hospitalEmail,
                "TXN123456",
                "Refund confirmed"
        )).thenThrow(new IllegalStateException("Refund already confirmed"));

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .put("/api/appointments/" + appointmentId + "/confirm-refund")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "transactionRef": "TXN123456",
                                        "notes": "Refund confirmed"
                                    }
                                    """)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Refund already confirmed"));

        verify(appointmentService)
                .confirmRefundTransfer(
                        appointmentId,
                        hospitalEmail,
                        "TXN123456",
                        "Refund confirmed"
                );
    }

    @Test
    void patientBankDetailsWithValidJwtShouldReturn200() throws Exception {

        String token = "valid.patient.token";
        String appointmentId = "appointment123";
        String patientEmail = "patient@example.com";

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token)).thenReturn(patientEmail);
        when(jwtService.extractRole(token)).thenReturn("PATIENT");

        Appointment updatedAppointment = new Appointment();

        when(appointmentService.submitBankDetails(
                org.mockito.ArgumentMatchers.eq(appointmentId),
                org.mockito.ArgumentMatchers.eq(patientEmail),
                org.mockito.ArgumentMatchers.any(
                        com.example.demo.dto.BankDetailsRequest.class)
        )).thenReturn(updatedAppointment);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .post("/api/appointments/" + appointmentId + "/bank-details")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isOk());

        verify(appointmentService)
                .submitBankDetails(
                        org.mockito.ArgumentMatchers.eq(appointmentId),
                        org.mockito.ArgumentMatchers.eq(patientEmail),
                        org.mockito.ArgumentMatchers.any(
                                com.example.demo.dto.BankDetailsRequest.class)
                );
    }

    @Test
    void patientBankDetailsWithoutJwtShouldBeForbidden() throws Exception {

        String appointmentId = "appointment123";

        mockMvc.perform(
                        org.springframework.test.web.servlet.request
                                .MockMvcRequestBuilders
                                .post("/api/appointments/"
                                        + appointmentId + "/bank-details")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isForbidden());

        org.mockito.Mockito.verify(
                appointmentService,
                org.mockito.Mockito.never()
        ).submitBankDetails(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(
                        com.example.demo.dto.BankDetailsRequest.class
                )
        );
    }


}
