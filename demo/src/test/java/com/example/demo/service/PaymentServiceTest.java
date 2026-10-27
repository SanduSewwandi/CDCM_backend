package com.example.demo.service;

import com.example.demo.model.Appointment;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.DoctorRepository;
import com.example.demo.repository.HospitalRepository;
import com.example.demo.repository.LabTestRepository;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.repository.PatientRepository;
import com.example.demo.repository.ScheduleRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.demo.dto.PaymentHistoryDTO;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepo;

    @Mock
    private LabTestRepository labRepo;

    @Mock
    private NotificationRepository notificationRepo;

    @Mock
    private DoctorRepository doctorRepo;

    @Mock
    private HospitalRepository hospitalRepo;

    @Mock
    private ScheduleRepository scheduleRepo;

    @Mock
    private PatientRepository patientRepo;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PaymentService paymentService;


    @Test
    void confirmPaymentSuccess_WithValidAppointment_ShouldConfirmPayment() {

        Appointment appointment = new Appointment();
        appointment.setId("APT001");
        appointment.setPatientId("PAT001");
        appointment.setDoctorId("DOC001");
        appointment.setHospitalId("HOS001");
        appointment.setScheduleId("SCH001");
        appointment.setAppointmentNumber("APT-001");

        appointment.setStatus("PENDING");
        appointment.setPaymentStatus("PENDING");
        appointment.setPaid(false);

        appointment.setDate(
                LocalDate.now().plusDays(7).toString()
        );

        appointment.setTime("10:00");

        when(appointmentRepo.findById("APT001"))
                .thenReturn(Optional.of(appointment));

        when(appointmentRepo.findByScheduleId("SCH001"))
                .thenReturn(Collections.emptyList());

        when(passwordEncoder.encode(anyString()))
                .thenReturn("hashed-otp");

        when(appointmentRepo.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Appointment result =
                paymentService.confirmPaymentSuccess(
                        "APT001",
                        "PAYHERE001",
                        1500.00
                );

        assertNotNull(result);

        assertTrue(result.isPaid());

        assertEquals(
                "PAID",
                result.getPaymentStatus()
        );

        assertEquals(
                "CONFIRMED",
                result.getStatus()
        );

        assertEquals(
                "PAYHERE001",
                result.getPayhereId()
        );

        assertEquals(
                1500.00,
                result.getAmount(),
                0.01
        );

        assertNotNull(
                result.getPaidAt()
        );

        assertEquals(
                "hashed-otp",
                result.getMedicalHistoryOtpHash()
        );

        verify(appointmentRepo)
                .save(appointment);
    }

    @Test
    void cancelPendingPayment_WithPendingAppointment_ShouldCancelPayment() {

        Appointment appointment = new Appointment();
        appointment.setId("APT001");
        appointment.setStatus("PENDING");
        appointment.setPaymentStatus("PENDING");
        appointment.setPaid(false);

        when(appointmentRepo.findById("APT001"))
                .thenReturn(Optional.of(appointment));

        when(appointmentRepo.save(appointment))
                .thenReturn(appointment);

        paymentService.cancelPendingPayment("APT001");

        assertEquals(
                "CANCELLED",
                appointment.getStatus()
        );

        assertEquals(
                "FAILED",
                appointment.getPaymentStatus()
        );

        assertFalse(appointment.isPaid());

        verify(appointmentRepo)
                .findById("APT001");

        verify(appointmentRepo)
                .save(appointment);
    }

    @Test
    void confirmPaymentSuccess_WhenAlreadyPaid_ShouldReturnWithoutDuplicateProcessing() {

        Appointment appointment = new Appointment();
        appointment.setId("APT001");
        appointment.setStatus("CONFIRMED");
        appointment.setPaymentStatus("PAID");
        appointment.setPaid(true);
        appointment.setPayhereId("PAYHERE001");
        appointment.setAmount(1500.00);

        when(appointmentRepo.findById("APT001"))
                .thenReturn(Optional.of(appointment));

        Appointment result =
                paymentService.confirmPaymentSuccess(
                        "APT001",
                        "PAYHERE002",
                        2000.00
                );

        assertNotNull(result);

        assertSame(
                appointment,
                result
        );

        assertEquals(
                "PAYHERE001",
                result.getPayhereId()
        );

        assertEquals(
                1500.00,
                result.getAmount(),
                0.01
        );

        verify(appointmentRepo)
                .findById("APT001");

        verify(appointmentRepo, never())
                .save(any(Appointment.class));

        verify(notificationRepo, never())
                .save(any());
    }

    @Test
    void confirmPaymentSuccess_WhenAppointmentCancelled_ShouldThrowException() {

        Appointment appointment = new Appointment();
        appointment.setId("APT001");
        appointment.setStatus("CANCELLED");
        appointment.setPaymentStatus("FAILED");
        appointment.setPaid(false);

        when(appointmentRepo.findById("APT001"))
                .thenReturn(Optional.of(appointment));

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> paymentService.confirmPaymentSuccess(
                                "APT001",
                                "PAYHERE001",
                                1500.00
                        )
                );

        assertEquals(
                "Cannot confirm appointment that was cancelled or payment failed.",
                exception.getMessage()
        );

        verify(appointmentRepo)
                .findById("APT001");

        verify(appointmentRepo, never())
                .save(any(Appointment.class));

        verify(notificationRepo, never())
                .save(any());
    }

    @Test
    void getPatientPaymentHistory_ShouldReturnAppointmentPayments() {

        Appointment appointment = new Appointment();
        appointment.setId("APT001");
        appointment.setPatientId("PAT001");
        appointment.setAppointmentNumber("APT-001");
        appointment.setPayhereId("PAYHERE001");
        appointment.setAmount(1500.00);
        appointment.setPaymentStatus("PAID");
        appointment.setPaid(true);
        appointment.setDate("2026-10-30");
        appointment.setTime("10:00");

        when(appointmentRepo.findByPatientId("PAT001"))
                .thenReturn(List.of(appointment));

        when(labRepo.findByPatientId("PAT001"))
                .thenReturn(Collections.emptyList());

        List<PaymentHistoryDTO> result =
                paymentService.getPatientPaymentHistory("PAT001");

        assertNotNull(result);
        assertEquals(1, result.size());

        PaymentHistoryDTO payment = result.get(0);

        assertEquals("APT001", payment.getId());
        assertEquals("PAYHERE001", payment.getTransactionId());
        assertEquals("APT-001", payment.getAppointmentNumber());

        assertEquals(
                1500.00,
                payment.getAmount(),
                0.01
        );

        assertEquals("PAID", payment.getPaymentStatus());
        assertTrue(payment.isPaid());

        assertEquals("APPOINTMENT", payment.getType());

        verify(appointmentRepo)
                .findByPatientId("PAT001");

        verify(labRepo)
                .findByPatientId("PAT001");
    }
}