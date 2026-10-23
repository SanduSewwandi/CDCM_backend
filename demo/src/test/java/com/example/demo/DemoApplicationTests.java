package com.example.demo;

import com.example.demo.model.Appointment;
import com.example.demo.model.Doctor;
import com.example.demo.model.Hospital;
import com.example.demo.model.Notification;
import com.example.demo.model.Patient;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.DoctorRepository;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.repository.HospitalRepository;
import com.example.demo.repository.LabTestRepository;
import com.example.demo.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.example.demo.service.ChatService;

import java.util.Optional;
import com.example.demo.model.Schedule;
import com.example.demo.repository.ScheduleRepository;
import com.example.demo.repository.PatientRepository;
import com.example.demo.dto.AppointmentResponseDTO;
import com.example.demo.dto.PaymentNotificationDTO;
import com.example.demo.service.PaymentService;
import com.example.demo.service.ScheduleService;
import com.example.demo.service.SmsService;
import com.example.demo.service.EmailService;
import com.example.demo.model.Conversation;
import com.example.demo.model.Message;
import com.example.demo.repository.ConversationRepository;
import com.example.demo.repository.MessageRepository;
import com.example.demo.dto.ConversationResponseDTO;
import com.example.demo.dto.SendMessageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.time.LocalDateTime;

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

	@Mock
	private ScheduleRepository scheduleRepository;

	@Mock
	private PatientRepository patientRepository;

	@Mock
	private HospitalRepository hospitalRepository;

	@Mock
	private ChatService chatService;

	@Mock
	private SmsService smsService;

	@Mock
	private EmailService emailService;

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
		verify(notificationRepository, never()).save(any(Notification.class));
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
		pendingAppt.setHospitalId("hospital-001");
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
		com.example.demo.repository.ScheduleRepository mockScheduleRepo = mock(com.example.demo.repository.ScheduleRepository.class);

		when(mockApptRepo.findById("appt-001")).thenReturn(Optional.of(pendingAppt));
		when(mockApptRepo.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(mockDocRepo.findById("doctorX")).thenReturn(Optional.of(doctor));

		com.example.demo.service.PaymentService paymentService = new com.example.demo.service.PaymentService();
		org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "appointmentRepo", mockApptRepo);
		org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "notificationRepo", mockNotifRepo);
		org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "doctorRepo", mockDocRepo);
		org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "hospitalRepo", mockHospRepo);
		org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "labRepo", mockLabRepo);
		org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "scheduleRepo", mockScheduleRepo);

		Appointment confirmedAppt = paymentService.confirmPaymentSuccess("appt-001", "PAYHERE-12345", 1000.00);

		assertNotNull(confirmedAppt);
		assertEquals("CONFIRMED", confirmedAppt.getStatus());
		assertEquals("PAID", confirmedAppt.getPaymentStatus());
		assertTrue(confirmedAppt.isPaid());
		assertEquals("PAYHERE-12345", confirmedAppt.getPayhereId());
		assertNotNull(confirmedAppt.getPaidAt());

		// Verify confirmation notifications created for Patient, Doctor, and Hospital
		ArgumentCaptor<Notification> notifCaptor = ArgumentCaptor.forClass(Notification.class);
		verify(mockNotifRepo, times(3)).save(notifCaptor.capture());
		java.util.List<Notification> capturedNotifs = notifCaptor.getAllValues();

		// Patient notification
		assertTrue(capturedNotifs.stream().anyMatch(n -> 
				"patientA".equals(n.getUserId()) && "Appointment Confirmed".equals(n.getTitle()) && n.getMessage().contains("Dr. John Doe")));

		// Doctor notification
		assertTrue(capturedNotifs.stream().anyMatch(n -> 
				"doctorX".equals(n.getUserId()) && n.getMessage().contains("APT-001")));

		// Hospital notification
		assertTrue(capturedNotifs.stream().anyMatch(n -> 
				"hospital-001".equals(n.getUserId()) && "hospital-001".equals(n.getHospitalId()) && "Payment Confirmed".equals(n.getTitle())));
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
		verify(notificationRepository, never()).save(any(Notification.class));
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

	// ========================================================
	// DOCTOR MY PATIENTS vs MY SCHEDULE VALIDATION TESTS
	// ========================================================

	private ScheduleService createScheduleService() {
		return new ScheduleService(
				scheduleRepository,
				doctorRepository,
				hospitalRepository,
				notificationRepository,
				appointmentRepository,
				patientRepository,
				smsService,
				emailService
		);
	}

	// CASE 1: Existing valid schedule + valid appointment => Patient appears.
	@Test
	void testDoctorMyPatients_Case1_ExistingSchedule_Appears() {
		Schedule s1 = new Schedule();
		s1.setId("sched-1");
		s1.setDoctorId("doc1");
		s1.setType("PHYSICAL");
		s1.setStatus("ACCEPTED");

		Appointment a1 = new Appointment();
		a1.setId("appt-1");
		a1.setPatientId("patient-1");
		a1.setDoctorId("doc1");
		a1.setScheduleId("sched-1");
		a1.setAppointmentNumber("1");
		a1.setDate("2026-10-15");
		a1.setStatus("CONFIRMED");

		when(appointmentRepository.findByDoctorId("doc1")).thenReturn(java.util.List.of(a1));
		when(scheduleRepository.findAllById(any())).thenReturn(java.util.List.of(s1));

		java.util.List<AppointmentResponseDTO> result = appointmentService.getAppointmentsByDoctor("doc1");

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals("appt-1", result.get(0).getId());
		assertEquals("patient-1", result.get(0).getPatientId());
	}

	// CASE 2: Deleted/non-existing schedule + appointment remains => Patient does NOT appear.
	@Test
	void testDoctorMyPatients_Case2_DeletedSchedule_DoesNotAppear() {
		Appointment orphanAppt = new Appointment();
		orphanAppt.setId("appt-orphan");
		orphanAppt.setPatientId("patient-orphan");
		orphanAppt.setDoctorId("doc1");
		orphanAppt.setScheduleId("sched-deleted");
		orphanAppt.setAppointmentNumber("2");
		orphanAppt.setDate("2026-10-10");
		orphanAppt.setStatus("CONFIRMED");

		when(appointmentRepository.findByDoctorId("doc1")).thenReturn(java.util.List.of(orphanAppt));
		when(scheduleRepository.findAllById(any())).thenReturn(java.util.Collections.emptyList());

		java.util.List<AppointmentResponseDTO> result = appointmentService.getAppointmentsByDoctor("doc1");

		assertNotNull(result);
		assertEquals(0, result.size());
	}

	// CASE 3: CANCELLED schedule + appointment remains => Patient does NOT appear.
	@Test
	void testDoctorMyPatients_Case3_CancelledSchedule_DoesNotAppear() {
		Schedule cancelledSchedule = new Schedule();
		cancelledSchedule.setId("sched-cancelled");
		cancelledSchedule.setDoctorId("doc1");
		cancelledSchedule.setType("PHYSICAL");
		cancelledSchedule.setStatus("CANCELLED");

		Appointment a = new Appointment();
		a.setId("appt-3");
		a.setPatientId("patient-3");
		a.setDoctorId("doc1");
		a.setScheduleId("sched-cancelled");
		a.setStatus("CONFIRMED");

		when(appointmentRepository.findByDoctorId("doc1")).thenReturn(java.util.List.of(a));
		when(scheduleRepository.findAllById(any())).thenReturn(java.util.List.of(cancelledSchedule));

		java.util.List<AppointmentResponseDTO> result = appointmentService.getAppointmentsByDoctor("doc1");

		assertNotNull(result);
		assertEquals(0, result.size());
	}

	// CASE 4: REJECTED schedule + appointment remains => Patient does NOT appear.
	@Test
	void testDoctorMyPatients_Case4_RejectedSchedule_DoesNotAppear() {
		Schedule rejectedSchedule = new Schedule();
		rejectedSchedule.setId("sched-rejected");
		rejectedSchedule.setDoctorId("doc1");
		rejectedSchedule.setType("PHYSICAL");
		rejectedSchedule.setStatus("REJECTED");

		Appointment a = new Appointment();
		a.setId("appt-4");
		a.setPatientId("patient-4");
		a.setDoctorId("doc1");
		a.setScheduleId("sched-rejected");
		a.setStatus("CONFIRMED");

		when(appointmentRepository.findByDoctorId("doc1")).thenReturn(java.util.List.of(a));
		when(scheduleRepository.findAllById(any())).thenReturn(java.util.List.of(rejectedSchedule));

		java.util.List<AppointmentResponseDTO> result = appointmentService.getAppointmentsByDoctor("doc1");

		assertNotNull(result);
		assertEquals(0, result.size());
	}

	// CASE 5: Existing valid schedule with multiple patients => All valid patients appear.
	@Test
	void testDoctorMyPatients_Case5_MultipleBookedPatientsUnderExistingSchedule_AllAppear() {
		Schedule s1 = new Schedule();
		s1.setId("sched-1");
		s1.setDoctorId("doc1");
		s1.setType("PHYSICAL");
		s1.setStatus("ACCEPTED");

		Appointment a1 = new Appointment();
		a1.setId("appt-1");
		a1.setPatientId("patient-1");
		a1.setDoctorId("doc1");
		a1.setScheduleId("sched-1");
		a1.setStatus("CONFIRMED");

		Appointment a2 = new Appointment();
		a2.setId("appt-2");
		a2.setPatientId("patient-2");
		a2.setDoctorId("doc1");
		a2.setScheduleId("sched-1");
		a2.setStatus("CONFIRMED");

		Appointment a3 = new Appointment();
		a3.setId("appt-3");
		a3.setPatientId("patient-3");
		a3.setDoctorId("doc1");
		a3.setScheduleId("sched-1");
		a3.setStatus("CONFIRMED");

		when(appointmentRepository.findByDoctorId("doc1")).thenReturn(java.util.List.of(a1, a2, a3));
		when(scheduleRepository.findAllById(any())).thenReturn(java.util.List.of(s1));

		java.util.List<AppointmentResponseDTO> result = appointmentService.getAppointmentsByDoctor("doc1");

		assertNotNull(result);
		assertEquals(3, result.size());
	}

	// CASE 6: Schedule belongs to another doctor => Patient does NOT appear.
	@Test
	void testDoctorMyPatients_Case6_AnotherDoctorSchedule_DoesNotAppear() {
		Schedule otherDocSchedule = new Schedule();
		otherDocSchedule.setId("sched-other-doc");
		otherDocSchedule.setDoctorId("doc-DIFFERENT");
		otherDocSchedule.setType("PHYSICAL");
		otherDocSchedule.setStatus("ACCEPTED");

		Appointment mismatchAppt = new Appointment();
		mismatchAppt.setId("appt-mismatch");
		mismatchAppt.setPatientId("patient-mismatch");
		mismatchAppt.setDoctorId("doc1");
		mismatchAppt.setScheduleId("sched-other-doc");
		mismatchAppt.setStatus("CONFIRMED");

		when(appointmentRepository.findByDoctorId("doc1")).thenReturn(java.util.List.of(mismatchAppt));
		when(scheduleRepository.findAllById(any())).thenReturn(java.util.List.of(otherDocSchedule));

		java.util.List<AppointmentResponseDTO> result = appointmentService.getAppointmentsByDoctor("doc1");

		assertNotNull(result);
		assertEquals(0, result.size());
	}

	// CASE 7: Valid PHYSICAL schedule => Preserve.
	@Test
	void testDoctorMyPatients_Case7_ValidPhysicalSchedule_Preserved() {
		Schedule physSchedule = new Schedule();
		physSchedule.setId("sched-phys-1");
		physSchedule.setDoctorId("doc1");
		physSchedule.setType("PHYSICAL");
		physSchedule.setStatus("ACCEPTED");

		Appointment physAppt = new Appointment();
		physAppt.setId("appt-phys-1");
		physAppt.setPatientId("patient-phys-1");
		physAppt.setDoctorId("doc1");
		physAppt.setScheduleId("sched-phys-1");
		physAppt.setStatus("CONFIRMED");

		when(appointmentRepository.findByDoctorId("doc1")).thenReturn(java.util.List.of(physAppt));
		when(scheduleRepository.findAllById(any())).thenReturn(java.util.List.of(physSchedule));

		java.util.List<AppointmentResponseDTO> result = appointmentService.getAppointmentsByDoctor("doc1");

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals("appt-phys-1", result.get(0).getId());
	}

	// CASE 8: Valid VIDEO schedule => Preserve.
	@Test
	void testDoctorMyPatients_Case8_ValidVideoSchedule_Preserved() {
		Schedule videoSchedule = new Schedule();
		videoSchedule.setId("sched-video-1");
		videoSchedule.setDoctorId("doc1");
		videoSchedule.setType("VIDEO");
		videoSchedule.setStatus("ACCEPTED");

		Appointment videoAppt = new Appointment();
		videoAppt.setId("appt-video-1");
		videoAppt.setPatientId("patient-video-1");
		videoAppt.setDoctorId("doc1");
		videoAppt.setScheduleId("sched-video-1");
		videoAppt.setStatus("CONFIRMED");

		when(appointmentRepository.findByDoctorId("doc1")).thenReturn(java.util.List.of(videoAppt));
		when(scheduleRepository.findAllById(any())).thenReturn(java.util.List.of(videoSchedule));

		java.util.List<AppointmentResponseDTO> result = appointmentService.getAppointmentsByDoctor("doc1");

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals("appt-video-1", result.get(0).getId());
	}

	// EXTRA MY PATIENTS: Cancelled appointment does NOT appear
	@Test
	void testDoctorMyPatients_CancelledAppointment_DoesNotAppear() {
		Schedule s = new Schedule();
		s.setId("sched-valid");
		s.setDoctorId("doc1");
		s.setStatus("ACCEPTED");

		Appointment cancelledAppt = new Appointment();
		cancelledAppt.setId("appt-c");
		cancelledAppt.setDoctorId("doc1");
		cancelledAppt.setScheduleId("sched-valid");
		cancelledAppt.setStatus("CANCELLED");

		when(appointmentRepository.findByDoctorId("doc1")).thenReturn(java.util.List.of(cancelledAppt));
		when(scheduleRepository.findAllById(any())).thenReturn(java.util.List.of(s));

		java.util.List<AppointmentResponseDTO> result = appointmentService.getAppointmentsByDoctor("doc1");

		assertNotNull(result);
		assertEquals(0, result.size());
	}

	// ========================================================
	// SCHEDULE CANCELLATION RESTRICTION TESTS
	// ========================================================

	// CASE 9: Future ACCEPTED schedule + zero active bookings => Doctor can cancel.
	@Test
	void testCancelSchedule_Case9_FutureAcceptedZeroBookings_Success() {
		Schedule schedule = new Schedule();
		schedule.setId("sched-future-0");
		schedule.setStatus("ACCEPTED");
		schedule.setDate("2099-01-01");
		schedule.setEndTime("12:00 PM");

		when(scheduleRepository.findById("sched-future-0")).thenReturn(Optional.of(schedule));
		when(appointmentRepository.existsByScheduleIdAndStatusIn(eq("sched-future-0"), any())).thenReturn(false);
		when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(appointmentRepository.findByScheduleId("sched-future-0")).thenReturn(java.util.Collections.emptyList());

		ScheduleService scheduleService = createScheduleService();
		Schedule cancelled = scheduleService.cancelSchedule("sched-future-0");

		assertNotNull(cancelled);
		assertEquals("CANCELLED", cancelled.getStatus());
		verify(scheduleRepository, times(1)).save(schedule);
	}

	// CASE 10: Future ACCEPTED schedule + one active booking => Doctor can cancel, cascades to booking with 100% refund in AWAITING_BANK_DETAILS.
	@Test
	void testCancelSchedule_Case10_FutureAcceptedOneActiveBooking_ThrowsException() {
		Schedule schedule = new Schedule();
		schedule.setId("sched-future-1");
		schedule.setStatus("ACCEPTED");
		schedule.setDate("2099-01-01");
		schedule.setStartTime("10:00 AM");
		schedule.setEndTime("12:00 PM");
		schedule.setDoctorId("doc1");
		schedule.setHospitalId("hosp1");
		schedule.setType("PHYSICAL");

		Appointment appt = new Appointment();
		appt.setId("appt-future-1");
		appt.setScheduleId("sched-future-1");
		appt.setPatientId("patient-1");
		appt.setDoctorId("doc1");
		appt.setHospitalId("hosp1");
		appt.setStatus("CONFIRMED");
		appt.setPaid(true);
		appt.setPaymentStatus("PAID");
		appt.setAmount(2000.0);

		Doctor doctor = new Doctor();
		doctor.setId("doc1");
		doctor.setFirstName("John");
		doctor.setLastName("Doe");

		Hospital hospital = new Hospital();
		hospital.setId("hosp1");
		hospital.setName("General Hospital");

		Patient patient = new Patient();
		patient.setId("patient-1");
		patient.setEmail("patient1@example.com");
		patient.setContactNumber("0771234567");
		patient.setFirstName("Patient One");

		when(scheduleRepository.findById("sched-future-1")).thenReturn(Optional.of(schedule));
		when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(doctorRepository.findById("doc1")).thenReturn(Optional.of(doctor));
		when(hospitalRepository.findById("hosp1")).thenReturn(Optional.of(hospital));
		when(patientRepository.findById("patient-1")).thenReturn(Optional.of(patient));
		when(appointmentRepository.findByScheduleId("sched-future-1")).thenReturn(java.util.List.of(appt));
		when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ScheduleService scheduleService = createScheduleService();
		Schedule cancelled = scheduleService.cancelSchedule("sched-future-1");

		assertNotNull(cancelled);
		assertEquals("CANCELLED", cancelled.getStatus());
		verify(scheduleRepository, times(1)).save(schedule);

		assertEquals("CANCELLED", appt.getStatus());
		assertEquals("DOCTOR", appt.getCancelledBy());
		assertEquals(100.0, appt.getRefundPercentage());
		assertEquals(2000.0, appt.getRefundAmount());
		assertEquals("AWAITING_BANK_DETAILS", appt.getRefundStatus());
		assertNull(appt.getRefundedAt());
		verify(appointmentRepository, times(1)).save(appt);
	}

	// CASE 11: Future ACCEPTED schedule + multiple active bookings => Doctor can cancel, cascade cancellation consistently without false refunds.
	@Test
	void testCancelSchedule_Case11_FutureAcceptedMultipleActiveBookings_ThrowsException() {
		Schedule schedule = new Schedule();
		schedule.setId("sched-future-multi");
		schedule.setStatus("ACCEPTED");
		schedule.setDate("2099-01-01");
		schedule.setStartTime("10:00 AM");
		schedule.setEndTime("12:00 PM");
		schedule.setDoctorId("doc1");
		schedule.setHospitalId("hosp1");
		schedule.setType("PHYSICAL");

		Doctor doctor = new Doctor();
		doctor.setId("doc1");
		doctor.setFirstName("John");
		doctor.setLastName("Doe");

		Hospital hospital = new Hospital();
		hospital.setId("hosp1");
		hospital.setName("General Hospital");

		Appointment appt1 = new Appointment();
		appt1.setId("appt-multi-paid");
		appt1.setScheduleId("sched-future-multi");
		appt1.setPatientId("patient-1");
		appt1.setDoctorId("doc1");
		appt1.setHospitalId("hosp1");
		appt1.setStatus("CONFIRMED");
		appt1.setPaid(true);
		appt1.setPaymentStatus("PAID");
		appt1.setAmount(2500.0);

		Appointment appt2 = new Appointment();
		appt2.setId("appt-multi-unpaid");
		appt2.setScheduleId("sched-future-multi");
		appt2.setPatientId("patient-2");
		appt2.setDoctorId("doc1");
		appt2.setHospitalId("hosp1");
		appt2.setStatus("PENDING");
		appt2.setPaid(false);
		appt2.setPaymentStatus("PENDING");
		appt2.setAmount(2500.0);

		Appointment appt3 = new Appointment();
		appt3.setId("appt-multi-cancelled");
		appt3.setScheduleId("sched-future-multi");
		appt3.setStatus("CANCELLED");
		appt3.setCancelledBy("PATIENT");
		appt3.setRefundStatus("AWAITING_BANK_DETAILS");

		when(scheduleRepository.findById("sched-future-multi")).thenReturn(Optional.of(schedule));
		when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(doctorRepository.findById("doc1")).thenReturn(Optional.of(doctor));
		when(hospitalRepository.findById("hosp1")).thenReturn(Optional.of(hospital));
		when(appointmentRepository.findByScheduleId("sched-future-multi")).thenReturn(java.util.List.of(appt1, appt2, appt3));
		when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ScheduleService scheduleService = createScheduleService();
		Schedule cancelled = scheduleService.cancelSchedule("sched-future-multi");

		assertNotNull(cancelled);
		assertEquals("CANCELLED", cancelled.getStatus());
		verify(scheduleRepository, times(1)).save(schedule);

		// Paid appointment: cancelled with 100% refund, awaiting bank details
		assertEquals("CANCELLED", appt1.getStatus());
		assertEquals("DOCTOR", appt1.getCancelledBy());
		assertEquals(100.0, appt1.getRefundPercentage());
		assertEquals(2500.0, appt1.getRefundAmount());
		assertEquals("AWAITING_BANK_DETAILS", appt1.getRefundStatus());
		assertNull(appt1.getRefundedAt());

		// Unpaid/pending appointment: cancelled with 0.0 refund, NOT_APPLICABLE, paymentStatus FAILED
		assertEquals("CANCELLED", appt2.getStatus());
		assertEquals("DOCTOR", appt2.getCancelledBy());
		assertEquals(0.0, appt2.getRefundPercentage());
		assertEquals(0.0, appt2.getRefundAmount());
		assertEquals("NOT_APPLICABLE", appt2.getRefundStatus());
		assertEquals("FAILED", appt2.getPaymentStatus());

		// Already cancelled appointment: unmodified
		assertEquals("PATIENT", appt3.getCancelledBy());
	}

	// CASE 12: Expired ACCEPTED schedule + zero bookings => Doctor cannot cancel because it is expired.
	@Test
	void testCancelSchedule_Case12_ExpiredAcceptedZeroBookings_ThrowsException() {
		Schedule schedule = new Schedule();
		schedule.setId("sched-past-0");
		schedule.setStatus("ACCEPTED");
		schedule.setDate("2020-01-01");
		schedule.setEndTime("10:00 AM");

		when(scheduleRepository.findById("sched-past-0")).thenReturn(Optional.of(schedule));

		ScheduleService scheduleService = createScheduleService();
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			scheduleService.cancelSchedule("sched-past-0");
		});

		assertEquals("Cannot cancel an expired schedule.", ex.getMessage());
		// Expired rule triggers first before checking bookings
		verify(appointmentRepository, never()).existsByScheduleIdAndStatusIn(any(), any());
		verify(scheduleRepository, never()).save(any(Schedule.class));
	}

	// CASE 13: Direct API/Postman cancellation attempt on schedule with active booking => Cancels schedule, cascades refunds, and prevents new bookings.
	@Test
	void testCancelSchedule_Case13_DirectAttemptOnScheduleWithActiveBooking_ScheduleUnchanged() {
		Schedule schedule = new Schedule();
		schedule.setId("sched-direct-attempt");
		schedule.setStatus("ACCEPTED");
		schedule.setDate("2099-05-20");
		schedule.setStartTime("02:00 PM");
		schedule.setEndTime("04:00 PM");
		schedule.setDoctorId("doc1");
		schedule.setHospitalId("hosp1");
		schedule.setType("PHYSICAL");

		Appointment activeAppt = new Appointment();
		activeAppt.setId("appt-direct-1");
		activeAppt.setScheduleId("sched-direct-attempt");
		activeAppt.setPatientId("patient-1");
		activeAppt.setDoctorId("doc1");
		activeAppt.setHospitalId("hosp1");
		activeAppt.setStatus("CONFIRMED");
		activeAppt.setPaid(true);
		activeAppt.setPaymentStatus("PAID");
		activeAppt.setAmount(1800.0);

		Doctor doc = new Doctor();
		doc.setId("doc1");
		doc.setFirstName("Jane");
		doc.setLastName("Smith");

		Hospital hosp = new Hospital();
		hosp.setId("hosp1");
		hosp.setName("Metro Hospital");

		when(scheduleRepository.findById("sched-direct-attempt")).thenReturn(Optional.of(schedule));
		when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(doctorRepository.findById("doc1")).thenReturn(Optional.of(doc));
		when(hospitalRepository.findById("hosp1")).thenReturn(Optional.of(hosp));
		when(appointmentRepository.findByScheduleId("sched-direct-attempt")).thenReturn(java.util.List.of(activeAppt));
		when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ScheduleService scheduleService = createScheduleService();
		Schedule cancelled = scheduleService.cancelSchedule("sched-direct-attempt");

		assertNotNull(cancelled);
		assertEquals("CANCELLED", cancelled.getStatus());
		verify(scheduleRepository, times(1)).save(schedule);

		assertEquals("CANCELLED", activeAppt.getStatus());
		assertEquals("DOCTOR", activeAppt.getCancelledBy());
		assertEquals(100.0, activeAppt.getRefundPercentage());
		assertEquals(1800.0, activeAppt.getRefundAmount());
		assertEquals("AWAITING_BANK_DETAILS", activeAppt.getRefundStatus());
		assertNull(activeAppt.getRefundedAt());

		// Ensure cancellation prevents new patient bookings on this schedule
		Appointment newBookingAttempt = new Appointment();
		newBookingAttempt.setPatientId("patient-new");
		newBookingAttempt.setDoctorId("doc1");
		newBookingAttempt.setScheduleId("sched-direct-attempt");
		newBookingAttempt.setAppointmentNumber("2");
		newBookingAttempt.setDate("2099-05-20");

		IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
			appointmentService.bookAppointment(newBookingAttempt);
		});
		assertTrue(ex.getMessage().contains("cancelled schedule"));
	}

	// CASE 14: Cancelled appointments do not block cancellation, and appointments are not deleted.
	@Test
	void testCancelSchedule_Case14_CancelledAppointmentsDoNotBlockCancellationAndAreNotDeleted() {
		Schedule schedule = new Schedule();
		schedule.setId("sched-with-cancelled");
		schedule.setStatus("ACCEPTED");
		schedule.setDate("2099-01-01");
		schedule.setEndTime("12:00 PM");

		Appointment cancelledAppt = new Appointment();
		cancelledAppt.setId("appt-cancelled");
		cancelledAppt.setStatus("CANCELLED");
		cancelledAppt.setPaymentStatus("FAILED");

		when(scheduleRepository.findById("sched-with-cancelled")).thenReturn(Optional.of(schedule));
		// Zero active bookings because only CANCELLED appointments exist
		when(appointmentRepository.existsByScheduleIdAndStatusIn(eq("sched-with-cancelled"), any())).thenReturn(false);
		when(scheduleRepository.save(any(Schedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(appointmentRepository.findByScheduleId("sched-with-cancelled")).thenReturn(java.util.List.of(cancelledAppt));

		ScheduleService scheduleService = createScheduleService();
		Schedule cancelled = scheduleService.cancelSchedule("sched-with-cancelled");

		assertEquals("CANCELLED", cancelled.getStatus());
		verify(appointmentRepository, never()).delete(any(Appointment.class));
		verify(appointmentRepository, never()).deleteById(any());
	}

	// ========================================================
	// CDCM DOCTOR-PATIENT ONE CONVERSATION & SECURITY TESTS
	// ========================================================

	private ChatService createTestChatService(
			ConversationRepository convRepo,
			MessageRepository msgRepo,
			SimpMessagingTemplate simpTemplate
	) {
		ChatService service = new ChatService();
		org.springframework.test.util.ReflectionTestUtils.setField(service, "conversationRepository", convRepo);
		org.springframework.test.util.ReflectionTestUtils.setField(service, "messageRepository", msgRepo);
		org.springframework.test.util.ReflectionTestUtils.setField(service, "patientRepository", patientRepository);
		org.springframework.test.util.ReflectionTestUtils.setField(service, "doctorRepository", doctorRepository);
		org.springframework.test.util.ReflectionTestUtils.setField(service, "appointmentRepository", appointmentRepository);
		org.springframework.test.util.ReflectionTestUtils.setField(service, "hospitalRepository", hospitalRepository);
		org.springframework.test.util.ReflectionTestUtils.setField(service, "messagingTemplate", simpTemplate != null ? simpTemplate : mock(SimpMessagingTemplate.class));
		return service;
	}

	private org.springframework.security.core.Authentication createAuth(String email, String role) {
		org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
		when(auth.isAuthenticated()).thenReturn(true);
		when(auth.getName()).thenReturn(email);
		doReturn(java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(role)))
				.when(auth).getAuthorities();
		return auth;
	}

	// --------------------------------------------------------
	// REQUIRED SECURITY TESTS (1 - 9)
	// --------------------------------------------------------

	// 1. Doctor D1 can read D1/P1 conversation.
	@Test
	void testSecurity1_DoctorD1CanReadD1P1Conversation() {
		ConversationRepository mockConvRepo = mock(ConversationRepository.class);
		MessageRepository mockMsgRepo = mock(MessageRepository.class);
		ChatService service = createTestChatService(mockConvRepo, mockMsgRepo, null);

		Doctor d1 = new Doctor();
		d1.setId("D1");
		d1.setEmail("doctor1@example.com");
		when(doctorRepository.findByEmail("doctor1@example.com")).thenReturn(Optional.of(d1));

		Conversation c = new Conversation();
		c.setId("conv-d1-p1");
		c.setDoctorId("D1");
		c.setPatientId("P1");
		when(mockConvRepo.findById("conv-d1-p1")).thenReturn(Optional.of(c));
		when(mockConvRepo.findByPatientIdAndDoctorId("P1", "D1")).thenReturn(java.util.List.of(c));
		when(mockMsgRepo.findByConversationIdOrderBySentAtAsc("conv-d1-p1")).thenReturn(java.util.List.of(new Message()));

		org.springframework.security.core.Authentication authD1 = createAuth("doctor1@example.com", "DOCTOR");
		java.util.List<Message> msgs = service.getMessages("conv-d1-p1", authD1);

		assertNotNull(msgs);
		assertEquals(1, msgs.size());
	}

	// 2. Patient P1 can read D1/P1 conversation.
	@Test
	void testSecurity2_PatientP1CanReadD1P1Conversation() {
		ConversationRepository mockConvRepo = mock(ConversationRepository.class);
		MessageRepository mockMsgRepo = mock(MessageRepository.class);
		ChatService service = createTestChatService(mockConvRepo, mockMsgRepo, null);

		com.example.demo.model.Patient p1 = new com.example.demo.model.Patient();
		p1.setId("P1");
		p1.setEmail("patient1@example.com");
		when(patientRepository.findByEmail("patient1@example.com")).thenReturn(Optional.of(p1));

		Conversation c = new Conversation();
		c.setId("conv-d1-p1");
		c.setDoctorId("D1");
		c.setPatientId("P1");
		when(mockConvRepo.findById("conv-d1-p1")).thenReturn(Optional.of(c));
		when(mockConvRepo.findByPatientIdAndDoctorId("P1", "D1")).thenReturn(java.util.List.of(c));
		when(mockMsgRepo.findByConversationIdOrderBySentAtAsc("conv-d1-p1")).thenReturn(java.util.List.of(new Message()));

		org.springframework.security.core.Authentication authP1 = createAuth("patient1@example.com", "PATIENT");
		java.util.List<Message> msgs = service.getMessages("conv-d1-p1", authP1);

		assertNotNull(msgs);
		assertEquals(1, msgs.size());
	}

	// 3. Doctor D2 cannot read D1/P1 conversation.
	@Test
	void testSecurity3_DoctorD2CannotReadD1P1Conversation() {
		ConversationRepository mockConvRepo = mock(ConversationRepository.class);
		MessageRepository mockMsgRepo = mock(MessageRepository.class);
		ChatService service = createTestChatService(mockConvRepo, mockMsgRepo, null);

		Doctor d2 = new Doctor();
		d2.setId("D2");
		d2.setEmail("doctor2@example.com");
		when(doctorRepository.findByEmail("doctor2@example.com")).thenReturn(Optional.of(d2));

		Conversation c = new Conversation();
		c.setId("conv-d1-p1");
		c.setDoctorId("D1");
		c.setPatientId("P1");
		when(mockConvRepo.findById("conv-d1-p1")).thenReturn(Optional.of(c));

		org.springframework.security.core.Authentication authD2 = createAuth("doctor2@example.com", "DOCTOR");
		assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
			service.getMessages("conv-d1-p1", authD2);
		});
	}

	// 4. Patient P2 cannot read D1/P1 conversation.
	@Test
	void testSecurity4_PatientP2CannotReadD1P1Conversation() {
		ConversationRepository mockConvRepo = mock(ConversationRepository.class);
		MessageRepository mockMsgRepo = mock(MessageRepository.class);
		ChatService service = createTestChatService(mockConvRepo, mockMsgRepo, null);

		com.example.demo.model.Patient p2 = new com.example.demo.model.Patient();
		p2.setId("P2");
		p2.setEmail("patient2@example.com");
		when(patientRepository.findByEmail("patient2@example.com")).thenReturn(Optional.of(p2));

		Conversation c = new Conversation();
		c.setId("conv-d1-p1");
		c.setDoctorId("D1");
		c.setPatientId("P1");
		when(mockConvRepo.findById("conv-d1-p1")).thenReturn(Optional.of(c));

		org.springframework.security.core.Authentication authP2 = createAuth("patient2@example.com", "PATIENT");
		assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
			service.getMessages("conv-d1-p1", authP2);
		});
	}

	// 5. Doctor D2 cannot send into D1/P1 conversation.
	@Test
	void testSecurity5_DoctorD2CannotSendIntoD1P1Conversation() {
		ConversationRepository mockConvRepo = mock(ConversationRepository.class);
		MessageRepository mockMsgRepo = mock(MessageRepository.class);
		ChatService service = createTestChatService(mockConvRepo, mockMsgRepo, null);

		Doctor d2 = new Doctor();
		d2.setId("D2");
		d2.setEmail("doctor2@example.com");
		when(doctorRepository.findByEmail("doctor2@example.com")).thenReturn(Optional.of(d2));

		Conversation c = new Conversation();
		c.setId("conv-d1-p1");
		c.setDoctorId("D1");
		c.setPatientId("P1");
		when(mockConvRepo.findById("conv-d1-p1")).thenReturn(Optional.of(c));

		SendMessageRequest req = new SendMessageRequest();
		req.setContent("Malicious inject");

		org.springframework.security.core.Authentication authD2 = createAuth("doctor2@example.com", "DOCTOR");
		assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
			service.sendMessage("conv-d1-p1", req, authD2);
		});
	}

	// 6. Patient P2 cannot send into D1/P1 conversation.
	@Test
	void testSecurity6_PatientP2CannotSendIntoD1P1Conversation() {
		ConversationRepository mockConvRepo = mock(ConversationRepository.class);
		MessageRepository mockMsgRepo = mock(MessageRepository.class);
		ChatService service = createTestChatService(mockConvRepo, mockMsgRepo, null);

		com.example.demo.model.Patient p2 = new com.example.demo.model.Patient();
		p2.setId("P2");
		p2.setEmail("patient2@example.com");
		when(patientRepository.findByEmail("patient2@example.com")).thenReturn(Optional.of(p2));

		Conversation c = new Conversation();
		c.setId("conv-d1-p1");
		c.setDoctorId("D1");
		c.setPatientId("P1");
		when(mockConvRepo.findById("conv-d1-p1")).thenReturn(Optional.of(c));

		SendMessageRequest req = new SendMessageRequest();
		req.setContent("Malicious inject");

		org.springframework.security.core.Authentication authP2 = createAuth("patient2@example.com", "PATIENT");
		assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
			service.sendMessage("conv-d1-p1", req, authP2);
		});
	}

	// 7. Sender ID cannot be spoofed through request body.
	@Test
	void testSecurity7_SenderIdCannotBeSpoofedThroughRequestBody() {
		ConversationRepository mockConvRepo = mock(ConversationRepository.class);
		MessageRepository mockMsgRepo = mock(MessageRepository.class);
		ChatService service = createTestChatService(mockConvRepo, mockMsgRepo, null);

		com.example.demo.model.Patient p1 = new com.example.demo.model.Patient();
		p1.setId("P1");
		p1.setEmail("patient1@example.com");
		when(patientRepository.findByEmail("patient1@example.com")).thenReturn(Optional.of(p1));

		Conversation c = new Conversation();
		c.setId("conv-d1-p1");
		c.setDoctorId("D1");
		c.setPatientId("P1");
		when(mockConvRepo.findById("conv-d1-p1")).thenReturn(Optional.of(c));
		when(mockConvRepo.findByPatientIdAndDoctorId("P1", "D1")).thenReturn(java.util.List.of(c));
		when(mockConvRepo.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));
		when(mockMsgRepo.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

		SendMessageRequest req = new SendMessageRequest();
		req.setContent("Hello doctor");
		req.setSenderId("D1"); // Spoofing doctor's ID!
		req.setSenderRole("PATIENT");

		org.springframework.security.core.Authentication authP1 = createAuth("patient1@example.com", "PATIENT");
		Message saved = service.sendMessage("conv-d1-p1", req, authP1);

		assertNotNull(saved);
		assertEquals("P1", saved.getSenderId(), "Sender ID must match authenticated user, not spoofed request body ID");
	}

	// 8. Sender role cannot be spoofed through request body.
	@Test
	void testSecurity8_SenderRoleCannotBeSpoofedThroughRequestBody() {
		ConversationRepository mockConvRepo = mock(ConversationRepository.class);
		MessageRepository mockMsgRepo = mock(MessageRepository.class);
		ChatService service = createTestChatService(mockConvRepo, mockMsgRepo, null);

		com.example.demo.model.Patient p1 = new com.example.demo.model.Patient();
		p1.setId("P1");
		p1.setEmail("patient1@example.com");
		when(patientRepository.findByEmail("patient1@example.com")).thenReturn(Optional.of(p1));

		Conversation c = new Conversation();
		c.setId("conv-d1-p1");
		c.setDoctorId("D1");
		c.setPatientId("P1");
		when(mockConvRepo.findById("conv-d1-p1")).thenReturn(Optional.of(c));
		when(mockConvRepo.findByPatientIdAndDoctorId("P1", "D1")).thenReturn(java.util.List.of(c));
		when(mockConvRepo.save(any(Conversation.class))).thenAnswer(inv -> inv.getArgument(0));
		when(mockMsgRepo.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

		SendMessageRequest req = new SendMessageRequest();
		req.setContent("Prescription written");
		req.setSenderId("P1");
		req.setSenderRole("DOCTOR"); // Spoofing doctor role as patient!

		org.springframework.security.core.Authentication authP1 = createAuth("patient1@example.com", "PATIENT");
		Message saved = service.sendMessage("conv-d1-p1", req, authP1);

		assertNotNull(saved);
		assertEquals("PATIENT", saved.getSenderRole(), "Sender role must match authenticated user authority, not spoofed role");
	}

	// 9. Invalid appointmentId from another Doctor/Patient pair cannot be attached to a message.
	@Test
	void testSecurity9_InvalidAppointmentIdFromAnotherDoctorPatientPairCannotBeAttached() {
		ConversationRepository mockConvRepo = mock(ConversationRepository.class);
		MessageRepository mockMsgRepo = mock(MessageRepository.class);
		ChatService service = createTestChatService(mockConvRepo, mockMsgRepo, null);

		Doctor d1 = new Doctor();
		d1.setId("D1");
		d1.setEmail("doctor1@example.com");
		when(doctorRepository.findByEmail("doctor1@example.com")).thenReturn(Optional.of(d1));

		Conversation c = new Conversation();
		c.setId("conv-d1-p1");
		c.setDoctorId("D1");
		c.setPatientId("P1");
		when(mockConvRepo.findById("conv-d1-p1")).thenReturn(Optional.of(c));

		// Appointment belonging to completely different Doctor D_OTHER and Patient P_OTHER
		Appointment foreignAppt = new Appointment();
		foreignAppt.setId("appt-foreign");
		foreignAppt.setDoctorId("D_OTHER");
		foreignAppt.setPatientId("P_OTHER");
		when(appointmentRepository.findById("appt-foreign")).thenReturn(Optional.of(foreignAppt));

		SendMessageRequest req = new SendMessageRequest();
		req.setContent("Hello");
		req.setAppointmentId("appt-foreign");

		org.springframework.security.core.Authentication authD1 = createAuth("doctor1@example.com", "DOCTOR");
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			service.sendMessage("conv-d1-p1", req, authD1);
		});

		assertTrue(ex.getMessage().contains("Invalid appointmentId"));
		verify(mockMsgRepo, never()).save(any(Message.class));
	}

	// --------------------------------------------------------
	// REQUIRED FUNCTIONAL TESTS (10 - 22)
	// --------------------------------------------------------

	// 10. D1/P1/A1 creates/uses one pair conversation.
	@Test
	void testFunctional10_D1P1A1_CreatesOrUsesOnePairConversation() {
		ConversationRepository mockConvRepo = mock(ConversationRepository.class);
		MessageRepository mockMsgRepo = mock(MessageRepository.class);
		ChatService service = createTestChatService(mockConvRepo, mockMsgRepo, null);

		when(mockConvRepo.findByPatientIdAndDoctorId("P1", "D1")).thenReturn(java.util.Collections.emptyList());
		when(mockConvRepo.save(any(Conversation.class))).thenAnswer(inv -> {
			Conversation c = inv.getArgument(0);
			if (c.getId() == null) c.setId("c1");
			return c;
		});

		Conversation c1 = service.createConversation("A1", "P1", "D1");
		assertNotNull(c1);
		assertEquals("c1", c1.getId());
		assertEquals("A1", c1.getAppointmentId());
		assertEquals("D1", c1.getDoctorId());
		assertEquals("P1", c1.getPatientId());
		verify(mockConvRepo, times(1)).save(any(Conversation.class));
	}

	// 11. D1/P1/A2 does NOT create a second visible thread.
	@Test
	void testFunctional11_D1P1A2_DoesNotCreateSecondVisibleThread() {
		ConversationRepository mockConvRepo = mock(ConversationRepository.class);
		MessageRepository mockMsgRepo = mock(MessageRepository.class);
		ChatService service = createTestChatService(mockConvRepo, mockMsgRepo, null);

		Conversation c1 = new Conversation();
		c1.setId("c1");
		c1.setDoctorId("D1");
		c1.setPatientId("P1");
		c1.setAppointmentId("A1");
		c1.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));

		when(mockConvRepo.findByPatientIdAndDoctorId("P1", "D1")).thenReturn(java.util.List.of(c1));

		Conversation c2 = service.createConversation("A2", "P1", "D1");
		assertEquals("c1", c2.getId(), "Must reuse existing canonical conversation");

		// In doctor conversation list, P1 appears only once
		when(mockConvRepo.findByDoctorId("D1")).thenReturn(java.util.List.of(c1));
		when(patientRepository.findById("P1")).thenReturn(Optional.of(new com.example.demo.model.Patient()));

		java.util.List<ConversationResponseDTO> dtos = service.getDoctorConversations("D1");
		assertEquals(1, dtos.size(), "Only one visible thread must exist for D1 + P1");
	}

	// 12. D1/P1/A3 does NOT create a third visible thread.
	@Test
	void testFunctional12_D1P1A3_DoesNotCreateThirdVisibleThread() {
		ConversationRepository mockConvRepo = mock(ConversationRepository.class);
		MessageRepository mockMsgRepo = mock(MessageRepository.class);
		ChatService service = createTestChatService(mockConvRepo, mockMsgRepo, null);

		Conversation c1 = new Conversation();
		c1.setId("c1");
		c1.setDoctorId("D1");
		c1.setPatientId("P1");
		c1.setAppointmentId("A1");
		c1.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));

		when(mockConvRepo.findByPatientIdAndDoctorId("P1", "D1")).thenReturn(java.util.List.of(c1));

		Conversation c3 = service.createConversation("A3", "P1", "D1");
		assertEquals("c1", c3.getId(), "Must reuse existing canonical conversation");

		when(mockConvRepo.findByDoctorId("D1")).thenReturn(java.util.List.of(c1));
		when(patientRepository.findById("P1")).thenReturn(Optional.of(new com.example.demo.model.Patient()));

		java.util.List<ConversationResponseDTO> dtos = service.getDoctorConversations("D1");
		assertEquals(1, dtos.size(), "Only one visible thread must exist for D1 + P1");
	}

	// 13. Booking A2 does NOT overwrite A1 historical conversation context.
	@Test
	void testFunctional13_BookingA2_DoesNotOverwriteA1HistoricalConversationContext() {
		ConversationRepository mockConvRepo = mock(ConversationRepository.class);
		MessageRepository mockMsgRepo = mock(MessageRepository.class);
		ChatService service = createTestChatService(mockConvRepo, mockMsgRepo, null);

		Conversation c1 = new Conversation();
		c1.setId("c1");
		c1.setDoctorId("D1");
		c1.setPatientId("P1");
		c1.setAppointmentId("A1"); // Historical appointment A1
		c1.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));

		when(mockConvRepo.findByPatientIdAndDoctorId("P1", "D1")).thenReturn(java.util.List.of(c1));

		// Patient books A2
		Conversation result = service.createConversation("A2", "P1", "D1");

		assertNotNull(result);
		assertEquals("c1", result.getId());
		assertEquals("A1", c1.getAppointmentId(), "Historical Conversation.appointmentId must NOT be overwritten to A2");
		verify(mockConvRepo, never()).save(c1);
	}

	// 14. Old A1 message resolves to appointment A1.
	@Test
	void testFunctional14_OldA1Message_ResolvesToAppointmentA1() {
		ConversationRepository mockConvRepo = mock(ConversationRepository.class);
		MessageRepository mockMsgRepo = mock(MessageRepository.class);
		ChatService service = createTestChatService(mockConvRepo, mockMsgRepo, null);

		Doctor d1 = new Doctor();
		d1.setId("D1");
		d1.setEmail("doctor1@example.com");
		when(doctorRepository.findByEmail("doctor1@example.com")).thenReturn(Optional.of(d1));

		Conversation c1 = new Conversation();
		c1.setId("c1");
		c1.setDoctorId("D1");
		c1.setPatientId("P1");
		c1.setAppointmentId("A1");

		Message m1 = new Message();
		m1.setId("m1");
		m1.setConversationId("c1");
		m1.setAppointmentId(null); // Old historical message where message-level appointmentId was null

		when(mockConvRepo.findById("c1")).thenReturn(Optional.of(c1));
		when(mockConvRepo.findByPatientIdAndDoctorId("P1", "D1")).thenReturn(java.util.List.of(c1));
		when(mockMsgRepo.findByConversationIdOrderBySentAtAsc("c1")).thenReturn(java.util.List.of(m1));

		org.springframework.security.core.Authentication auth = createAuth("doctor1@example.com", "DOCTOR");
		java.util.List<Message> messages = service.getMessages("c1", auth);

		assertEquals(1, messages.size());
		assertEquals("A1", messages.get(0).getAppointmentId(), "Old message must resolve to historical conversation appointment A1");
		verify(mockMsgRepo, never()).save(m1); // Must not persist derived value during read
	}

	// 15. A2 message resolves to appointment A2.
	@Test
	void testFunctional15_A2Message_ResolvesToAppointmentA2() {
		ConversationRepository mockConvRepo = mock(ConversationRepository.class);
		MessageRepository mockMsgRepo = mock(MessageRepository.class);
		ChatService service = createTestChatService(mockConvRepo, mockMsgRepo, null);

		Doctor d1 = new Doctor();
		d1.setId("D1");
		d1.setEmail("doctor1@example.com");
		when(doctorRepository.findByEmail("doctor1@example.com")).thenReturn(Optional.of(d1));

		Conversation c1 = new Conversation();
		c1.setId("c1");
		c1.setDoctorId("D1");
		c1.setPatientId("P1");
		c1.setAppointmentId("A1");

		Message m2 = new Message();
		m2.setId("m2");
		m2.setConversationId("c1");
		m2.setAppointmentId("A2"); // Message sent with A2 context

		when(mockConvRepo.findById("c1")).thenReturn(Optional.of(c1));
		when(mockConvRepo.findByPatientIdAndDoctorId("P1", "D1")).thenReturn(java.util.List.of(c1));
		when(mockMsgRepo.findByConversationIdOrderBySentAtAsc("c1")).thenReturn(java.util.List.of(m2));

		org.springframework.security.core.Authentication auth = createAuth("doctor1@example.com", "DOCTOR");
		java.util.List<Message> messages = service.getMessages("c1", auth);

		assertEquals(1, messages.size());
		assertEquals("A2", messages.get(0).getAppointmentId());
	}

	// 16. A3 message resolves to appointment A3.
	@Test
	void testFunctional16_A3Message_ResolvesToAppointmentA3() {
		ConversationRepository mockConvRepo = mock(ConversationRepository.class);
		MessageRepository mockMsgRepo = mock(MessageRepository.class);
		ChatService service = createTestChatService(mockConvRepo, mockMsgRepo, null);

		Doctor d1 = new Doctor();
		d1.setId("D1");
		d1.setEmail("doctor1@example.com");
		when(doctorRepository.findByEmail("doctor1@example.com")).thenReturn(Optional.of(d1));

		Conversation c1 = new Conversation();
		c1.setId("c1");
		c1.setDoctorId("D1");
		c1.setPatientId("P1");
		c1.setAppointmentId("A1");

		Message m3 = new Message();
		m3.setId("m3");
		m3.setConversationId("c1");
		m3.setAppointmentId("A3");

		when(mockConvRepo.findById("c1")).thenReturn(Optional.of(c1));
		when(mockConvRepo.findByPatientIdAndDoctorId("P1", "D1")).thenReturn(java.util.List.of(c1));
		when(mockMsgRepo.findByConversationIdOrderBySentAtAsc("c1")).thenReturn(java.util.List.of(m3));

		org.springframework.security.core.Authentication auth = createAuth("doctor1@example.com", "DOCTOR");
		java.util.List<Message> messages = service.getMessages("c1", auth);

		assertEquals(1, messages.size());
		assertEquals("A3", messages.get(0).getAppointmentId());
	}

	// 17. Historical C1/C2/C3 messages aggregate exactly once.
	@Test
	void testFunctional17_HistoricalC1C2C3Messages_AggregateExactlyOnce() {
		ConversationRepository mockConvRepo = mock(ConversationRepository.class);
		MessageRepository mockMsgRepo = mock(MessageRepository.class);
		ChatService service = createTestChatService(mockConvRepo, mockMsgRepo, null);

		Doctor d1 = new Doctor();
		d1.setId("D1");
		d1.setEmail("doctor1@example.com");
		when(doctorRepository.findByEmail("doctor1@example.com")).thenReturn(Optional.of(d1));

		Conversation c1 = new Conversation();
		c1.setId("c1"); c1.setDoctorId("D1"); c1.setPatientId("P1"); c1.setAppointmentId("A1");
		Conversation c2 = new Conversation();
		c2.setId("c2"); c2.setDoctorId("D1"); c2.setPatientId("P1"); c2.setAppointmentId("A2");
		Conversation c3 = new Conversation();
		c3.setId("c3"); c3.setDoctorId("D1"); c3.setPatientId("P1"); c3.setAppointmentId("A3");

		Message m1 = new Message(); m1.setId("m1"); m1.setConversationId("c1");
		Message m2 = new Message(); m2.setId("m2"); m2.setConversationId("c2");
		Message m3 = new Message(); m3.setId("m3"); m3.setConversationId("c3");

		when(mockConvRepo.findById("c1")).thenReturn(Optional.of(c1));
		when(mockConvRepo.findByPatientIdAndDoctorId("P1", "D1")).thenReturn(java.util.List.of(c1, c2, c3));
		when(mockMsgRepo.findByConversationIdInOrderBySentAtAsc(java.util.List.of("c1", "c2", "c3")))
				.thenReturn(java.util.List.of(m1, m2, m3));

		org.springframework.security.core.Authentication auth = createAuth("doctor1@example.com", "DOCTOR");
		java.util.List<Message> messages = service.getMessages("c1", auth);

		assertEquals(3, messages.size());
		long distinctCount = messages.stream().map(Message::getId).distinct().count();
		assertEquals(3, distinctCount, "Historical messages must aggregate exactly once without duplicates");
	}

	// 18. Aggregated messages are chronological.
	@Test
	void testFunctional18_AggregatedMessages_AreChronological() {
		ConversationRepository mockConvRepo = mock(ConversationRepository.class);
		MessageRepository mockMsgRepo = mock(MessageRepository.class);
		ChatService service = createTestChatService(mockConvRepo, mockMsgRepo, null);

		Doctor d1 = new Doctor();
		d1.setId("D1");
		d1.setEmail("doctor1@example.com");
		when(doctorRepository.findByEmail("doctor1@example.com")).thenReturn(Optional.of(d1));

		Conversation c1 = new Conversation();
		c1.setId("c1"); c1.setDoctorId("D1"); c1.setPatientId("P1");

		Message m1 = new Message(); m1.setId("m1"); m1.setSentAt(LocalDateTime.of(2026, 1, 1, 10, 0));
		Message m2 = new Message(); m2.setId("m2"); m2.setSentAt(LocalDateTime.of(2026, 1, 1, 10, 30));
		Message m3 = new Message(); m3.setId("m3"); m3.setSentAt(LocalDateTime.of(2026, 1, 1, 11, 0));

		when(mockConvRepo.findById("c1")).thenReturn(Optional.of(c1));
		when(mockConvRepo.findByPatientIdAndDoctorId("P1", "D1")).thenReturn(java.util.List.of(c1));
		when(mockMsgRepo.findByConversationIdOrderBySentAtAsc("c1")).thenReturn(java.util.List.of(m1, m2, m3));

		org.springframework.security.core.Authentication auth = createAuth("doctor1@example.com", "DOCTOR");
		java.util.List<Message> messages = service.getMessages("c1", auth);

		assertEquals(3, messages.size());
		assertTrue(messages.get(0).getSentAt().isBefore(messages.get(1).getSentAt()));
		assertTrue(messages.get(1).getSentAt().isBefore(messages.get(2).getSentAt()));
	}

	// 19. Different patient creates separate conversation.
	@Test
	void testFunctional19_DifferentPatient_CreatesSeparateConversation() {
		ConversationRepository mockConvRepo = mock(ConversationRepository.class);
		MessageRepository mockMsgRepo = mock(MessageRepository.class);
		ChatService service = createTestChatService(mockConvRepo, mockMsgRepo, null);

		Conversation convP1 = new Conversation();
		convP1.setId("conv-p1"); convP1.setDoctorId("D1"); convP1.setPatientId("P1");
		Conversation convP2 = new Conversation();
		convP2.setId("conv-p2"); convP2.setDoctorId("D1"); convP2.setPatientId("P2");

		when(mockConvRepo.findByDoctorId("D1")).thenReturn(java.util.List.of(convP1, convP2));
		when(patientRepository.findById("P1")).thenReturn(Optional.of(new com.example.demo.model.Patient()));
		when(patientRepository.findById("P2")).thenReturn(Optional.of(new com.example.demo.model.Patient()));

		java.util.List<ConversationResponseDTO> dtos = service.getDoctorConversations("D1");
		assertEquals(2, dtos.size(), "Different patients must produce separate conversations");
	}

	// 20. Different doctor creates separate conversation.
	@Test
	void testFunctional20_DifferentDoctor_CreatesSeparateConversation() {
		ConversationRepository mockConvRepo = mock(ConversationRepository.class);
		MessageRepository mockMsgRepo = mock(MessageRepository.class);
		ChatService service = createTestChatService(mockConvRepo, mockMsgRepo, null);

		Conversation convD1 = new Conversation();
		convD1.setId("conv-d1"); convD1.setDoctorId("D1"); convD1.setPatientId("P1");
		Conversation convD2 = new Conversation();
		convD2.setId("conv-d2"); convD2.setDoctorId("D2"); convD2.setPatientId("P1");

		when(mockConvRepo.findByPatientId("P1")).thenReturn(java.util.List.of(convD1, convD2));
		when(doctorRepository.findById("D1")).thenReturn(Optional.of(new Doctor()));
		when(doctorRepository.findById("D2")).thenReturn(Optional.of(new Doctor()));

		java.util.List<ConversationResponseDTO> dtos = service.getPatientConversations("P1");
		assertEquals(2, dtos.size(), "Different doctors must produce separate conversations");
	}

	// 21. Canonical selection remains identical even when repository input order is reversed.
	@Test
	void testFunctional21_CanonicalSelection_RemainsIdenticalWhenRepositoryInputOrderReversed() {
		ChatService service = new ChatService();

		Conversation c1 = new Conversation();
		c1.setId("c1");
		c1.setCreatedAt(LocalDateTime.of(2026, 1, 1, 10, 0));

		Conversation c2 = new Conversation();
		c2.setId("c2");
		c2.setCreatedAt(LocalDateTime.of(2026, 2, 1, 10, 0));

		Conversation canonicalForward = service.getCanonical(java.util.List.of(c1, c2));
		Conversation canonicalReversed = service.getCanonical(java.util.List.of(c2, c1));

		assertNotNull(canonicalForward);
		assertNotNull(canonicalReversed);
		assertEquals("c1", canonicalForward.getId());
		assertEquals("c1", canonicalReversed.getId(), "Canonical selection must be identical regardless of input encounter order");
	}

	// 22. Canonical selection remains deterministic when createdAt values are identical/null.
	@Test
	void testFunctional22_CanonicalSelection_RemainsDeterministicWhenCreatedAtIdenticalOrNull() {
		ChatService service = new ChatService();

		// Case A: Identical createdAt -> ID tie-breaker
		LocalDateTime sameTime = LocalDateTime.of(2026, 3, 1, 12, 0);
		Conversation cAlpha = new Conversation(); cAlpha.setId("conv-alpha"); cAlpha.setCreatedAt(sameTime);
		Conversation cBeta = new Conversation(); cBeta.setId("conv-beta"); cBeta.setCreatedAt(sameTime);

		assertEquals("conv-alpha", service.getCanonical(java.util.List.of(cBeta, cAlpha)).getId());
		assertEquals("conv-alpha", service.getCanonical(java.util.List.of(cAlpha, cBeta)).getId());

		// Case B: Null createdAt on both -> ID tie-breaker
		Conversation cNullA = new Conversation(); cNullA.setId("conv-10"); cNullA.setCreatedAt(null);
		Conversation cNullB = new Conversation(); cNullB.setId("conv-20"); cNullB.setCreatedAt(null);

		assertEquals("conv-10", service.getCanonical(java.util.List.of(cNullB, cNullA)).getId());
		assertEquals("conv-10", service.getCanonical(java.util.List.of(cNullA, cNullB)).getId());

		// Case C: Null ID handled safely without NPE
		Conversation cNullId = new Conversation(); cNullId.setId(null); cNullId.setCreatedAt(sameTime);
		Conversation cValidId = new Conversation(); cValidId.setId("conv-z"); cValidId.setCreatedAt(sameTime);

		Conversation canonical = service.getCanonical(java.util.List.of(cNullId, cValidId));
		assertNotNull(canonical);
	}

	// --------------------------------------------------------
	// STEP 9 VERIFICATION TESTS: EXISTING DUPLICATE DATA (CASES 1-6)
	// --------------------------------------------------------

	// Case 1: D1 + P1 with existing C1/A1, C2/A2, C3/A3 in MongoDB -> Exactly 1 visible conversation
	@Test
	void testStep9_Case1_ExistingDuplicates_ReturnsExactlyOneVisibleConversation() {
		ConversationRepository mockConvRepo = mock(ConversationRepository.class);
		MessageRepository mockMsgRepo = mock(MessageRepository.class);
		ChatService service = createTestChatService(mockConvRepo, mockMsgRepo, null);

		Conversation c1 = new Conversation(); c1.setId("c1"); c1.setDoctorId("D1"); c1.setPatientId("P1"); c1.setAppointmentId("A1");
		c1.setCreatedAt(LocalDateTime.of(2026, 1, 1, 9, 0));
		Conversation c2 = new Conversation(); c2.setId("c2"); c2.setDoctorId("D1"); c2.setPatientId("P1"); c2.setAppointmentId("A2");
		c2.setCreatedAt(LocalDateTime.of(2026, 1, 2, 9, 0));
		Conversation c3 = new Conversation(); c3.setId("c3"); c3.setDoctorId("D1"); c3.setPatientId("P1"); c3.setAppointmentId("A3");
		c3.setCreatedAt(LocalDateTime.of(2026, 1, 3, 9, 0));

		when(mockConvRepo.findByDoctorId("D1")).thenReturn(java.util.List.of(c1, c2, c3));
		when(patientRepository.findById("P1")).thenReturn(Optional.of(new com.example.demo.model.Patient()));

		java.util.List<ConversationResponseDTO> result = service.getDoctorConversations("D1");

		assertNotNull(result);
		assertEquals(1, result.size(), "Doctor Messages list must return EXACTLY ONE visible conversation for D1 + P1");
		assertEquals("c1", result.get(0).getId(), "Canonical ID must be preserved");
	}

	// Case 2: Open that conversation -> messages from C1 + C2 + C3 appear exactly once and chronologically
	@Test
	void testStep9_Case2_OpenConversation_MessagesFromC1C2C3AppearExactlyOnceAndChronologically() {
		ConversationRepository mockConvRepo = mock(ConversationRepository.class);
		MessageRepository mockMsgRepo = mock(MessageRepository.class);
		ChatService service = createTestChatService(mockConvRepo, mockMsgRepo, null);

		Doctor d1 = new Doctor(); d1.setId("D1"); d1.setEmail("doc1@example.com");
		when(doctorRepository.findByEmail("doc1@example.com")).thenReturn(Optional.of(d1));

		Conversation c1 = new Conversation(); c1.setId("c1"); c1.setDoctorId("D1"); c1.setPatientId("P1"); c1.setAppointmentId("A1");
		c1.setCreatedAt(LocalDateTime.of(2026, 1, 1, 9, 0));
		Conversation c2 = new Conversation(); c2.setId("c2"); c2.setDoctorId("D1"); c2.setPatientId("P1"); c2.setAppointmentId("A2");
		c2.setCreatedAt(LocalDateTime.of(2026, 1, 2, 9, 0));
		Conversation c3 = new Conversation(); c3.setId("c3"); c3.setDoctorId("D1"); c3.setPatientId("P1"); c3.setAppointmentId("A3");
		c3.setCreatedAt(LocalDateTime.of(2026, 1, 3, 9, 0));

		when(mockConvRepo.findById("c1")).thenReturn(Optional.of(c1));
		when(mockConvRepo.findByPatientIdAndDoctorId("P1", "D1")).thenReturn(java.util.List.of(c1, c2, c3));

		Message m1 = new Message(); m1.setId("m1"); m1.setConversationId("c1"); m1.setContent("Msg from A1");
		m1.setSentAt(LocalDateTime.of(2026, 1, 1, 10, 0));
		Message m2 = new Message(); m2.setId("m2"); m2.setConversationId("c2"); m2.setContent("Msg from A2");
		m2.setSentAt(LocalDateTime.of(2026, 1, 2, 10, 0));
		Message m3 = new Message(); m3.setId("m3"); m3.setConversationId("c3"); m3.setContent("Msg from A3");
		m3.setSentAt(LocalDateTime.of(2026, 1, 3, 10, 0));

		when(mockMsgRepo.findByConversationIdInOrderBySentAtAsc(java.util.List.of("c1", "c2", "c3")))
				.thenReturn(java.util.List.of(m1, m2, m3));

		Authentication auth = new UsernamePasswordAuthenticationToken(
				"doc1@example.com", "pass", java.util.List.of(new SimpleGrantedAuthority("ROLE_DOCTOR")));

		java.util.List<Message> messages = service.getMessages("c1", auth);

		assertEquals(3, messages.size());
		assertEquals("m1", messages.get(0).getId());
		assertEquals("A1", messages.get(0).getAppointmentId());
		assertEquals("m2", messages.get(1).getId());
		assertEquals("A2", messages.get(1).getAppointmentId());
		assertEquals("m3", messages.get(2).getId());
		assertEquals("A3", messages.get(2).getAppointmentId());
	}

	// Case 3: New appointment A4 for D1 + P1 does NOT create a second visible thread
	@Test
	void testStep9_Case3_NewAppointmentA4_DoesNotCreateSecondThread() {
		ConversationRepository mockConvRepo = mock(ConversationRepository.class);
		MessageRepository mockMsgRepo = mock(MessageRepository.class);
		ChatService service = createTestChatService(mockConvRepo, mockMsgRepo, null);

		Conversation c1 = new Conversation(); c1.setId("c1"); c1.setDoctorId("D1"); c1.setPatientId("P1"); c1.setAppointmentId("A1");
		c1.setCreatedAt(LocalDateTime.of(2026, 1, 1, 9, 0));

		when(mockConvRepo.findByPatientIdAndDoctorId("P1", "D1")).thenReturn(java.util.List.of(c1));

		Conversation created = service.createConversation("A4", "P1", "D1");
		assertEquals("c1", created.getId(), "Must reuse existing canonical conversation");

		when(mockConvRepo.findByDoctorId("D1")).thenReturn(java.util.List.of(c1));
		when(patientRepository.findById("P1")).thenReturn(Optional.of(new com.example.demo.model.Patient()));

		java.util.List<ConversationResponseDTO> list = service.getDoctorConversations("D1");
		assertEquals(1, list.size(), "Still exactly 1 visible conversation thread for D1 + P1");
	}

	// Case 4: D1 + P2 results in separate second conversation
	@Test
	void testStep9_Case4_D1P2_ResultsInSeparateSecondConversation() {
		ConversationRepository mockConvRepo = mock(ConversationRepository.class);
		MessageRepository mockMsgRepo = mock(MessageRepository.class);
		ChatService service = createTestChatService(mockConvRepo, mockMsgRepo, null);

		Conversation c1 = new Conversation(); c1.setId("c1"); c1.setDoctorId("D1"); c1.setPatientId("P1"); c1.setAppointmentId("A1");
		Conversation c2 = new Conversation(); c2.setId("c2"); c2.setDoctorId("D1"); c2.setPatientId("P2"); c2.setAppointmentId("A2");

		when(mockConvRepo.findByDoctorId("D1")).thenReturn(java.util.List.of(c1, c2));
		when(patientRepository.findById("P1")).thenReturn(Optional.of(new com.example.demo.model.Patient()));
		when(patientRepository.findById("P2")).thenReturn(Optional.of(new com.example.demo.model.Patient()));

		java.util.List<ConversationResponseDTO> list = service.getDoctorConversations("D1");
		assertEquals(2, list.size(), "D1 + P1 and D1 + P2 must be separate threads");
	}

	// Case 5: D2 + P1 results in separate conversation for D2
	@Test
	void testStep9_Case5_D2P1_ResultsInSeparateConversationForD2() {
		ConversationRepository mockConvRepo = mock(ConversationRepository.class);
		MessageRepository mockMsgRepo = mock(MessageRepository.class);
		ChatService service = createTestChatService(mockConvRepo, mockMsgRepo, null);

		Conversation cD1 = new Conversation(); cD1.setId("c-d1"); cD1.setDoctorId("D1"); cD1.setPatientId("P1");
		Conversation cD2 = new Conversation(); cD2.setId("c-d2"); cD2.setDoctorId("D2"); cD2.setPatientId("P1");

		when(mockConvRepo.findByDoctorId("D1")).thenReturn(java.util.List.of(cD1));
		when(mockConvRepo.findByDoctorId("D2")).thenReturn(java.util.List.of(cD2));
		when(patientRepository.findById("P1")).thenReturn(Optional.of(new com.example.demo.model.Patient()));

		java.util.List<ConversationResponseDTO> listD1 = service.getDoctorConversations("D1");
		java.util.List<ConversationResponseDTO> listD2 = service.getDoctorConversations("D2");

		assertEquals(1, listD1.size());
		assertEquals("c-d1", listD1.get(0).getId());
		assertEquals(1, listD2.size());
		assertEquals("c-d2", listD2.get(0).getId());
		assertNotEquals(listD1.get(0).getId(), listD2.get(0).getId(), "D1 and D2 must have separate conversations with P1");
	}

	// Case 6: Historical conversations where patientId is null in MongoDB are resolved via appointment and grouped
	@Test
	void testStep9_Case6_HistoricalConversationsWithNullPatientId_ResolvedAndGrouped() {
		ConversationRepository mockConvRepo = mock(ConversationRepository.class);
		MessageRepository mockMsgRepo = mock(MessageRepository.class);
		ChatService service = createTestChatService(mockConvRepo, mockMsgRepo, null);

		// Old legacy documents with null patientId
		Conversation cOld1 = new Conversation(); cOld1.setId("c-old-1"); cOld1.setDoctorId("D1"); cOld1.setPatientId(null); cOld1.setAppointmentId("A1");
		Conversation cOld2 = new Conversation(); cOld2.setId("c-old-2"); cOld2.setDoctorId("D1"); cOld2.setPatientId(null); cOld2.setAppointmentId("A2");

		Appointment appt1 = new Appointment(); appt1.setId("A1"); appt1.setPatientId("P1"); appt1.setDoctorId("D1");
		Appointment appt2 = new Appointment(); appt2.setId("A2"); appt2.setPatientId("P1"); appt2.setDoctorId("D1");

		when(mockConvRepo.findByDoctorId("D1")).thenReturn(java.util.List.of(cOld1, cOld2));
		when(appointmentRepository.findById("A1")).thenReturn(Optional.of(appt1));
		when(appointmentRepository.findById("A2")).thenReturn(Optional.of(appt2));
		when(patientRepository.findById("P1")).thenReturn(Optional.of(new com.example.demo.model.Patient()));

		java.util.List<ConversationResponseDTO> dtos = service.getDoctorConversations("D1");
		assertEquals(1, dtos.size(), "Legacy conversations with null patientId must be resolved via appointment and grouped into 1");
		assertEquals("P1", dtos.get(0).getPatientId());
	}

	// =========================================================================
	// PHYSICAL APPOINTMENT BOOKING & NOTIFICATION SCENARIOS
	// =========================================================================

	// Scenario 1: Initial booking does not send any notifications (Patient, Doctor, Hospital)
	@Test
	void testScenario1_BookingInitiation_SendsNoNotifications() {
		Appointment appt = new Appointment();
		appt.setPatientId("patient-101");
		appt.setDoctorId("doctor-202");
		appt.setScheduleId("schedule-303");
		appt.setHospitalId("hospital-404");
		appt.setAppointmentNumber("5");
		appt.setDate("2026-11-15");
		appt.setTime("09:00 - 10:00");

		when(appointmentRepository.existsByPatientIdAndDoctorIdAndScheduleId("patient-101", "doctor-202", "schedule-303")).thenReturn(false);
		when(appointmentRepository.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

		Appointment booked = appointmentService.bookAppointment(appt);
		assertNotNull(booked);
		assertEquals("PENDING", booked.getStatus());
		assertEquals("PENDING", booked.getPaymentStatus());
		assertFalse(booked.isPaid());
		verify(notificationRepository, never()).save(any(Notification.class));
	}

	// Scenario 2: Patient Portal My Appointments excludes pending, unpaid, and failed appointments
	@Test
	void testScenario2_PatientAppointments_ExcludesPendingUnpaidAndFailed() {
		Appointment pendingAppt = new Appointment();
		pendingAppt.setId("a1");
		pendingAppt.setPatientId("patient-101");
		pendingAppt.setStatus("PENDING");
		pendingAppt.setPaymentStatus("PENDING");
		pendingAppt.setPaid(false);

		Appointment failedAppt = new Appointment();
		failedAppt.setId("a2");
		failedAppt.setPatientId("patient-101");
		failedAppt.setStatus("CANCELLED");
		failedAppt.setPaymentStatus("FAILED");
		failedAppt.setPaid(false);

		when(appointmentRepository.findByPatientId("patient-101")).thenReturn(java.util.List.of(pendingAppt, failedAppt));

		java.util.List<Appointment> result = appointmentService.getAppointmentsForPatient("patient-101");
		assertNotNull(result);
		assertTrue(result.isEmpty(), "Pending and failed unpaid appointments must not appear in Patient Portal My Appointments");
	}

	// Scenario 3: Patient Portal My Appointments includes confirmed and paid appointments
	@Test
	void testScenario3_PatientAppointments_IncludesConfirmedAndPaid() {
		Appointment confirmedAppt = new Appointment();
		confirmedAppt.setId("a1");
		confirmedAppt.setPatientId("patient-101");
		confirmedAppt.setStatus("CONFIRMED");
		confirmedAppt.setPaymentStatus("PAID");
		confirmedAppt.setPaid(true);

		Appointment pendingAppt = new Appointment();
		pendingAppt.setId("a2");
		pendingAppt.setPatientId("patient-101");
		pendingAppt.setStatus("PENDING");
		pendingAppt.setPaymentStatus("PENDING");
		pendingAppt.setPaid(false);

		when(appointmentRepository.findByPatientId("patient-101")).thenReturn(java.util.List.of(confirmedAppt, pendingAppt));

		java.util.List<Appointment> result = appointmentService.getAppointmentsForPatient("patient-101");
		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals("a1", result.get(0).getId());
		assertEquals("CONFIRMED", result.get(0).getStatus());
		assertTrue(result.get(0).isPaid());
	}

	// Scenario 4: Cancelled or failed appointment cannot be confirmed by unverified frontend callback
	@Test
	void testScenario4_CancelledOrFailedAppointment_CannotBeConfirmedByFrontendCallback() {
		Appointment cancelledAppt = new Appointment();
		cancelledAppt.setId("appt-cancelled");
		cancelledAppt.setStatus("CANCELLED");
		cancelledAppt.setPaymentStatus("FAILED");
		cancelledAppt.setPaid(false);

		AppointmentRepository mockApptRepo = mock(AppointmentRepository.class);
		when(mockApptRepo.findById("appt-cancelled")).thenReturn(Optional.of(cancelledAppt));

		PaymentService paymentService = new PaymentService();
		org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "appointmentRepo", mockApptRepo);

		assertThrows(IllegalStateException.class, () -> {
			paymentService.confirmPaymentSuccess("appt-cancelled", "PAYHERE-999", 1000.00);
		});
	}

	// Scenario 5: PayHere IPN cancellation updates appointment to CANCELLED and FAILED
	@Test
	void testScenario5_PayHereIpn_CancellationUpdatesStatusToCancelledAndFailed() {
		Appointment pendingAppt = new Appointment();
		pendingAppt.setId("order-cancel");
		pendingAppt.setStatus("PENDING");
		pendingAppt.setPaymentStatus("PENDING");
		pendingAppt.setPaid(false);

		AppointmentRepository mockApptRepo = mock(AppointmentRepository.class);
		NotificationRepository mockNotifRepo = mock(NotificationRepository.class);
		when(mockApptRepo.findById("order-cancel")).thenReturn(Optional.of(pendingAppt));
		when(mockApptRepo.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));

		PaymentService paymentService = new PaymentService();
		org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "appointmentRepo", mockApptRepo);
		org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "notificationRepo", mockNotifRepo);

		paymentService.cancelPendingPayment("order-cancel");

		assertEquals("CANCELLED", pendingAppt.getStatus());
		assertEquals("FAILED", pendingAppt.getPaymentStatus());
		assertFalse(pendingAppt.isPaid());
		verify(mockNotifRepo, never()).save(any(Notification.class));
	}

	// Scenario 6: Repeated payment-success callback is idempotent and does not create duplicate notifications
	@Test
	void testScenario6_RepeatedPaymentSuccessCallback_IsIdempotentAndDoesNotDuplicateNotifications() {
		Appointment appt = new Appointment();
		appt.setId("appt-idem");
		appt.setPatientId("patientA");
		appt.setDoctorId("doctorX");
		appt.setHospitalId("hospital-001");
		appt.setScheduleId("schedule1");
		appt.setAppointmentNumber("APT-001");
		appt.setStatus("PENDING");
		appt.setPaymentStatus("PENDING");
		appt.setPaid(false);
		appt.setAmount(1000.00);

		Doctor doc = new Doctor();
		doc.setTitle("Dr.");
		doc.setFirstName("John");
		doc.setLastName("Doe");

		AppointmentRepository mockApptRepo = mock(AppointmentRepository.class);
		NotificationRepository mockNotifRepo = mock(NotificationRepository.class);
		DoctorRepository mockDocRepo = mock(DoctorRepository.class);
		HospitalRepository mockHospRepo = mock(HospitalRepository.class);
		LabTestRepository mockLabRepo = mock(LabTestRepository.class);
		ScheduleRepository mockScheduleRepo = mock(ScheduleRepository.class);

		when(mockApptRepo.findById("appt-idem")).thenReturn(Optional.of(appt));
		when(mockApptRepo.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));
		when(mockDocRepo.findById("doctorX")).thenReturn(Optional.of(doc));

		PaymentService paymentService = new PaymentService();
		org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "appointmentRepo", mockApptRepo);
		org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "notificationRepo", mockNotifRepo);
		org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "doctorRepo", mockDocRepo);
		org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "hospitalRepo", mockHospRepo);
		org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "labRepo", mockLabRepo);
		org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "scheduleRepo", mockScheduleRepo);

		// First call
		Appointment first = paymentService.confirmPaymentSuccess("appt-idem", "PAY-1", 1000.00);
		assertEquals("CONFIRMED", first.getStatus());
		assertEquals("PAID", first.getPaymentStatus());
		assertTrue(first.isPaid());
		verify(mockNotifRepo, times(3)).save(any(Notification.class));

		// Second repeated call
		Appointment second = paymentService.confirmPaymentSuccess("appt-idem", "PAY-1", 1000.00);
		assertEquals("CONFIRMED", second.getStatus());
		assertEquals("PAID", second.getPaymentStatus());
		assertTrue(second.isPaid());

		// Verify no additional notifications were saved on the second call (still exactly 3 total)
		verify(mockNotifRepo, times(3)).save(any(Notification.class));
	}

	// Scenario 7: Frontend callback and PayHere IPN both reporting success result in exactly one confirmation and one notification set
	@Test
	void testScenario7_FrontendCallbackAndIpnBothSuccess_IsIdempotentAndSendsExactlyOneNotificationSet() {
		Appointment appt = new Appointment();
		appt.setId("order-double");
		appt.setPatientId("patientA");
		appt.setDoctorId("doctorX");
		appt.setHospitalId("hospital-001");
		appt.setScheduleId("schedule1");
		appt.setAppointmentNumber("APT-002");
		appt.setStatus("PENDING");
		appt.setPaymentStatus("PENDING");
		appt.setPaid(false);
		appt.setAmount(1000.00);

		Doctor doc = new Doctor();
		doc.setTitle("Dr.");
		doc.setFirstName("Alice");
		doc.setLastName("Smith");

		AppointmentRepository mockApptRepo = mock(AppointmentRepository.class);
		NotificationRepository mockNotifRepo = mock(NotificationRepository.class);
		DoctorRepository mockDocRepo = mock(DoctorRepository.class);
		ScheduleRepository mockScheduleRepo = mock(ScheduleRepository.class);

		when(mockApptRepo.findById("order-double")).thenReturn(Optional.of(appt));
		when(mockApptRepo.save(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));
		when(mockDocRepo.findById("doctorX")).thenReturn(Optional.of(doc));

		PaymentService paymentService = new PaymentService();
		org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "appointmentRepo", mockApptRepo);
		org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "notificationRepo", mockNotifRepo);
		org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "doctorRepo", mockDocRepo);
		org.springframework.test.util.ReflectionTestUtils.setField(paymentService, "scheduleRepo", mockScheduleRepo);

		// 1. Frontend callback reports success
		Appointment confirmed = paymentService.confirmPaymentSuccess("order-double", "PAY-FE", 1000.00, false);
		assertEquals("CONFIRMED", confirmed.getStatus());
		verify(mockNotifRepo, times(3)).save(any(Notification.class));

		// 2. IPN callback reports success
		Appointment ipnConfirmed = paymentService.confirmPaymentSuccess("order-double", "PAY-IPN", 1000.00, true);
		assertEquals("CONFIRMED", ipnConfirmed.getStatus());

		// Ensure still exactly 3 notifications (one each for Patient, Doctor, Hospital)
		verify(mockNotifRepo, times(3)).save(any(Notification.class));
	}
}
