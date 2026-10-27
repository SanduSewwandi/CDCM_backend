package com.example.demo.service;

import com.example.demo.model.Appointment;
import com.example.demo.model.Schedule;

import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.DoctorRepository;
import com.example.demo.repository.HospitalRepository;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.repository.PatientRepository;
import com.example.demo.repository.ScheduleRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ChatService chatService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AppointmentService appointmentService;


    @Test
    void bookAppointment_WithValidDetails_ShouldSaveAppointment() {

        Appointment appointment = new Appointment();
        appointment.setPatientId("PAT001");
        appointment.setDoctorId("DOC001");
        appointment.setHospitalId("HOS001");
        appointment.setScheduleId("SCH001");
        appointment.setAppointmentNumber("1");

        Schedule schedule = new Schedule();
        schedule.setId("SCH001");
        schedule.setDoctorId("DOC001");
        schedule.setHospitalId("HOS001");
        schedule.setStatus("ACCEPTED");

        // Future date so the schedule is not expired
        schedule.setDate(
                LocalDate.now().plusDays(7).toString()
        );

        schedule.setStartTime("09:00");
        schedule.setEndTime("10:00");

        when(scheduleRepository.findById("SCH001"))
                .thenReturn(Optional.of(schedule));

        when(appointmentRepository.findByPatientId("PAT001"))
                .thenReturn(Collections.emptyList());

        when(appointmentRepository
                .existsByPatientIdAndDoctorIdAndScheduleId(
                        "PAT001",
                        "DOC001",
                        "SCH001"
                ))
                .thenReturn(false);

        when(appointmentRepository.findByScheduleId("SCH001"))
                .thenReturn(Collections.emptyList());

        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> {
                    Appointment saved = invocation.getArgument(0);
                    saved.setId("APT_DB_001");
                    return saved;
                });

        Appointment result =
                appointmentService.bookAppointment(appointment);

        assertNotNull(result);

        assertEquals(
                "APT-001",
                result.getAppointmentNumber()
        );

        assertEquals("PENDING", result.getStatus());
        assertEquals("PENDING", result.getPaymentStatus());
        assertFalse(result.isPaid());

        assertEquals(1000.00, result.getAmount(), 0.01);

        verify(appointmentRepository)
                .save(appointment);

        verify(chatService).createConversation(
                "APT_DB_001",
                "PAT001",
                "DOC001"
        );
    }

    @Test
    void bookAppointment_WhenDuplicateBooking_ShouldThrowException() {

        Appointment appointment = new Appointment();
        appointment.setPatientId("PAT001");
        appointment.setDoctorId("DOC001");
        appointment.setHospitalId("HOS001");
        appointment.setScheduleId("SCH001");
        appointment.setAppointmentNumber("1");

        Schedule schedule = new Schedule();
        schedule.setId("SCH001");
        schedule.setDoctorId("DOC001");
        schedule.setHospitalId("HOS001");
        schedule.setStatus("ACCEPTED");
        schedule.setDate(LocalDate.now().plusDays(7).toString());
        schedule.setStartTime("09:00");
        schedule.setEndTime("10:00");

        when(scheduleRepository.findById("SCH001"))
                .thenReturn(Optional.of(schedule));

        when(appointmentRepository.findByPatientId("PAT001"))
                .thenReturn(Collections.emptyList());

        when(appointmentRepository
                .existsByPatientIdAndDoctorIdAndScheduleId(
                        "PAT001",
                        "DOC001",
                        "SCH001"
                ))
                .thenReturn(true);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> appointmentService.bookAppointment(appointment)
                );

        assertEquals(
                "You have already booked this doctor's schedule.",
                exception.getMessage()
        );

        verify(appointmentRepository, never())
                .save(any(Appointment.class));

        verify(chatService, never())
                .createConversation(
                        anyString(),
                        anyString(),
                        anyString()
                );
    }

    @Test
    void bookAppointment_WhenSchedulePending_ShouldThrowException() {

        Appointment appointment = new Appointment();
        appointment.setPatientId("PAT001");
        appointment.setDoctorId("DOC001");
        appointment.setHospitalId("HOS001");
        appointment.setScheduleId("SCH001");
        appointment.setAppointmentNumber("1");

        Schedule schedule = new Schedule();
        schedule.setId("SCH001");
        schedule.setStatus("PENDING");

        when(scheduleRepository.findById("SCH001"))
                .thenReturn(Optional.of(schedule));

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> appointmentService.bookAppointment(appointment)
                );

        assertEquals(
                "Cannot book an appointment for a schedule pending doctor acceptance.",
                exception.getMessage()
        );

        verify(appointmentRepository, never())
                .save(any(Appointment.class));

        verify(chatService, never())
                .createConversation(
                        anyString(),
                        anyString(),
                        anyString()
                );
    }

    @Test
    void getAppointmentsForPatient_ShouldReturnOnlyValidPaidAppointments() {

        Appointment confirmed = new Appointment();
        confirmed.setId("APT001");
        confirmed.setStatus("CONFIRMED");
        confirmed.setPaymentStatus("PAID");
        confirmed.setPaid(true);

        Appointment pending = new Appointment();
        pending.setId("APT002");
        pending.setStatus("PENDING");
        pending.setPaymentStatus("PENDING");
        pending.setPaid(false);

        Appointment failed = new Appointment();
        failed.setId("APT003");
        failed.setStatus("PENDING");
        failed.setPaymentStatus("FAILED");
        failed.setPaid(false);

        when(appointmentRepository.findByPatientId("PAT001"))
                .thenReturn(
                        java.util.List.of(
                                confirmed,
                                pending,
                                failed
                        )
                );

        java.util.List<Appointment> result =
                appointmentService.getAppointmentsForPatient("PAT001");

        assertNotNull(result);
        assertEquals(1, result.size());

        assertEquals(
                "APT001",
                result.get(0).getId()
        );

        verify(appointmentRepository)
                .findByPatientId("PAT001");
    }

    @Test
    void cancelPendingAppointment_WithValidPendingAppointment_ShouldCancelSuccessfully() {

        Appointment appointment = new Appointment();
        appointment.setId("APT001");
        appointment.setStatus("PENDING");
        appointment.setPaymentStatus("PENDING");
        appointment.setPaid(false);

        when(appointmentRepository.findById("APT001"))
                .thenReturn(Optional.of(appointment));

        when(appointmentRepository.save(appointment))
                .thenReturn(appointment);

        Appointment result =
                appointmentService.cancelPendingAppointment("APT001");

        assertNotNull(result);

        assertEquals(
                "CANCELLED",
                result.getStatus()
        );

        assertEquals(
                "FAILED",
                result.getPaymentStatus()
        );

        verify(appointmentRepository)
                .findById("APT001");

        verify(appointmentRepository)
                .save(appointment);
    }

    @Test
    void getAppointmentsByDoctor_ShouldReturnOnlyValidAppointments() {

        Appointment validAppointment = new Appointment();
        validAppointment.setId("APT001");
        validAppointment.setDoctorId("DOC001");
        validAppointment.setScheduleId("SCH001");
        validAppointment.setStatus("CONFIRMED");

        Appointment cancelledAppointment = new Appointment();
        cancelledAppointment.setId("APT002");
        cancelledAppointment.setDoctorId("DOC001");
        cancelledAppointment.setScheduleId("SCH002");
        cancelledAppointment.setStatus("CANCELLED");

        Schedule validSchedule = new Schedule();
        validSchedule.setId("SCH001");
        validSchedule.setDoctorId("DOC001");
        validSchedule.setStatus("ACCEPTED");

        Schedule cancelledSchedule = new Schedule();
        cancelledSchedule.setId("SCH002");
        cancelledSchedule.setDoctorId("DOC001");
        cancelledSchedule.setStatus("CANCELLED");

        when(appointmentRepository.findByDoctorId("DOC001"))
                .thenReturn(List.of(
                        validAppointment,
                        cancelledAppointment
                ));

        when(scheduleRepository.findAllById(any(Set.class)))
                .thenReturn(List.of(
                        validSchedule,
                        cancelledSchedule
                ));

        List<com.example.demo.dto.AppointmentResponseDTO> result =
                appointmentService.getAppointmentsByDoctor("DOC001");

        assertNotNull(result);
        assertEquals(1, result.size());

        assertEquals(
                "APT001",
                result.get(0).getId()
        );

        verify(appointmentRepository)
                .findByDoctorId("DOC001");

        verify(scheduleRepository)
                .findAllById(any(Set.class));
    }
}