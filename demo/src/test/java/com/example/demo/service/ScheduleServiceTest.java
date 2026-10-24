
package com.example.demo.service;

import com.example.demo.dto.ScheduleRequest;
import com.example.demo.model.Appointment;
import com.example.demo.model.Doctor;
import com.example.demo.model.Hospital;
import com.example.demo.model.Notification;
import com.example.demo.model.Patient;
import com.example.demo.model.Schedule;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.DoctorRepository;
import com.example.demo.repository.HospitalRepository;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.repository.PatientRepository;
import com.example.demo.repository.ScheduleRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private SmsService smsService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ScheduleService scheduleService;

    private ScheduleRequest physicalRequest;
    private ScheduleRequest videoRequest;

    private String futureDate() {
        return LocalDate.now(ZoneId.of("Asia/Colombo"))
                .plusDays(10)
                .toString();
    }

    private Schedule createSchedule(String status, String type) {
        Schedule schedule = new Schedule(
                "doctor-1",
                "hospital-1",
                futureDate(),
                "10:00",
                "11:00",
                type,
                "VIDEO".equals(type) ? "https://meet.example.com/test" : null,
                "PHYSICAL".equals(type) ? "Room 10" : null,
                10,
                status
        );
        schedule.setId("schedule-1");
        return schedule;
    }

    @BeforeEach
    void setUp() {
        physicalRequest = new ScheduleRequest(
                "doctor-1",
                "hospital-1",
                futureDate(),
                "10:00",
                "11:00",
                "PHYSICAL",
                null,
                "Room 10",
                10
        );

        videoRequest = new ScheduleRequest(
                "doctor-1",
                "hospital-1",
                futureDate(),
                "14:00",
                "15:00",
                "VIDEO",
                "https://meet.example.com/test",
                null,
                8
        );
    }

    // =========================================================
    // CREATE SCHEDULE TESTS
    // =========================================================

    @Test
    void createPhysicalScheduleSuccessfully() {
        when(scheduleRepository.save(any(Schedule.class)))
                .thenAnswer(invocation -> {
                    Schedule schedule = invocation.getArgument(0);
                    schedule.setId("schedule-1");
                    return schedule;
                });

        when(hospitalRepository.findById("hospital-1"))
                .thenReturn(Optional.empty());

        when(doctorRepository.findById("doctor-1"))
                .thenReturn(Optional.empty());

        Schedule result = scheduleService.createSchedule(physicalRequest);

        assertNotNull(result);
        assertEquals("schedule-1", result.getId());
        assertEquals("doctor-1", result.getDoctorId());
        assertEquals("hospital-1", result.getHospitalId());
        assertEquals("PHYSICAL", result.getType());
        assertEquals("Room 10", result.getRoomNumber());
        assertNull(result.getMeetingLink());
        assertEquals(10, result.getMaximumPatients());
        assertEquals("PENDING", result.getStatus());

        verify(scheduleRepository).save(any(Schedule.class));
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void createVideoScheduleSuccessfully() {
        when(scheduleRepository.save(any(Schedule.class)))
                .thenAnswer(invocation -> {
                    Schedule schedule = invocation.getArgument(0);
                    schedule.setId("video-schedule-1");
                    return schedule;
                });

        when(hospitalRepository.findById("hospital-1"))
                .thenReturn(Optional.empty());

        when(doctorRepository.findById("doctor-1"))
                .thenReturn(Optional.empty());

        Schedule result = scheduleService.createSchedule(videoRequest);

        assertNotNull(result);
        assertEquals("VIDEO", result.getType());
        assertNull(result.getRoomNumber());
        assertEquals(
                "https://meet.example.com/test",
                result.getMeetingLink()
        );
        assertEquals(8, result.getMaximumPatients());
        assertEquals("PENDING", result.getStatus());

        verify(scheduleRepository).save(any(Schedule.class));
    }

    @Test
    void createScheduleDefaultsMissingTypeToPhysical() {
        physicalRequest.setType(null);
        physicalRequest.setRoomNumber("Room 5");

        when(scheduleRepository.save(any(Schedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(hospitalRepository.findById("hospital-1"))
                .thenReturn(Optional.empty());

        when(doctorRepository.findById("doctor-1"))
                .thenReturn(Optional.empty());

        Schedule result = scheduleService.createSchedule(physicalRequest);

        assertEquals("PHYSICAL", result.getType());
        assertEquals("Room 5", result.getRoomNumber());
    }

    @Test
    void createScheduleDefaultsBlankTypeToPhysical() {
        physicalRequest.setType("   ");

        when(scheduleRepository.save(any(Schedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(hospitalRepository.findById("hospital-1"))
                .thenReturn(Optional.empty());

        when(doctorRepository.findById("doctor-1"))
                .thenReturn(Optional.empty());

        Schedule result = scheduleService.createSchedule(physicalRequest);

        assertEquals("PHYSICAL", result.getType());
    }

    @Test
    void createScheduleAcceptsLowercaseVideoType() {
        videoRequest.setType("video");

        when(scheduleRepository.save(any(Schedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(hospitalRepository.findById("hospital-1"))
                .thenReturn(Optional.empty());

        when(doctorRepository.findById("doctor-1"))
                .thenReturn(Optional.empty());

        Schedule result = scheduleService.createSchedule(videoRequest);

        assertEquals("VIDEO", result.getType());
    }

    @Test
    void createScheduleRejectsInvalidType() {
        physicalRequest.setType("ONLINE");

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> scheduleService.createSchedule(physicalRequest)
        );

        assertEquals(
                "Invalid schedule type. Only PHYSICAL or VIDEO is allowed.",
                exception.getMessage()
        );

        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    void createPhysicalScheduleRejectsMissingRoomNumber() {
        physicalRequest.setRoomNumber(null);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> scheduleService.createSchedule(physicalRequest)
        );

        assertEquals(
                "Room number is required for PHYSICAL schedules.",
                exception.getMessage()
        );

        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    void createPhysicalScheduleRejectsBlankRoomNumber() {
        physicalRequest.setRoomNumber("   ");

        assertThrows(
                RuntimeException.class,
                () -> scheduleService.createSchedule(physicalRequest)
        );

        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    void createScheduleRejectsZeroMaximumPatients() {
        physicalRequest.setMaximumPatients(0);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> scheduleService.createSchedule(physicalRequest)
        );

        assertEquals(
                "Maximum patients must be greater than 0.",
                exception.getMessage()
        );

        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    void createScheduleRejectsNegativeMaximumPatients() {
        physicalRequest.setMaximumPatients(-1);

        assertThrows(
                RuntimeException.class,
                () -> scheduleService.createSchedule(physicalRequest)
        );

        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    // =========================================================
    // SCHEDULE RETRIEVAL AND BOOKING COUNTS
    // =========================================================

    @Test
    void getDoctorSchedulesPopulatesDetailsAndBookingCounts() {
        Schedule schedule = createSchedule("ACCEPTED", "PHYSICAL");
        List<Schedule> schedules = new ArrayList<>(List.of(schedule));

        Doctor doctor = mock(Doctor.class);
        when(doctor.getFirstName()).thenReturn("Amal");
        when(doctor.getLastName()).thenReturn("Perera");
        when(doctor.getSpecialization()).thenReturn("Cardiology");

        Hospital hospital = mock(Hospital.class);
        when(hospital.getName()).thenReturn("City Hospital");
        when(hospital.getLocation()).thenReturn("Colombo");

        when(scheduleRepository.findByDoctorId("doctor-1"))
                .thenReturn(schedules);
        when(doctorRepository.findById("doctor-1"))
                .thenReturn(Optional.of(doctor));
        when(hospitalRepository.findById("hospital-1"))
                .thenReturn(Optional.of(hospital));

        when(appointmentRepository.countByScheduleIdAndStatus(
                "schedule-1", "CONFIRMED")).thenReturn(2L);
        when(appointmentRepository.countByScheduleIdAndStatus(
                "schedule-1", "COMPLETED")).thenReturn(1L);
        when(appointmentRepository.countByScheduleIdAndStatus(
                "schedule-1", "PAID")).thenReturn(2L);

        List<Schedule> result =
                scheduleService.getDoctorSchedules("doctor-1");

        assertEquals(1, result.size());
        assertEquals("Dr. Amal Perera", schedule.getDoctorName());
        assertEquals("Cardiology", schedule.getSpecialty());
        assertEquals("City Hospital", schedule.getHospitalName());
        assertEquals("Colombo", schedule.getHospitalLocation());

        assertEquals(5L, schedule.getBookedPatientCount());
        assertEquals(5L, schedule.getAvailableSlots());
    }

    @Test
    void getHospitalSchedulesUsesHospitalRepositoryQuery() {
        when(scheduleRepository.findByHospitalId("hospital-1"))
                .thenReturn(new ArrayList<>());

        List<Schedule> result =
                scheduleService.getHospitalSchedules("hospital-1");

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(scheduleRepository).findByHospitalId("hospital-1");
    }

    @Test
    void getHospitalSchedulesByDateUsesHospitalAndDate() {
        when(scheduleRepository.findByHospitalIdAndDate(
                "hospital-1", futureDate()))
                .thenReturn(new ArrayList<>());

        List<Schedule> result =
                scheduleService.getHospitalSchedulesByDate(
                        "hospital-1", futureDate()
                );

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(scheduleRepository).findByHospitalIdAndDate(
                "hospital-1", futureDate()
        );
    }

    @Test
    void availableSlotsNeverBecomeNegative() {
        Schedule schedule = createSchedule("ACCEPTED", "VIDEO");
        schedule.setMaximumPatients(2);

        when(scheduleRepository.findByDoctorId("doctor-1"))
                .thenReturn(new ArrayList<>(List.of(schedule)));
        when(doctorRepository.findById("doctor-1"))
                .thenReturn(Optional.empty());
        when(hospitalRepository.findById("hospital-1"))
                .thenReturn(Optional.empty());

        when(appointmentRepository.countByScheduleIdAndStatus(
                "schedule-1", "CONFIRMED")).thenReturn(2L);
        when(appointmentRepository.countByScheduleIdAndStatus(
                "schedule-1", "COMPLETED")).thenReturn(1L);
        when(appointmentRepository.countByScheduleIdAndStatus(
                "schedule-1", "PAID")).thenReturn(1L);

        List<Schedule> result =
                scheduleService.getDoctorSchedules("doctor-1");

        assertEquals(4L, result.get(0).getBookedPatientCount());
        assertEquals(0L, result.get(0).getAvailableSlots());
    }

    // =========================================================
    // ACCEPT SCHEDULE TESTS
    // =========================================================

    @Test
    void acceptPendingFutureScheduleSuccessfully() {
        Schedule schedule = createSchedule("PENDING", "PHYSICAL");

        when(scheduleRepository.findById("schedule-1"))
                .thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any(Schedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(doctorRepository.findById("doctor-1"))
                .thenReturn(Optional.empty());

        Schedule result =
                scheduleService.acceptSchedule("schedule-1");

        assertEquals("ACCEPTED", result.getStatus());

        verify(scheduleRepository).save(schedule);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void acceptScheduleRejectsBlankId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> scheduleService.acceptSchedule(" ")
        );

        verify(scheduleRepository, never()).findById(anyString());
    }

    @Test
    void acceptScheduleRejectsUnknownId() {
        when(scheduleRepository.findById("missing"))
                .thenReturn(Optional.empty());

        assertThrows(
                RuntimeException.class,
                () -> scheduleService.acceptSchedule("missing")
        );
    }

    @Test
    void acceptScheduleRejectsNonPendingSchedule() {
        Schedule schedule = createSchedule("ACCEPTED", "PHYSICAL");

        when(scheduleRepository.findById("schedule-1"))
                .thenReturn(Optional.of(schedule));

        assertThrows(
                IllegalStateException.class,
                () -> scheduleService.acceptSchedule("schedule-1")
        );

        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    void acceptScheduleRejectsExpiredSchedule() {
        Schedule schedule = createSchedule("PENDING", "PHYSICAL");
        schedule.setDate("2000-01-01");
        schedule.setEndTime("10:00");

        when(scheduleRepository.findById("schedule-1"))
                .thenReturn(Optional.of(schedule));

        assertThrows(
                IllegalArgumentException.class,
                () -> scheduleService.acceptSchedule("schedule-1")
        );

        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    void doctorCannotAcceptAnotherDoctorsScheduleWhenDoctorIsFound() {
        Schedule schedule = createSchedule("PENDING", "PHYSICAL");

        Doctor otherDoctor = mock(Doctor.class);
        when(otherDoctor.getId()).thenReturn("doctor-2");

        when(scheduleRepository.findById("schedule-1"))
                .thenReturn(Optional.of(schedule));
        when(doctorRepository.findByEmail("other@example.com"))
                .thenReturn(Optional.of(otherDoctor));

        assertThrows(
                SecurityException.class,
                () -> scheduleService.acceptScheduleByDoctor(
                        "schedule-1", " OTHER@example.com "
                )
        );

        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    // =========================================================
    // REJECT SCHEDULE TESTS
    // =========================================================

    @Test
    void rejectPendingScheduleAndSaveReason() {
        Schedule schedule = createSchedule("PENDING", "VIDEO");

        when(scheduleRepository.findById("schedule-1"))
                .thenReturn(Optional.of(schedule));
        when(appointmentRepository.existsByScheduleIdAndStatusIn(
                eq("schedule-1"), anyList()))
                .thenReturn(false);
        when(scheduleRepository.save(any(Schedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(doctorRepository.findById("doctor-1"))
                .thenReturn(Optional.empty());

        Schedule result = scheduleService.rejectSchedule(
                "schedule-1", "Doctor unavailable"
        );

        assertEquals("REJECTED", result.getStatus());
        assertEquals("Doctor unavailable", result.getRejectionReason());

        verify(scheduleRepository).save(schedule);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void rejectScheduleRejectsNonPendingSchedule() {
        Schedule schedule = createSchedule("ACCEPTED", "PHYSICAL");

        when(scheduleRepository.findById("schedule-1"))
                .thenReturn(Optional.of(schedule));

        assertThrows(
                IllegalStateException.class,
                () -> scheduleService.rejectSchedule("schedule-1")
        );

        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    void rejectScheduleRejectsExpiredSchedule() {
        Schedule schedule = createSchedule("PENDING", "PHYSICAL");
        schedule.setDate("2000-01-01");
        schedule.setEndTime("10:00");

        when(scheduleRepository.findById("schedule-1"))
                .thenReturn(Optional.of(schedule));

        assertThrows(
                IllegalArgumentException.class,
                () -> scheduleService.rejectSchedule("schedule-1")
        );

        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    void rejectScheduleWithActiveBookingsIsNotAllowed() {
        Schedule schedule = createSchedule("PENDING", "PHYSICAL");

        when(scheduleRepository.findById("schedule-1"))
                .thenReturn(Optional.of(schedule));
        when(appointmentRepository.existsByScheduleIdAndStatusIn(
                eq("schedule-1"), anyList()))
                .thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () -> scheduleService.rejectSchedule("schedule-1")
        );

        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    void rejectScheduleDoesNotCancelAppointments() {
        Schedule schedule = createSchedule("PENDING", "PHYSICAL");

        when(scheduleRepository.findById("schedule-1"))
                .thenReturn(Optional.of(schedule));
        when(appointmentRepository.existsByScheduleIdAndStatusIn(
                eq("schedule-1"), anyList()))
                .thenReturn(false);
        when(scheduleRepository.save(any(Schedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(doctorRepository.findById("doctor-1"))
                .thenReturn(Optional.empty());

        scheduleService.rejectSchedule("schedule-1", "Unavailable");

        verify(appointmentRepository, never())
                .findByScheduleId(anyString());
    }

    @Test
    void doctorCannotRejectAnotherDoctorsScheduleWhenDoctorIsFound() {
        Schedule schedule = createSchedule("PENDING", "PHYSICAL");

        Doctor otherDoctor = mock(Doctor.class);
        when(otherDoctor.getId()).thenReturn("doctor-2");

        when(scheduleRepository.findById("schedule-1"))
                .thenReturn(Optional.of(schedule));
        when(doctorRepository.findByEmail("other@example.com"))
                .thenReturn(Optional.of(otherDoctor));

        assertThrows(
                SecurityException.class,
                () -> scheduleService.rejectScheduleByDoctor(
                        "schedule-1",
                        "other@example.com",
                        "Unavailable"
                )
        );

        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    // =========================================================
    // DOCTOR CANCELLATION TESTS
    // =========================================================

    @Test
    void cancelScheduleRejectsPendingSchedule() {
        Schedule schedule = createSchedule("PENDING", "PHYSICAL");

        when(scheduleRepository.findById("schedule-1"))
                .thenReturn(Optional.of(schedule));

        assertThrows(
                IllegalStateException.class,
                () -> scheduleService.cancelSchedule("schedule-1")
        );

        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    void cancelScheduleRejectsExpiredSchedule() {
        Schedule schedule = createSchedule("ACCEPTED", "PHYSICAL");
        schedule.setDate("2000-01-01");
        schedule.setEndTime("10:00");

        when(scheduleRepository.findById("schedule-1"))
                .thenReturn(Optional.of(schedule));

        assertThrows(
                IllegalArgumentException.class,
                () -> scheduleService.cancelSchedule("schedule-1")
        );

        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    void cancelAcceptedScheduleSuccessfullyWhenThereAreNoAppointments() {
        Schedule schedule = createSchedule("ACCEPTED", "PHYSICAL");

        when(scheduleRepository.findById("schedule-1"))
                .thenReturn(Optional.of(schedule));
        when(doctorRepository.findById("doctor-1"))
                .thenReturn(Optional.empty());
        when(hospitalRepository.findById("hospital-1"))
                .thenReturn(Optional.empty());
        when(scheduleRepository.save(any(Schedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(appointmentRepository.findByScheduleId("schedule-1"))
                .thenReturn(new ArrayList<>());

        Schedule result = scheduleService.cancelSchedule(
                "schedule-1", "Doctor unavailable"
        );

        assertEquals("CANCELLED", result.getStatus());
        assertEquals("Doctor unavailable", result.getCancellationReason());

        verify(scheduleRepository).save(schedule);
        verify(appointmentRepository).findByScheduleId("schedule-1");
        verify(notificationRepository, atLeastOnce())
                .save(any(Notification.class));
    }

    @Test
    void cancelScheduleRejectsAlreadyCancelledSchedule() {
        Schedule schedule = createSchedule("CANCELLED", "PHYSICAL");

        when(scheduleRepository.findById("schedule-1"))
                .thenReturn(Optional.of(schedule));

        assertThrows(
                IllegalStateException.class,
                () -> scheduleService.cancelSchedule("schedule-1")
        );

        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    void cancelScheduleByDoctorRejectsBlankId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> scheduleService.cancelScheduleByDoctor(
                        " ", "doctor@example.com", "Unavailable"
                )
        );

        verify(scheduleRepository, never()).findById(anyString());
    }

    @Test
    void doctorCannotCancelAnotherDoctorsScheduleWhenDoctorIsFound() {
        Schedule schedule = createSchedule("ACCEPTED", "PHYSICAL");

        Doctor otherDoctor = mock(Doctor.class);
        when(otherDoctor.getId()).thenReturn("doctor-2");

        when(scheduleRepository.findById("schedule-1"))
                .thenReturn(Optional.of(schedule));
        when(doctorRepository.findByEmail("other@example.com"))
                .thenReturn(Optional.of(otherDoctor));

        assertThrows(
                SecurityException.class,
                () -> scheduleService.cancelScheduleByDoctor(
                        "schedule-1",
                        "other@example.com",
                        "Unavailable"
                )
        );

        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    // =========================================================
    // HOSPITAL CANCELLATION AUTHORIZATION TESTS
    // =========================================================

    @Test
    void hospitalCancellationRequiresEmail() {
        assertThrows(
                SecurityException.class,
                () -> scheduleService.cancelScheduleByHospital(
                        "schedule-1", " ", "Reason"
                )
        );

        verify(hospitalRepository, never()).findByEmail(anyString());
    }

    @Test
    void hospitalCancellationRejectsUnknownHospital() {
        when(hospitalRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                SecurityException.class,
                () -> scheduleService.cancelScheduleByHospital(
                        "schedule-1",
                        "unknown@example.com",
                        "Reason"
                )
        );
    }

    @Test
    void hospitalCannotCancelAnotherHospitalsSchedule() {
        Hospital hospital = mock(Hospital.class);
        when(hospital.getId()).thenReturn("hospital-2");

        Schedule schedule = createSchedule("ACCEPTED", "PHYSICAL");

        when(hospitalRepository.findByEmail("hospital@example.com"))
                .thenReturn(Optional.of(hospital));
        when(scheduleRepository.findById("schedule-1"))
                .thenReturn(Optional.of(schedule));

        assertThrows(
                SecurityException.class,
                () -> scheduleService.cancelScheduleByHospital(
                        "schedule-1",
                        "hospital@example.com",
                        "Reason"
                )
        );

        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    void hospitalCannotCancelRejectedSchedule() {
        Hospital hospital = mock(Hospital.class);
        when(hospital.getId()).thenReturn("hospital-1");

        Schedule schedule = createSchedule("REJECTED", "PHYSICAL");

        when(hospitalRepository.findByEmail("hospital@example.com"))
                .thenReturn(Optional.of(hospital));
        when(scheduleRepository.findById("schedule-1"))
                .thenReturn(Optional.of(schedule));

        assertThrows(
                IllegalArgumentException.class,
                () -> scheduleService.cancelScheduleByHospital(
                        "schedule-1",
                        "hospital@example.com",
                        "Reason"
                )
        );

        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    // =========================================================
    // DATE/TIME HELPER TESTS
    // =========================================================

    @Test
    void isSchedulePastRecognizesPastDate() {
        assertTrue(
                ScheduleService.isSchedulePast(
                        "2000-01-01", "10:00"
                )
        );
    }

    @Test
    void isSchedulePastReturnsFalseForFutureDate() {
        assertFalse(
                ScheduleService.isSchedulePast(
                        futureDate(), "10:00"
                )
        );
    }

    @Test
    void isSchedulePastSupportsAlternativeDateFormat() {
        assertTrue(
                ScheduleService.isSchedulePast(
                        "01/01/2000", "10:00"
                )
        );
    }

    @Test
    void isSchedulePastReturnsFalseForBlankDate() {
        assertFalse(ScheduleService.isSchedulePast(" ", "10:00"));
    }

    @Test
    void isSchedulePastReturnsFalseForUnrecognizedDate() {
        assertFalse(
                ScheduleService.isSchedulePast(
                        "not-a-date", "10:00"
                )
        );
    }
}
