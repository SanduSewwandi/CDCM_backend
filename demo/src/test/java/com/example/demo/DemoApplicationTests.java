package com.example.demo;

import com.example.demo.model.Appointment;
import com.example.demo.model.Doctor;
import com.example.demo.model.Notification;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.DoctorRepository;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DemoApplicationTests {

	@Mock
	private AppointmentRepository appointmentRepository;

	@Mock
	private NotificationRepository notificationRepository;

	@Mock
	private DoctorRepository doctorRepository;

	@InjectMocks
	private AppointmentService appointmentService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void contextLoads() {
	}

	// 1. Patient A -> Doctor X -> Physical Schedule 1 -> First booking succeeds & creates notification
	@Test
	void testPhysicalAppointment_FirstBooking_SucceedsAndCreatesNotification() {
		Appointment appointment = new Appointment();
		appointment.setPatientId("patientA");
		appointment.setDoctorId("doctorX");
		appointment.setScheduleId("schedule1");
		appointment.setAppointmentNumber("1");
		appointment.setDate("2026-09-10");
		appointment.setTime("10:00 - 11:00");

		Doctor doctor = new Doctor();
		doctor.setTitle("Dr.");
		doctor.setFirstName("John");
		doctor.setLastName("Doe");

		when(appointmentRepository.existsByPatientIdAndDoctorIdAndScheduleId("patientA", "doctorX", "schedule1"))
				.thenReturn(false);
		when(appointmentRepository.save(any(Appointment.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(doctorRepository.findById("doctorX")).thenReturn(Optional.of(doctor));

		Appointment result = appointmentService.bookAppointment(appointment);

		assertNotNull(result);
		assertEquals("APT-001", result.getAppointmentNumber());
		assertEquals("CONFIRMED", result.getStatus());
		verify(appointmentRepository, times(1)).save(appointment);

		// Verify notification creation for Patient A
		ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
		verify(notificationRepository, times(1)).save(notifCaptor.capture());
		Notification capturedNotif = notifCaptor.getValue();
		assertEquals("patientA", capturedNotif.getUserId());
		assertEquals("Appointment Booked Successfully", capturedNotif.getTitle());
		assertEquals("doctorX", capturedNotif.getDoctorId());
		assertEquals("Dr. John Doe", capturedNotif.getDoctorName());
		assertEquals("PHYSICAL", capturedNotif.getScheduleType());
		assertFalse(capturedNotif.isRead());
		assertTrue(capturedNotif.getMessage().contains("Dr. John Doe"));
	}

	// 2. Patient A -> Doctor X -> Physical Schedule 1 -> Second booking is rejected & NO notification created
	@Test
	void testPhysicalAppointment_DuplicateBooking_RejectedAndNoNotification() {
		Appointment appointment = new Appointment();
		appointment.setPatientId("patientA");
		appointment.setDoctorId("doctorX");
		appointment.setScheduleId("schedule1");
		appointment.setAppointmentNumber("2");

		when(appointmentRepository.existsByPatientIdAndDoctorIdAndScheduleId("patientA", "doctorX", "schedule1"))
				.thenReturn(true);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			appointmentService.bookAppointment(appointment);
		});

		assertEquals("You have already booked this doctor's schedule.", exception.getMessage());
		verify(appointmentRepository, never()).save(any(Appointment.class));
		verify(notificationRepository, never()).save(any(Notification.class));
	}

	// 3. Patient A -> Doctor X -> Physical Schedule 2 -> Booking succeeds
	@Test
	void testPhysicalAppointment_DifferentSchedule_Succeeds() {
		Appointment appointment = new Appointment();
		appointment.setPatientId("patientA");
		appointment.setDoctorId("doctorX");
		appointment.setScheduleId("schedule2");
		appointment.setAppointmentNumber("1");

		when(appointmentRepository.existsByPatientIdAndDoctorIdAndScheduleId("patientA", "doctorX", "schedule2"))
				.thenReturn(false);
		when(appointmentRepository.save(any(Appointment.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		Appointment result = appointmentService.bookAppointment(appointment);

		assertNotNull(result);
		assertEquals("APT-001", result.getAppointmentNumber());
		assertEquals("CONFIRMED", result.getStatus());
		verify(appointmentRepository, times(1)).save(appointment);
		verify(notificationRepository, times(1)).save(any(Notification.class));
	}

	// 4. Patient A -> Doctor Y -> Physical Schedule 1 -> Booking succeeds
	@Test
	void testPhysicalAppointment_DifferentDoctor_Succeeds() {
		Appointment appointment = new Appointment();
		appointment.setPatientId("patientA");
		appointment.setDoctorId("doctorY");
		appointment.setScheduleId("schedule1");
		appointment.setAppointmentNumber("1");

		when(appointmentRepository.existsByPatientIdAndDoctorIdAndScheduleId("patientA", "doctorY", "schedule1"))
				.thenReturn(false);
		when(appointmentRepository.save(any(Appointment.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		Appointment result = appointmentService.bookAppointment(appointment);

		assertNotNull(result);
		assertEquals("APT-001", result.getAppointmentNumber());
		assertEquals("CONFIRMED", result.getStatus());
		verify(appointmentRepository, times(1)).save(appointment);
		verify(notificationRepository, times(1)).save(any(Notification.class));
	}

	// 5. Patient B -> Doctor X -> Physical Schedule 1 -> Booking succeeds & notification belongs to Patient B
	@Test
	void testPhysicalAppointment_DifferentPatient_SucceedsAndNotifiesPatientB() {
		Appointment appointment = new Appointment();
		appointment.setPatientId("patientB");
		appointment.setDoctorId("doctorX");
		appointment.setScheduleId("schedule1");
		appointment.setAppointmentNumber("1");

		when(appointmentRepository.existsByPatientIdAndDoctorIdAndScheduleId("patientB", "doctorX", "schedule1"))
				.thenReturn(false);
		when(appointmentRepository.save(any(Appointment.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		Appointment result = appointmentService.bookAppointment(appointment);

		assertNotNull(result);
		assertEquals("APT-001", result.getAppointmentNumber());
		assertEquals("CONFIRMED", result.getStatus());
		verify(appointmentRepository, times(1)).save(appointment);

		ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
		verify(notificationRepository, times(1)).save(notifCaptor.capture());
		Notification capturedNotif = notifCaptor.getValue();
		assertEquals("patientB", capturedNotif.getUserId());
		assertNotEquals("patientA", capturedNotif.getUserId());
	}
}
