package com.example.demo;

import com.example.demo.model.Appointment;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DemoApplicationTests {

	@Mock
	private AppointmentRepository appointmentRepository;

	@InjectMocks
	private AppointmentService appointmentService;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void contextLoads() {
	}

	// 1. Patient A -> Doctor X -> Physical Schedule 1 -> First booking succeeds
	@Test
	void testPhysicalAppointment_FirstBooking_Succeeds() {
		Appointment appointment = new Appointment();
		appointment.setPatientId("patientA");
		appointment.setDoctorId("doctorX");
		appointment.setScheduleId("schedule1");
		appointment.setAppointmentNumber("1");

		when(appointmentRepository.existsByPatientIdAndDoctorIdAndScheduleId("patientA", "doctorX", "schedule1"))
				.thenReturn(false);
		when(appointmentRepository.save(any(Appointment.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		Appointment result = appointmentService.bookAppointment(appointment);

		assertNotNull(result);
		assertEquals("APT-001", result.getAppointmentNumber());
		assertEquals("CONFIRMED", result.getStatus());
		verify(appointmentRepository, times(1)).save(appointment);
	}

	// 2. Patient A -> Doctor X -> Physical Schedule 1 -> Second booking is rejected
	@Test
	void testPhysicalAppointment_DuplicateBooking_Rejected() {
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
	}

	// 5. Patient B -> Doctor X -> Physical Schedule 1 -> Booking succeeds
	@Test
	void testPhysicalAppointment_DifferentPatient_Succeeds() {
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
	}
}
