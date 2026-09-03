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

	// 1. Patient A -> Doctor X -> Physical Schedule 1 -> Initial booking succeeds as PENDING and unpaid
	@Test
	void testPhysicalAppointment_FirstBooking_InitialStateIsPendingAndUnpaid() {
		Appointment appointment = new Appointment();
		appointment.setPatientId("patientA");
		appointment.setDoctorId("doctorX");
		appointment.setScheduleId("schedule1");
		appointment.setAppointmentNumber("1");
		appointment.setDate("2026-09-10");
		appointment.setTime("10:00 - 11:00");

		when(appointmentRepository.existsByPatientIdAndDoctorIdAndScheduleId("patientA", "doctorX", "schedule1"))
				.thenReturn(false);
		when(appointmentRepository.save(any(Appointment.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		Appointment result = appointmentService.bookAppointment(appointment);

		assertNotNull(result);
		assertEquals("APT-001", result.getAppointmentNumber());
		assertEquals("PENDING", result.getStatus());
		assertEquals("PENDING", result.getPaymentStatus());
		assertFalse(result.isPaid());
		assertEquals(1000.00, result.getAmount());
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

	// 3. Physical Appointment -> Payment Success -> Transitions to CONFIRMED and PAID
	@Test
	void testPhysicalAppointment_PaymentSuccess_TransitionsToConfirmedAndPaid() {
		Appointment pendingAppt = new Appointment();
		pendingAppt.setId("appt-001");
		pendingAppt.setPatientId("patientA");
		pendingAppt.setDoctorId("doctorX");
		pendingAppt.setScheduleId("schedule1");
		pendingAppt.setAppointmentNumber("APT-001");
		pendingAppt.setDate("2026-09-10");
		pendingAppt.setTime("10:00 - 11:00");
		pendingAppt.setStatus("PENDING");
		pendingAppt.setPaymentStatus("PENDING");
		pendingAppt.setPaid(false);
		pendingAppt.setAmount(1000.00);

		Doctor doctor = new Doctor();
		doctor.setTitle("Dr.");
		doctor.setFirstName("John");
		doctor.setLastName("Doe");

		AppointmentRepository mockApptRepo = mock(AppointmentRepository.class);
		NotificationRepository mockNotifRepo = mock(NotificationRepository.class);
		DoctorRepository mockDocRepo = mock(DoctorRepository.class);
		HospitalRepository mockHospRepo = mock(HospitalRepository.class);
		LabTestRepository mockLabRepo = mock(LabTestRepository.class);

		when(mockApptRepo.findById("appt-001")).thenReturn(Optional.of(pendingAppt));
		when(mockApptRepo.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(mockDocRepo.findById("doctorX")).thenReturn(Optional.of(doctor));

		com.example.demo.service.PaymentService paymentService = new com.example.demo.service.PaymentService();
		org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "appointmentRepo", mockApptRepo);
		org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "notificationRepo", mockNotifRepo);
		org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "doctorRepo", mockDocRepo);
		org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "hospitalRepo", mockHospRepo);
		org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "labRepo", mockLabRepo);

		Appointment confirmedAppt = paymentService.confirmPaymentSuccess("appt-001", "PAYHERE-12345", 1000.00);

		assertNotNull(confirmedAppt);
		assertEquals("CONFIRMED", confirmedAppt.getStatus());
		assertEquals("PAID", confirmedAppt.getPaymentStatus());
		assertTrue(confirmedAppt.isPaid());
		assertEquals("PAYHERE-12345", confirmedAppt.getPayhereId());
		assertNotNull(confirmedAppt.getPaidAt());

		// Verify confirmation notification creation for Patient A
		ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
		verify(mockNotifRepo, times(1)).save(notifCaptor.capture());
		Notification capturedNotif = notifCaptor.getValue();
		assertEquals("patientA", capturedNotif.getUserId());
		assertEquals("Appointment Confirmed", capturedNotif.getTitle());
		assertTrue(capturedNotif.getMessage().contains("Dr. John Doe"));
	}

	// 4. Physical Appointment -> Cancelled / Unpaid -> Remains PENDING
	@Test
	void testPhysicalAppointment_UnpaidOrCancelled_RemainsPending() {
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

		// Without payment confirmation, status remains PENDING and unpaid
		assertEquals("PENDING", result.getStatus());
		assertEquals("PENDING", result.getPaymentStatus());
		assertFalse(result.isPaid());
	}

	// 6. Payment History: Patient A retrieves their own payment records
	@Test
	void testPatientPaymentHistory_RetrievesPatientPayments() {
		Appointment appt1 = new Appointment();
		appt1.setId("order-101");
		appt1.setPatientId("patientA");
		appt1.setDoctorId("doctorX");
		appt1.setAppointmentNumber("APT-001");
		appt1.setDate("2026-09-10");
		appt1.setTime("10:00 - 11:00");
		appt1.setAmount(1000.00);
		appt1.setPaymentStatus("PAID");
		appt1.setPaid(true);
		appt1.setPayhereId("PAY-101");

		Doctor doc = new Doctor();
		doc.setTitle("Dr.");
		doc.setFirstName("John");
		doc.setLastName("Doe");

		AppointmentRepository mockApptRepo = mock(AppointmentRepository.class);
		DoctorRepository mockDocRepo = mock(DoctorRepository.class);
		HospitalRepository mockHospRepo = mock(HospitalRepository.class);
		LabTestRepository mockLabRepo = mock(LabTestRepository.class);

		when(mockApptRepo.findByPatientId("patientA")).thenReturn(java.util.List.of(appt1));
		when(mockDocRepo.findById("doctorX")).thenReturn(Optional.of(doc));
		when(mockLabRepo.findByPatientId("patientA")).thenReturn(java.util.Collections.emptyList());

		com.example.demo.service.PaymentService paymentService = new com.example.demo.service.PaymentService();
		org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "appointmentRepo", mockApptRepo);
		org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "doctorRepo", mockDocRepo);
		org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "hospitalRepo", mockHospRepo);
		org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "labRepo", mockLabRepo);

		java.util.List<com.example.demo.dto.PaymentHistoryDTO> history = paymentService.getPatientPaymentHistory("patientA");

		assertNotNull(history);
		assertEquals(1, history.size());
		assertEquals("order-101", history.get(0).getId());
		assertEquals("PAY-101", history.get(0).getTransactionId());
		assertEquals("PAID", history.get(0).getPaymentStatus());
		assertTrue(history.get(0).isPaid());
		assertEquals(1000.00, history.get(0).getAmount());
		assertEquals("Dr. John Doe", history.get(0).getDoctorName());
		assertTrue(history.get(0).getDescription().contains("Dr. John Doe"));
	}

	// 7. Payment History: Patient isolation - Patient A cannot see Patient B's payments
	@Test
	void testPatientPaymentHistory_PatientIsolation() {
		Appointment apptB = new Appointment();
		apptB.setId("order-202");
		apptB.setPatientId("patientB");
		apptB.setAmount(1000.00);
		apptB.setPaymentStatus("PAID");

		AppointmentRepository mockApptRepo = mock(AppointmentRepository.class);
		when(mockApptRepo.findByPatientId("patientA")).thenReturn(java.util.Collections.emptyList());
		when(mockApptRepo.findByPatientId("patientB")).thenReturn(java.util.List.of(apptB));

		com.example.demo.service.PaymentService paymentService = new com.example.demo.service.PaymentService();
		org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "appointmentRepo", mockApptRepo);

		java.util.List<com.example.demo.dto.PaymentHistoryDTO> historyA = paymentService.getPatientPaymentHistory("patientA");
		java.util.List<com.example.demo.dto.PaymentHistoryDTO> historyB = paymentService.getPatientPaymentHistory("patientB");

		assertEquals(0, historyA.size());
		assertEquals(1, historyB.size());
		assertEquals("order-202", historyB.get(0).getId());
	}
}
