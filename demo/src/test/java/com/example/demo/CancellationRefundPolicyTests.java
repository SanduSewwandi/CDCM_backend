package com.example.demo;

import com.example.demo.dto.RefundPreviewDTO;
import com.example.demo.model.Appointment;
import com.example.demo.model.CancellationRefundPolicy;
import com.example.demo.model.Doctor;
import com.example.demo.model.Hospital;
import com.example.demo.model.Patient;
import com.example.demo.model.Schedule;
import com.example.demo.model.Notification;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.DoctorRepository;
import com.example.demo.repository.HospitalRepository;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.repository.PatientRepository;
import com.example.demo.repository.ScheduleRepository;
import com.example.demo.service.AppointmentService;
import com.example.demo.service.HospitalService;
import com.example.demo.service.ScheduleService;
import com.example.demo.service.SmsService;
import com.example.demo.service.EmailService;
import com.example.demo.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CancellationRefundPolicyTests {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SmsService smsService;

    @Mock
    private EmailService emailService;

    @Mock
    private ChatService chatService;

    @InjectMocks
    private AppointmentService appointmentService;

    @InjectMocks
    private HospitalService hospitalService;

    @InjectMocks
    private ScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // =========================================================
    // 1. POLICY MODEL & PERCENTAGE VALIDATION TESTS
    // =========================================================

    @Test
    void test1_policyDefaultValues_are90_75_50_0() {
        CancellationRefundPolicy policy = new CancellationRefundPolicy();
        assertEquals(90.0, policy.getOver48HoursPercent());
        assertEquals(75.0, policy.getBetween24And48HoursPercent());
        assertEquals(50.0, policy.getBetween6And24HoursPercent());
        assertEquals(0.0, policy.getUnder6HoursPercent());
        assertTrue(policy.arePercentagesValid());
    }

    @Test
    void test2_policyValidPercentages_passValidation() {
        CancellationRefundPolicy policy = new CancellationRefundPolicy(100.0, 80.0, 40.0, 0.0);
        assertTrue(policy.arePercentagesValid());
    }

    @Test
    void test3_policyNegativeOrAbove100_failsValidation() {
        CancellationRefundPolicy pNegative = new CancellationRefundPolicy(-5.0, 75.0, 50.0, 0.0);
        assertFalse(pNegative.arePercentagesValid());

        CancellationRefundPolicy pOver100 = new CancellationRefundPolicy(90.0, 105.0, 50.0, 0.0);
        assertFalse(pOver100.arePercentagesValid());
    }

    // =========================================================
    // 2. HOSPITAL SERVICE REFUND POLICY CONFIGURATION TESTS
    // =========================================================

    @Test
    void test4_hospitalService_getRefundPolicy_returnsDefaultsWhenNotSet() {
        Hospital hospital = new Hospital();
        hospital.setId("hosp1");
        hospital.setEmail("general@hospital.com");

        when(hospitalRepository.findByEmail("general@hospital.com")).thenReturn(Optional.of(hospital));

        CancellationRefundPolicy policy = hospitalService.getRefundPolicy("general@hospital.com");
        assertNotNull(policy);
        assertEquals(90.0, policy.getOver48HoursPercent());
        assertEquals(75.0, policy.getBetween24And48HoursPercent());
    }

    @Test
    void test5_hospitalService_updateRefundPolicy_persistsValidPolicy() {
        Hospital hospital = new Hospital();
        hospital.setId("hosp1");
        hospital.setEmail("general@hospital.com");

        when(hospitalRepository.findByEmail("general@hospital.com")).thenReturn(Optional.of(hospital));
        when(hospitalRepository.save(any(Hospital.class))).thenAnswer(inv -> inv.getArgument(0));

        CancellationRefundPolicy newPolicy = new CancellationRefundPolicy(85.0, 65.0, 35.0, 10.0);
        CancellationRefundPolicy updated = hospitalService.updateRefundPolicy("general@hospital.com", newPolicy);

        assertEquals(85.0, updated.getOver48HoursPercent());
        assertEquals(65.0, updated.getBetween24And48HoursPercent());
        verify(hospitalRepository, times(1)).save(hospital);
    }

    @Test
    void test6_hospitalService_updateRefundPolicy_rejectsInvalidPolicy() {
        CancellationRefundPolicy invalidPolicy = new CancellationRefundPolicy(120.0, 75.0, 50.0, 0.0);
        assertThrows(IllegalArgumentException.class, () ->
                hospitalService.updateRefundPolicy("general@hospital.com", invalidPolicy)
        );
    }

    // =========================================================
    // 3. DATE/TIME PARSING & RESOLUTION TESTS
    // =========================================================

    @Test
    void test7_parseAppointmentDateTime_parsesStandardFormats() {
        LocalDateTime dt = appointmentService.parseAppointmentDateTime("2026-12-25", "10:30 AM", null);
        assertNotNull(dt);
        assertEquals(2026, dt.getYear());
        assertEquals(12, dt.getMonthValue());
        assertEquals(25, dt.getDayOfMonth());
        assertEquals(10, dt.getHour());
        assertEquals(30, dt.getMinute());
    }

    @Test
    void test8_parseAppointmentDateTime_fallsBackToScheduleWhenMissing() {
        Schedule schedule = new Schedule();
        schedule.setId("sched1");
        schedule.setDate("2026-11-15");
        schedule.setStartTime("04:00 PM");

        when(scheduleRepository.findById("sched1")).thenReturn(Optional.of(schedule));

        LocalDateTime dt = appointmentService.parseAppointmentDateTime(null, null, "sched1");
        assertNotNull(dt);
        assertEquals(2026, dt.getYear());
        assertEquals(11, dt.getMonthValue());
        assertEquals(15, dt.getDayOfMonth());
        assertEquals(16, dt.getHour());
    }

    // =========================================================
    // 4. REFUND PERCENTAGE CALCULATION TIER TESTS
    // =========================================================

    @Test
    void test9_calculateRefund_over48HoursAhead_returnsTier1_90Percent() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = now.plusHours(50);

        Appointment appt = new Appointment();
        appt.setDate(future.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        appt.setTime(future.format(DateTimeFormatter.ofPattern("hh:mm a")));

        double refundPct = appointmentService.calculateRefundPercentage(appt, now);
        assertEquals(90.0, refundPct);
    }

    @Test
    void test10_calculateRefund_between24And48Hours_returnsTier2_75Percent() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = now.plusHours(30);

        Appointment appt = new Appointment();
        appt.setDate(future.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        appt.setTime(future.format(DateTimeFormatter.ofPattern("hh:mm a")));

        double refundPct = appointmentService.calculateRefundPercentage(appt, now);
        assertEquals(75.0, refundPct);
    }

    @Test
    void test11_calculateRefund_between6And24Hours_returnsTier3_50Percent() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = now.plusHours(12);

        Appointment appt = new Appointment();
        appt.setDate(future.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        appt.setTime(future.format(DateTimeFormatter.ofPattern("hh:mm a")));

        double refundPct = appointmentService.calculateRefundPercentage(appt, now);
        assertEquals(50.0, refundPct);
    }

    @Test
    void test12_calculateRefund_under6Hours_returnsTier4_0Percent() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = now.plusHours(2);

        Appointment appt = new Appointment();
        appt.setDate(future.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        appt.setTime(future.format(DateTimeFormatter.ofPattern("hh:mm a")));

        double refundPct = appointmentService.calculateRefundPercentage(appt, now);
        assertEquals(0.0, refundPct);
    }

    @Test
    void test13_calculateRefund_pastAppointment_throwsIllegalArgumentException() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime past = now.minusHours(5);

        Appointment appt = new Appointment();
        appt.setDate(past.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        appt.setTime(past.format(DateTimeFormatter.ofPattern("hh:mm a")));

        assertThrows(IllegalArgumentException.class, () ->
                appointmentService.calculateRefundPercentage(appt, now)
        );
    }

    @Test
    void test14_calculateRefund_customHospitalPolicyApplied() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = now.plusHours(30); // 24-48h tier

        Hospital customHospital = new Hospital();
        customHospital.setId("hospCust");
        customHospital.setCancellationRefundPolicy(new CancellationRefundPolicy(95.0, 80.0, 60.0, 20.0));

        when(hospitalRepository.findById("hospCust")).thenReturn(Optional.of(customHospital));

        Appointment appt = new Appointment();
        appt.setHospitalId("hospCust");
        appt.setDate(future.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        appt.setTime(future.format(DateTimeFormatter.ofPattern("hh:mm a")));

        double refundPct = appointmentService.calculateRefundPercentage(appt, now);
        assertEquals(80.0, refundPct); // Custom 80% instead of default 75%
    }

    // =========================================================
    // 5. CANCELLATION REFUND PREVIEW TESTS
    // =========================================================

    @Test
    void test15_preview_unauthenticated_throwsSecurityException() {
        assertThrows(SecurityException.class, () ->
                appointmentService.getCancellationRefundPreview("appt1", "")
        );
    }

    @Test
    void test16_preview_nonOwner_throwsSecurityException() {
        Patient patient = new Patient();
        patient.setId("patient123");
        patient.setEmail("alice@test.com");

        Appointment appt = new Appointment();
        appt.setId("appt1");
        appt.setPatientId("someoneElse");

        when(appointmentRepository.findById("appt1")).thenReturn(Optional.of(appt));
        when(patientRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(patient));

        assertThrows(SecurityException.class, () ->
                appointmentService.getCancellationRefundPreview("appt1", "alice@test.com")
        );
    }

    @Test
    void test17_preview_alreadyCancelled_throwsIllegalArgumentException() {
        Patient patient = new Patient();
        patient.setId("patient1");
        patient.setEmail("alice@test.com");

        Appointment appt = new Appointment();
        appt.setId("appt1");
        appt.setPatientId("patient1");
        appt.setStatus("CANCELLED");

        when(appointmentRepository.findById("appt1")).thenReturn(Optional.of(appt));
        when(patientRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(patient));

        assertThrows(IllegalArgumentException.class, () ->
                appointmentService.getCancellationRefundPreview("appt1", "alice@test.com")
        );
    }

    @Test
    void test18_preview_completed_throwsIllegalArgumentException() {
        Patient patient = new Patient();
        patient.setId("patient1");
        patient.setEmail("alice@test.com");

        Appointment appt = new Appointment();
        appt.setId("appt1");
        appt.setPatientId("patient1");
        appt.setStatus("COMPLETED");

        when(appointmentRepository.findById("appt1")).thenReturn(Optional.of(appt));
        when(patientRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(patient));

        assertThrows(IllegalArgumentException.class, () ->
                appointmentService.getCancellationRefundPreview("appt1", "alice@test.com")
        );
    }

    @Test
    void test19_preview_pastAppointment_throwsIllegalArgumentException() {
        Patient patient = new Patient();
        patient.setId("patient1");
        patient.setEmail("alice@test.com");

        LocalDateTime past = LocalDateTime.now().minusDays(1);
        Appointment appt = new Appointment();
        appt.setId("appt1");
        appt.setPatientId("patient1");
        appt.setStatus("CONFIRMED");
        appt.setDate(past.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        appt.setTime(past.format(DateTimeFormatter.ofPattern("hh:mm a")));

        when(appointmentRepository.findById("appt1")).thenReturn(Optional.of(appt));
        when(patientRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(patient));

        assertThrows(IllegalArgumentException.class, () ->
                appointmentService.getCancellationRefundPreview("appt1", "alice@test.com")
        );
    }

    @Test
    void test20_preview_paidAppointment_calculatesEligibleRefund() {
        Patient patient = new Patient();
        patient.setId("patient1");
        patient.setEmail("alice@test.com");

        LocalDateTime future = LocalDateTime.now().plusHours(50); // 48+ hours -> 90%
        Appointment appt = new Appointment();
        appt.setId("appt1");
        appt.setPatientId("patient1");
        appt.setStatus("PAID");
        appt.setPaid(true);
        appt.setAmount(2000.0);
        appt.setDate(future.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        appt.setTime(future.format(DateTimeFormatter.ofPattern("hh:mm a")));

        when(appointmentRepository.findById("appt1")).thenReturn(Optional.of(appt));
        when(patientRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(patient));

        RefundPreviewDTO preview = appointmentService.getCancellationRefundPreview("appt1", "alice@test.com");
        assertNotNull(preview);
        assertTrue(preview.isPaid());
        assertEquals(90.0, preview.getRefundPercentage());
        assertEquals(1800.0, preview.getEligibleRefund());
        assertTrue(preview.isEligibleForRefund());
    }

    @Test
    void test21_preview_unpaidAppointment_returnsZeroRefund() {
        Patient patient = new Patient();
        patient.setId("patient1");
        patient.setEmail("alice@test.com");

        LocalDateTime future = LocalDateTime.now().plusHours(50);
        Appointment appt = new Appointment();
        appt.setId("appt1");
        appt.setPatientId("patient1");
        appt.setStatus("CONFIRMED");
        appt.setPaid(false);
        appt.setAmount(2000.0);
        appt.setDate(future.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        appt.setTime(future.format(DateTimeFormatter.ofPattern("hh:mm a")));

        when(appointmentRepository.findById("appt1")).thenReturn(Optional.of(appt));
        when(patientRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(patient));

        RefundPreviewDTO preview = appointmentService.getCancellationRefundPreview("appt1", "alice@test.com");
        assertNotNull(preview);
        assertFalse(preview.isPaid());
        assertEquals(0.0, preview.getEligibleRefund());
        assertFalse(preview.isEligibleForRefund());
    }

    // =========================================================
    // 6. PATIENT CANCELLATION EXECUTION TESTS
    // =========================================================

    @Test
    void test22_cancelByPatient_paidAppointment_recordsRefundAuditAndPreservesAmount() {
        Patient patient = new Patient();
        patient.setId("patient1");
        patient.setEmail("alice@test.com");
        patient.setFirstName("Alice");
        patient.setLastName("Wonderland");

        LocalDateTime future = LocalDateTime.now().plusHours(30); // 24-48h -> 75%
        Appointment appt = new Appointment();
        appt.setId("appt1");
        appt.setPatientId("patient1");
        appt.setStatus("PAID");
        appt.setPaid(true);
        appt.setPaymentStatus("PAID");
        appt.setAmount(3000.0);
        appt.setDate(future.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        appt.setTime(future.format(DateTimeFormatter.ofPattern("hh:mm a")));

        when(appointmentRepository.findById("appt1")).thenReturn(Optional.of(appt));
        when(patientRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(patient));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment cancelled = appointmentService.cancelAppointmentByPatient("appt1", "alice@test.com", "Conflict");

        assertEquals("CANCELLED", cancelled.getStatus());
        assertEquals("PATIENT", cancelled.getCancelledBy());
        assertEquals("Conflict", cancelled.getRefundReason());
        assertEquals(75.0, cancelled.getRefundPercentage());
        assertEquals(2250.0, cancelled.getRefundAmount());
        // Refund lifecycle: Awaiting patient bank details (not prematurely marked REFUNDED)
        assertEquals("AWAITING_BANK_DETAILS", cancelled.getRefundStatus());
        assertNull(cancelled.getRefundedAt());
        // Verify original payment amount was untouched!
        assertEquals(3000.0, cancelled.getAmount());
    }

    @Test
    void test23_cancelByPatient_unpaidAppointment_noRefundRecorded() {
        Patient patient = new Patient();
        patient.setId("patient1");
        patient.setEmail("alice@test.com");
        patient.setFirstName("Alice");
        patient.setLastName("Wonderland");

        LocalDateTime future = LocalDateTime.now().plusHours(50);
        Appointment appt = new Appointment();
        appt.setId("appt1");
        appt.setPatientId("patient1");
        appt.setStatus("CONFIRMED");
        appt.setPaid(false);
        appt.setDate(future.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        appt.setTime(future.format(DateTimeFormatter.ofPattern("hh:mm a")));

        when(appointmentRepository.findById("appt1")).thenReturn(Optional.of(appt));
        when(patientRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(patient));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment cancelled = appointmentService.cancelAppointmentByPatient("appt1", "alice@test.com", "Changed mind");

        assertEquals("CANCELLED", cancelled.getStatus());
        assertEquals("PATIENT", cancelled.getCancelledBy());
        assertEquals(0.0, cancelled.getRefundPercentage());
        assertEquals(0.0, cancelled.getRefundAmount());
        assertEquals("NOT_APPLICABLE", cancelled.getRefundStatus());
        assertEquals("FAILED", cancelled.getPaymentStatus());
        assertNull(cancelled.getRefundedAt());
    }

    // =========================================================
    // 7. HOSPITAL CANCELLATION TESTS (100% REFUND)
    // =========================================================

    @Test
    void test24_cancelByHospital_paidAppointment_records100PercentRefund() {
        Hospital hospital = new Hospital();
        hospital.setId("hosp1");
        hospital.setEmail("hosp@test.com");
        hospital.setName("City Hospital");

        Appointment appt = new Appointment();
        appt.setId("appt1");
        appt.setHospitalId("hosp1");
        appt.setStatus("PAID");
        appt.setPaid(true);
        appt.setAmount(4500.0);

        when(hospitalRepository.findByEmail("hosp@test.com")).thenReturn(Optional.of(hospital));
        when(appointmentRepository.findById("appt1")).thenReturn(Optional.of(appt));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment cancelled = appointmentService.cancelAppointmentByHospital("appt1", "hosp@test.com", "Doctor unavailable");

        assertEquals("CANCELLED", cancelled.getStatus());
        assertEquals("HOSPITAL", cancelled.getCancelledBy());
        assertEquals(100.0, cancelled.getRefundPercentage());
        assertEquals(4500.0, cancelled.getRefundAmount());
        assertEquals("AWAITING_BANK_DETAILS", cancelled.getRefundStatus());
        assertNull(cancelled.getRefundedAt());
    }

    // =========================================================
    // 8. SCHEDULE CANCELLATION TESTS (DOCTOR 100% REFUND)
    // =========================================================

    @Test
    void test25_scheduleCancellation_paidAppointments_records100PercentRefund() {
        Schedule schedule = new Schedule();
        schedule.setId("sched1");
        schedule.setDoctorId("doc1");
        schedule.setHospitalId("hosp1");
        schedule.setDate("2026-11-20");
        schedule.setStartTime("10:00 AM");
        schedule.setStatus("ACCEPTED");

        Doctor doctor = new Doctor();
        doctor.setId("doc1");
        doctor.setFirstName("John");
        doctor.setLastName("Smith");

        Hospital hospital = new Hospital();
        hospital.setId("hosp1");
        hospital.setName("General Hospital");

        Appointment paidAppt = new Appointment();
        paidAppt.setId("apptPaid");
        paidAppt.setScheduleId("sched1");
        paidAppt.setStatus("PAID");
        paidAppt.setPaid(true);
        paidAppt.setAmount(2500.0);
        paidAppt.setPatientId("p1");

        Appointment unpaidAppt = new Appointment();
        unpaidAppt.setId("apptUnpaid");
        unpaidAppt.setScheduleId("sched1");
        unpaidAppt.setStatus("CONFIRMED");
        unpaidAppt.setPaid(false);
        unpaidAppt.setPatientId("p2");

        when(scheduleRepository.findById("sched1")).thenReturn(Optional.of(schedule));
        when(doctorRepository.findById("doc1")).thenReturn(Optional.of(doctor));
        when(hospitalRepository.findById("hosp1")).thenReturn(Optional.of(hospital));
        when(appointmentRepository.findByScheduleId("sched1")).thenReturn(List.of(paidAppt, unpaidAppt));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(inv -> inv.getArgument(0));

        scheduleService.cancelSchedule("sched1");

        // Paid appointment receives 100% refund, awaiting bank details
        assertEquals("CANCELLED", paidAppt.getStatus());
        assertEquals("DOCTOR", paidAppt.getCancelledBy());
        assertEquals(100.0, paidAppt.getRefundPercentage());
        assertEquals(2500.0, paidAppt.getRefundAmount());
        assertEquals("AWAITING_BANK_DETAILS", paidAppt.getRefundStatus());
        assertNull(paidAppt.getRefundedAt());

        // Unpaid appointment receives no refund
        assertEquals("CANCELLED", unpaidAppt.getStatus());
        assertEquals("DOCTOR", unpaidAppt.getCancelledBy());
        assertEquals(0.0, unpaidAppt.getRefundPercentage());
        assertEquals(0.0, unpaidAppt.getRefundAmount());
        assertEquals("NOT_APPLICABLE", unpaidAppt.getRefundStatus());
        assertEquals("FAILED", unpaidAppt.getPaymentStatus());
    }

    // =========================================================
    // 9. BANK DETAILS SUBMISSION & REFUND CONFIRMATION TESTS
    // =========================================================

    @Test
    void test26_patientSubmitBankDetails_validatesOwnershipAndTransitionsStatus() {
        Patient patient = new Patient();
        patient.setId("patient1");
        patient.setEmail("alice@test.com");
        patient.setFirstName("Alice");
        patient.setLastName("Wonderland");

        Appointment appt = new Appointment();
        appt.setId("appt1");
        appt.setPatientId("patient1");
        appt.setStatus("CANCELLED");
        appt.setRefundStatus("AWAITING_BANK_DETAILS");
        appt.setRefundAmount(2000.0);

        when(patientRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(patient));
        when(appointmentRepository.findById("appt1")).thenReturn(Optional.of(appt));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        com.example.demo.dto.BankDetailsRequest request = new com.example.demo.dto.BankDetailsRequest();
        request.setAccountHolderName("Alice Wonderland");
        request.setBankName("Commercial Bank");
        request.setBranchName("Colombo 03");
        request.setAccountNumber("1234567890");

        Appointment updated = appointmentService.submitBankDetails("appt1", "alice@test.com", request);

        assertEquals("AWAITING_PROCESSING", updated.getRefundStatus());
        assertEquals("Alice Wonderland", updated.getBankAccountHolderName());
        assertEquals("Commercial Bank", updated.getBankName());
        assertEquals("Colombo 03", updated.getBankBranchName());
        assertEquals("1234567890", updated.getBankAccountNumber());
        assertEquals("****7890", updated.getMaskedBankAccountNumber());
        assertNotNull(updated.getBankDetailsSubmittedAt());
    }

    @Test
    void test27_patientSubmitBankDetails_rejectsUnauthorizedPatient() {
        Patient otherPatient = new Patient();
        otherPatient.setId("intruder");
        otherPatient.setEmail("eve@test.com");

        Appointment appt = new Appointment();
        appt.setId("appt1");
        appt.setPatientId("patient1");
        appt.setStatus("CANCELLED");
        appt.setRefundStatus("AWAITING_BANK_DETAILS");
        appt.setRefundAmount(2000.0);

        when(patientRepository.findByEmail("eve@test.com")).thenReturn(Optional.of(otherPatient));
        when(appointmentRepository.findById("appt1")).thenReturn(Optional.of(appt));

        com.example.demo.dto.BankDetailsRequest request = new com.example.demo.dto.BankDetailsRequest();
        request.setAccountHolderName("Eve");
        request.setBankName("Bank");
        request.setBranchName("Branch");
        request.setAccountNumber("123456789");

        assertThrows(SecurityException.class, () ->
                appointmentService.submitBankDetails("appt1", "eve@test.com", request)
        );
    }

    @Test
    void test27a_patientSubmitBankDetails_missingBranchName_throwsIllegalArgumentException() {
        com.example.demo.dto.BankDetailsRequest request = new com.example.demo.dto.BankDetailsRequest();
        request.setAccountHolderName("Alice Wonderland");
        request.setBankName("Commercial Bank");
        request.setBranchName(""); // missing branch name
        request.setAccountNumber("1234567890");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                appointmentService.submitBankDetails("appt1", "alice@test.com", request)
        );
        assertTrue(ex.getMessage().contains("Branch name is required"));
    }

    @Test
    void test27b_patientSubmitBankDetails_missingRequiredFields_throwsIllegalArgumentException() {
        // Missing account holder name
        com.example.demo.dto.BankDetailsRequest reqNoHolder = new com.example.demo.dto.BankDetailsRequest();
        reqNoHolder.setBankName("Bank");
        reqNoHolder.setBranchName("Branch");
        reqNoHolder.setAccountNumber("12345678");
        assertThrows(IllegalArgumentException.class, () ->
                appointmentService.submitBankDetails("appt1", "alice@test.com", reqNoHolder)
        );

        // Missing bank name
        com.example.demo.dto.BankDetailsRequest reqNoBank = new com.example.demo.dto.BankDetailsRequest();
        reqNoBank.setAccountHolderName("Alice");
        reqNoBank.setBranchName("Branch");
        reqNoBank.setAccountNumber("12345678");
        assertThrows(IllegalArgumentException.class, () ->
                appointmentService.submitBankDetails("appt1", "alice@test.com", reqNoBank)
        );

        // Missing account number
        com.example.demo.dto.BankDetailsRequest reqNoAcc = new com.example.demo.dto.BankDetailsRequest();
        reqNoAcc.setAccountHolderName("Alice");
        reqNoAcc.setBankName("Bank");
        reqNoAcc.setBranchName("Branch");
        assertThrows(IllegalArgumentException.class, () ->
                appointmentService.submitBankDetails("appt1", "alice@test.com", reqNoAcc)
        );

        // Invalid account number format
        com.example.demo.dto.BankDetailsRequest reqInvalidAcc = new com.example.demo.dto.BankDetailsRequest();
        reqInvalidAcc.setAccountHolderName("Alice");
        reqInvalidAcc.setBankName("Bank");
        reqInvalidAcc.setBranchName("Branch");
        reqInvalidAcc.setAccountNumber("12");
        assertThrows(IllegalArgumentException.class, () ->
                appointmentService.submitBankDetails("appt1", "alice@test.com", reqInvalidAcc)
        );
    }

    @Test
    void test27c_patientSubmitBankDetails_unauthorizedRequest_throwsSecurityException() {
        com.example.demo.dto.BankDetailsRequest request = new com.example.demo.dto.BankDetailsRequest();
        request.setAccountHolderName("Alice");
        request.setBankName("Bank");
        request.setBranchName("Branch");
        request.setAccountNumber("12345678");

        // Null patient email
        assertThrows(SecurityException.class, () ->
                appointmentService.submitBankDetails("appt1", null, request)
        );

        // Non-existent patient
        when(patientRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());
        assertThrows(SecurityException.class, () ->
                appointmentService.submitBankDetails("appt1", "unknown@test.com", request)
        );
    }

    @Test
    void test27d_patientSubmitBankDetails_acceptsBankBranchNameAlias_succeeds() {
        Patient patient = new Patient();
        patient.setId("patient1");
        patient.setEmail("alice@test.com");

        Appointment appt = new Appointment();
        appt.setId("appt1");
        appt.setPatientId("patient1");
        appt.setStatus("CANCELLED");
        appt.setRefundStatus("AWAITING_BANK_DETAILS");
        appt.setRefundAmount(2000.0);

        when(patientRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(patient));
        when(appointmentRepository.findById("appt1")).thenReturn(Optional.of(appt));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        com.example.demo.dto.BankDetailsRequest request = new com.example.demo.dto.BankDetailsRequest();
        request.setAccountHolderName("Alice Wonderland");
        request.setBankName("Commercial Bank");
        request.setBankBranchName("Kandy Branch");
        request.setAccountNumber("9876543210");

        Appointment updated = appointmentService.submitBankDetails("appt1", "alice@test.com", request);

        assertEquals("AWAITING_PROCESSING", updated.getRefundStatus());
        assertEquals("Commercial Bank", updated.getBankName());
        assertEquals("Kandy Branch", updated.getBankBranchName());
        assertEquals("9876543210", updated.getBankAccountNumber());
    }

    @Test
    void test28_doctorCancelSchedule_withActiveBookings_cascadesAndRefunds100Percent() {
        Schedule schedule = new Schedule();
        schedule.setId("schedDoc");
        schedule.setDoctorId("doc1");
        schedule.setHospitalId("hosp1");
        schedule.setDate("2026-11-20");
        schedule.setStartTime("09:00 AM");
        schedule.setStatus("ACCEPTED");

        Doctor doctor = new Doctor();
        doctor.setId("doc1");
        doctor.setEmail("doc1@test.com");
        doctor.setFirstName("Emily");
        doctor.setLastName("Clark");

        Hospital hospital = new Hospital();
        hospital.setId("hosp1");
        hospital.setName("Apex Hospital");

        Appointment activeBooking = new Appointment();
        activeBooking.setId("apptBooked");
        activeBooking.setScheduleId("schedDoc");
        activeBooking.setStatus("CONFIRMED");
        activeBooking.setPaid(true);
        activeBooking.setPaymentStatus("PAID");
        activeBooking.setAmount(3500.0);
        activeBooking.setPatientId("patientX");

        when(scheduleRepository.findById("schedDoc")).thenReturn(Optional.of(schedule));
        when(doctorRepository.findByEmail("doc1@test.com")).thenReturn(Optional.of(doctor));
        when(doctorRepository.findById("doc1")).thenReturn(Optional.of(doctor));
        when(hospitalRepository.findById("hosp1")).thenReturn(Optional.of(hospital));
        when(appointmentRepository.findByScheduleId("schedDoc")).thenReturn(List.of(activeBooking));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(inv -> inv.getArgument(0));

        Schedule result = scheduleService.cancelScheduleByDoctor("schedDoc", "doc1@test.com", "Family Emergency");

        assertEquals("CANCELLED", result.getStatus());
        assertEquals("CANCELLED", activeBooking.getStatus());
        assertEquals("DOCTOR", activeBooking.getCancelledBy());
        assertEquals(100.0, activeBooking.getRefundPercentage());
        assertEquals(3500.0, activeBooking.getRefundAmount());
        assertEquals("AWAITING_BANK_DETAILS", activeBooking.getRefundStatus());
    }

    @Test
    void test29_doctorCancelSchedule_rejectsUnauthorizedDoctor() {
        Schedule schedule = new Schedule();
        schedule.setId("schedDoc");
        schedule.setDoctorId("docReal");
        schedule.setStatus("ACCEPTED");

        Doctor unauthorizedDoctor = new Doctor();
        unauthorizedDoctor.setId("docImposter");
        unauthorizedDoctor.setEmail("imposter@test.com");

        when(scheduleRepository.findById("schedDoc")).thenReturn(Optional.of(schedule));
        when(doctorRepository.findByEmail("imposter@test.com")).thenReturn(Optional.of(unauthorizedDoctor));

        assertThrows(SecurityException.class, () ->
                scheduleService.cancelScheduleByDoctor("schedDoc", "imposter@test.com", "Trying to cancel")
        );
    }

    @Test
    void test30_hospitalCancelSchedule_withBookings_cascadesAndRefunds100Percent() {
        Schedule schedule = new Schedule();
        schedule.setId("schedHosp");
        schedule.setDoctorId("doc1");
        schedule.setHospitalId("hosp1");
        schedule.setDate("2026-11-25");
        schedule.setStartTime("10:00 AM");
        schedule.setStatus("ACCEPTED");

        Hospital hospital = new Hospital();
        hospital.setId("hosp1");
        hospital.setEmail("hosp@apex.com");
        hospital.setName("Apex Hospital");

        Doctor doctor = new Doctor();
        doctor.setId("doc1");
        doctor.setFirstName("Sarah");
        doctor.setLastName("Connor");

        Appointment appt = new Appointment();
        appt.setId("apptHosp");
        appt.setScheduleId("schedHosp");
        appt.setStatus("CONFIRMED");
        appt.setPaid(true);
        appt.setPaymentStatus("PAID");
        appt.setAmount(4000.0);
        appt.setPatientId("patientY");

        when(hospitalRepository.findByEmail("hosp@apex.com")).thenReturn(Optional.of(hospital));
        when(scheduleRepository.findById("schedHosp")).thenReturn(Optional.of(schedule));
        when(doctorRepository.findById("doc1")).thenReturn(Optional.of(doctor));
        when(appointmentRepository.findByScheduleId("schedHosp")).thenReturn(List.of(appt));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(inv -> inv.getArgument(0));

        Schedule cancelled = scheduleService.cancelScheduleByHospital("schedHosp", "hosp@apex.com", "Ward Renovation");

        assertEquals("CANCELLED", cancelled.getStatus());
        assertEquals("CANCELLED", appt.getStatus());
        assertEquals("HOSPITAL", appt.getCancelledBy());
        assertEquals(100.0, appt.getRefundPercentage());
        assertEquals(4000.0, appt.getRefundAmount());
        assertEquals("AWAITING_BANK_DETAILS", appt.getRefundStatus());
    }

    @Test
    void test31_hospitalCancelSchedule_rejectsAnotherHospital() {
        Schedule schedule = new Schedule();
        schedule.setId("schedHosp");
        schedule.setHospitalId("hospOriginal");
        schedule.setStatus("ACCEPTED");

        Hospital otherHospital = new Hospital();
        otherHospital.setId("hospRival");
        otherHospital.setEmail("rival@hospital.com");

        when(hospitalRepository.findByEmail("rival@hospital.com")).thenReturn(Optional.of(otherHospital));
        when(scheduleRepository.findById("schedHosp")).thenReturn(Optional.of(schedule));

        assertThrows(SecurityException.class, () ->
                scheduleService.cancelScheduleByHospital("schedHosp", "rival@hospital.com", "Rival cancel")
        );
    }

    @Test
    void test32_hospitalPendingRefunds_onlyReturnsOwnHospitalAwaitingRefunds() {
        Hospital hospital = new Hospital();
        hospital.setId("hosp1");
        hospital.setEmail("hosp@test.com");

        Appointment appt1 = new Appointment();
        appt1.setId("appt1");
        appt1.setHospitalId("hosp1");
        appt1.setStatus("CANCELLED");
        appt1.setRefundStatus("AWAITING_BANK_DETAILS");

        Appointment appt2 = new Appointment();
        appt2.setId("appt2");
        appt2.setHospitalId("hosp1");
        appt2.setStatus("CANCELLED");
        appt2.setRefundStatus("AWAITING_PROCESSING");

        Appointment apptAlreadyRefunded = new Appointment();
        apptAlreadyRefunded.setId("appt3");
        apptAlreadyRefunded.setHospitalId("hosp1");
        apptAlreadyRefunded.setStatus("CANCELLED");
        apptAlreadyRefunded.setRefundStatus("REFUNDED");

        when(hospitalRepository.findByEmail("hosp@test.com")).thenReturn(Optional.of(hospital));
        when(appointmentRepository.findByHospitalId("hosp1")).thenReturn(List.of(appt1, appt2, apptAlreadyRefunded));

        List<com.example.demo.dto.AppointmentResponseDTO> pending = appointmentService.getPendingRefundsForHospital("hosp1", "hosp@test.com");

        assertEquals(2, pending.size());
        assertTrue(pending.stream().anyMatch(p -> p.getId().equals("appt1")));
        assertTrue(pending.stream().anyMatch(p -> p.getId().equals("appt2")));
        assertFalse(pending.stream().anyMatch(p -> p.getId().equals("appt3")));
    }

    @Test
    void test33_hospitalConfirmRefundTransfer_transitionsToRefundedWithTimestampAndRef() {
        Hospital hospital = new Hospital();
        hospital.setId("hosp1");
        hospital.setEmail("hosp@test.com");
        hospital.setName("Apex Hospital");

        Appointment appt = new Appointment();
        appt.setId("appt1");
        appt.setHospitalId("hosp1");
        appt.setStatus("CANCELLED");
        appt.setRefundStatus("AWAITING_PROCESSING");
        appt.setRefundAmount(3000.0);

        when(hospitalRepository.findByEmail("hosp@test.com")).thenReturn(Optional.of(hospital));
        when(appointmentRepository.findById("appt1")).thenReturn(Optional.of(appt));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment refunded = appointmentService.confirmRefundTransfer("appt1", "hosp@test.com", "BANK-TXN-9988", "Transfer complete");

        assertEquals("REFUNDED", refunded.getRefundStatus());
        assertNotNull(refunded.getRefundedAt());
        assertEquals("BANK-TXN-9988", refunded.getRefundTransactionRef());
        assertEquals("Apex Hospital", refunded.getRefundConfirmedBy());
    }

    @Test
    void test34_hospitalConfirmRefundTransfer_preventsDoubleRefund() {
        Hospital hospital = new Hospital();
        hospital.setId("hosp1");
        hospital.setEmail("hosp@test.com");

        Appointment appt = new Appointment();
        appt.setId("appt1");
        appt.setHospitalId("hosp1");
        appt.setStatus("CANCELLED");
        appt.setRefundStatus("REFUNDED"); // already refunded!
        appt.setRefundAmount(3000.0);

        when(hospitalRepository.findByEmail("hosp@test.com")).thenReturn(Optional.of(hospital));
        when(appointmentRepository.findById("appt1")).thenReturn(Optional.of(appt));

        assertThrows(IllegalStateException.class, () ->
                appointmentService.confirmRefundTransfer("appt1", "hosp@test.com", "TXN-2", "Double")
        );
    }

    @Test
    void test35_hospitalConfirmRefundTransfer_rejectsUnauthorizedHospital() {
        Hospital otherHosp = new Hospital();
        otherHosp.setId("hospOther");
        otherHosp.setEmail("other@hosp.com");

        Appointment appt = new Appointment();
        appt.setId("appt1");
        appt.setHospitalId("hosp1");
        appt.setStatus("CANCELLED");
        appt.setRefundStatus("AWAITING_PROCESSING");
        appt.setRefundAmount(3000.0);

        when(hospitalRepository.findByEmail("other@hosp.com")).thenReturn(Optional.of(otherHosp));
        when(appointmentRepository.findById("appt1")).thenReturn(Optional.of(appt));

        assertThrows(SecurityException.class, () ->
                appointmentService.confirmRefundTransfer("appt1", "other@hosp.com", "TXN", "Notes")
        );
    }

    // =========================================================
    // 10. SCHEDULE REJECTION & BOOKING PROTECTION TESTS
    // =========================================================

    @Test
    void test36_rejectPendingSchedule_withoutCancellationOrRefundProcessing() {
        Schedule schedule = new Schedule();
        schedule.setId("schedPending");
        schedule.setDoctorId("doc1");
        schedule.setHospitalId("hosp1");
        schedule.setDate("2026-11-20");
        schedule.setStartTime("10:00 AM");
        schedule.setEndTime("12:00 PM");
        schedule.setType("PHYSICAL");
        schedule.setStatus("PENDING");

        Doctor doctor = new Doctor();
        doctor.setId("doc1");
        doctor.setEmail("doc1@test.com");
        doctor.setFirstName("Gregory");
        doctor.setLastName("House");

        when(scheduleRepository.findById("schedPending")).thenReturn(Optional.of(schedule));
        when(doctorRepository.findByEmail("doc1@test.com")).thenReturn(Optional.of(doctor));
        when(doctorRepository.findById("doc1")).thenReturn(Optional.of(doctor));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(inv -> inv.getArgument(0));

        Schedule rejected = scheduleService.rejectScheduleByDoctor("schedPending", "doc1@test.com", "Medical conference attendance");

        // Verify status and rejection reason
        assertEquals("REJECTED", rejected.getStatus());
        assertEquals("Medical conference attendance", rejected.getRejectionReason());

        // Verify hospital notification was created
        verify(notificationRepository, times(1)).save(any(Notification.class));

        // CRITICAL: Rejection must NOT trigger appointment cancellation, refund processing, or patient cancellation emails
        verify(appointmentRepository, never()).findByScheduleId(anyString());
        verify(appointmentRepository, never()).save(any(Appointment.class));
        verify(emailService, never()).sendDoctorScheduleCancellationEmail(any(), any(), any(), any(), any(), any(), any(), any(), any(), anyDouble(), anyBoolean());
        verify(emailService, never()).sendHospitalScheduleCancellationEmail(any(), any(), any(), any(), any(), any(), any(), any(), any(), anyDouble(), anyBoolean());
    }

    @Test
    void test37_rejectSchedule_rejectsNonPendingSchedule() {
        Schedule acceptedSchedule = new Schedule();
        acceptedSchedule.setId("schedAccepted");
        acceptedSchedule.setDate("2026-11-20");
        acceptedSchedule.setStartTime("10:00 AM");
        acceptedSchedule.setEndTime("12:00 PM");
        acceptedSchedule.setStatus("ACCEPTED");

        when(scheduleRepository.findById("schedAccepted")).thenReturn(Optional.of(acceptedSchedule));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                scheduleService.rejectSchedule("schedAccepted", "Already accepted")
        );
        assertTrue(ex.getMessage().contains("Only pending schedules can be rejected"));
        verify(scheduleRepository, never()).save(any(Schedule.class));
    }

    @Test
    void test38_bookingProtection_rejectedScheduleCannotReceiveNewBookings() {
        Schedule rejectedSchedule = new Schedule();
        rejectedSchedule.setId("schedRejected");
        rejectedSchedule.setDate("2026-12-01");
        rejectedSchedule.setStartTime("10:00 AM");
        rejectedSchedule.setEndTime("12:00 PM");
        rejectedSchedule.setStatus("REJECTED");

        Appointment appt = new Appointment();
        appt.setPatientId("patient1");
        appt.setDoctorId("doc1");
        appt.setScheduleId("schedRejected");
        appt.setAppointmentNumber("1");

        when(scheduleRepository.findById("schedRejected")).thenReturn(Optional.of(rejectedSchedule));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                appointmentService.bookAppointment(appt)
        );
        assertTrue(ex.getMessage().contains("rejected schedule"));
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void test39_bookingProtection_pendingScheduleCannotReceiveNewBookings() {
        Schedule pendingSchedule = new Schedule();
        pendingSchedule.setId("schedPending");
        pendingSchedule.setDate("2026-12-01");
        pendingSchedule.setStartTime("10:00 AM");
        pendingSchedule.setEndTime("12:00 PM");
        pendingSchedule.setStatus("PENDING");

        Appointment appt = new Appointment();
        appt.setPatientId("patient1");
        appt.setDoctorId("doc1");
        appt.setScheduleId("schedPending");
        appt.setAppointmentNumber("1");

        when(scheduleRepository.findById("schedPending")).thenReturn(Optional.of(pendingSchedule));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                appointmentService.bookAppointment(appt)
        );
        assertTrue(ex.getMessage().contains("pending doctor acceptance"));
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void test40_bookingProtection_cancelledScheduleCannotReceiveNewBookings() {
        Schedule cancelledSchedule = new Schedule();
        cancelledSchedule.setId("schedCancelled");
        cancelledSchedule.setDate("2026-12-01");
        cancelledSchedule.setStartTime("10:00 AM");
        cancelledSchedule.setEndTime("12:00 PM");
        cancelledSchedule.setStatus("CANCELLED");

        Appointment appt = new Appointment();
        appt.setPatientId("patient1");
        appt.setDoctorId("doc1");
        appt.setScheduleId("schedCancelled");
        appt.setAppointmentNumber("1");

        when(scheduleRepository.findById("schedCancelled")).thenReturn(Optional.of(cancelledSchedule));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                appointmentService.bookAppointment(appt)
        );
        assertTrue(ex.getMessage().contains("cancelled schedule"));
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void test41_acceptedSchedule_withOneActiveBooking_canBeCancelled_cascadesAndRefunds100Percent() {
        Schedule schedule = new Schedule();
        schedule.setId("schedSingle");
        schedule.setDoctorId("doc1");
        schedule.setHospitalId("hosp1");
        schedule.setDate("2026-11-20");
        schedule.setStartTime("09:00 AM");
        schedule.setEndTime("11:00 AM");
        schedule.setType("PHYSICAL");
        schedule.setStatus("ACCEPTED");

        Doctor doctor = new Doctor();
        doctor.setId("doc1");
        doctor.setEmail("doc1@test.com");
        doctor.setFirstName("James");
        doctor.setLastName("Wilson");

        Hospital hospital = new Hospital();
        hospital.setId("hosp1");
        hospital.setName("Metro Hospital");

        Patient patient = new Patient();
        patient.setId("patSingle");
        patient.setFirstName("John");
        patient.setEmail("john@patient.com");
        patient.setContactNumber("0771234567");

        Appointment booking = new Appointment();
        booking.setId("apptSingle");
        booking.setScheduleId("schedSingle");
        booking.setStatus("CONFIRMED");
        booking.setPaid(true);
        booking.setPaymentStatus("PAID");
        booking.setAmount(2500.0);
        booking.setPatientId("patSingle");
        booking.setAppointmentNumber("APT-001");

        when(scheduleRepository.findById("schedSingle")).thenReturn(Optional.of(schedule));
        when(doctorRepository.findByEmail("doc1@test.com")).thenReturn(Optional.of(doctor));
        when(doctorRepository.findById("doc1")).thenReturn(Optional.of(doctor));
        when(hospitalRepository.findById("hosp1")).thenReturn(Optional.of(hospital));
        when(patientRepository.findById("patSingle")).thenReturn(Optional.of(patient));
        when(appointmentRepository.findByScheduleId("schedSingle")).thenReturn(List.of(booking));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(inv -> inv.getArgument(0));

        Schedule cancelled = scheduleService.cancelScheduleByDoctor("schedSingle", "doc1@test.com", "Urgent Surgery");

        assertEquals("CANCELLED", cancelled.getStatus());
        assertEquals("Urgent Surgery", cancelled.getCancellationReason());

        // Verify booking cascade cancellation & 100% refund in AWAITING_BANK_DETAILS
        assertEquals("CANCELLED", booking.getStatus());
        assertEquals("DOCTOR", booking.getCancelledBy());
        assertEquals(100.0, booking.getRefundPercentage());
        assertEquals(2500.0, booking.getRefundAmount());
        assertEquals("AWAITING_BANK_DETAILS", booking.getRefundStatus());
        assertNull(booking.getRefundedAt());
        assertEquals(2500.0, booking.getAmount()); // original amount preserved

        // Verify notifications & email sent
        verify(notificationRepository, atLeastOnce()).save(any(Notification.class));
        verify(emailService, times(1)).sendDoctorScheduleCancellationEmail(
                eq("john@patient.com"),
                eq("John"),
                eq("Dr. James Wilson"),
                eq("APT-001"),
                eq("appointment"),
                eq("2026-11-20"),
                eq("09:00 AM"),
                eq("Metro Hospital"),
                eq("Urgent Surgery"),
                eq(2500.0),
                eq(true)
        );
    }

    @Test
    void test42_acceptedSchedule_withMultipleActiveBookings_canBeCancelled_cascadesAndRefundsConsistently() {
        Schedule schedule = new Schedule();
        schedule.setId("schedMulti");
        schedule.setDoctorId("doc1");
        schedule.setHospitalId("hosp1");
        schedule.setDate("2026-11-22");
        schedule.setStartTime("08:00 AM");
        schedule.setEndTime("12:00 PM");
        schedule.setType("PHYSICAL");
        schedule.setStatus("ACCEPTED");

        Doctor doctor = new Doctor();
        doctor.setId("doc1");
        doctor.setEmail("doc1@test.com");
        doctor.setFirstName("Lisa");
        doctor.setLastName("Cuddy");

        Hospital hospital = new Hospital();
        hospital.setId("hosp1");
        hospital.setName("Metro Hospital");

        // Booking 1: Paid booking
        Patient pat1 = new Patient();
        pat1.setId("pat1");
        pat1.setFirstName("Arthur");
        pat1.setEmail("arthur@test.com");

        Appointment appt1 = new Appointment();
        appt1.setId("appt1");
        appt1.setScheduleId("schedMulti");
        appt1.setStatus("CONFIRMED");
        appt1.setPaid(true);
        appt1.setPaymentStatus("PAID");
        appt1.setAmount(3000.0);
        appt1.setPatientId("pat1");
        appt1.setAppointmentNumber("APT-001");

        // Booking 2: Another Paid booking
        Patient pat2 = new Patient();
        pat2.setId("pat2");
        pat2.setFirstName("Betty");
        pat2.setEmail("betty@test.com");

        Appointment appt2 = new Appointment();
        appt2.setId("appt2");
        appt2.setScheduleId("schedMulti");
        appt2.setStatus("PAID");
        appt2.setPaid(true);
        appt2.setPaymentStatus("PAID");
        appt2.setAmount(3000.0);
        appt2.setPatientId("pat2");
        appt2.setAppointmentNumber("APT-002");

        // Booking 3: Unpaid / Pending payment booking
        Patient pat3 = new Patient();
        pat3.setId("pat3");
        pat3.setFirstName("Charlie");
        pat3.setEmail("charlie@test.com");

        Appointment appt3 = new Appointment();
        appt3.setId("appt3");
        appt3.setScheduleId("schedMulti");
        appt3.setStatus("PENDING");
        appt3.setPaid(false);
        appt3.setPaymentStatus("PENDING");
        appt3.setAmount(3000.0);
        appt3.setPatientId("pat3");
        appt3.setAppointmentNumber("APT-003");

        // Booking 4: Already cancelled appointment
        Appointment appt4 = new Appointment();
        appt4.setId("appt4");
        appt4.setScheduleId("schedMulti");
        appt4.setStatus("CANCELLED");
        appt4.setCancelledBy("PATIENT");
        appt4.setRefundStatus("AWAITING_BANK_DETAILS");

        when(scheduleRepository.findById("schedMulti")).thenReturn(Optional.of(schedule));
        when(doctorRepository.findByEmail("doc1@test.com")).thenReturn(Optional.of(doctor));
        when(doctorRepository.findById("doc1")).thenReturn(Optional.of(doctor));
        when(hospitalRepository.findById("hosp1")).thenReturn(Optional.of(hospital));
        when(patientRepository.findById("pat1")).thenReturn(Optional.of(pat1));
        when(patientRepository.findById("pat2")).thenReturn(Optional.of(pat2));
        when(patientRepository.findById("pat3")).thenReturn(Optional.of(pat3));
        when(appointmentRepository.findByScheduleId("schedMulti")).thenReturn(List.of(appt1, appt2, appt3, appt4));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(inv -> inv.getArgument(0));

        Schedule cancelled = scheduleService.cancelScheduleByDoctor("schedMulti", "doc1@test.com", "Conference Attendance");

        assertEquals("CANCELLED", cancelled.getStatus());
        assertEquals("Conference Attendance", cancelled.getCancellationReason());

        // Booking 1 & 2: 100% refund, awaiting bank details, not marked refunded prematurely
        assertEquals("CANCELLED", appt1.getStatus());
        assertEquals(100.0, appt1.getRefundPercentage());
        assertEquals(3000.0, appt1.getRefundAmount());
        assertEquals("AWAITING_BANK_DETAILS", appt1.getRefundStatus());
        assertNull(appt1.getRefundedAt());

        assertEquals("CANCELLED", appt2.getStatus());
        assertEquals(100.0, appt2.getRefundPercentage());
        assertEquals(3000.0, appt2.getRefundAmount());
        assertEquals("AWAITING_BANK_DETAILS", appt2.getRefundStatus());
        assertNull(appt2.getRefundedAt());

        // Booking 3: Unpaid booking cancelled with NO false refund!
        assertEquals("CANCELLED", appt3.getStatus());
        assertEquals(0.0, appt3.getRefundPercentage());
        assertEquals(0.0, appt3.getRefundAmount());
        assertEquals("NOT_APPLICABLE", appt3.getRefundStatus());
        assertEquals("FAILED", appt3.getPaymentStatus());

        // Booking 4: Already cancelled booking was skipped/unmodified
        assertEquals("PATIENT", appt4.getCancelledBy());

        // Verify patient emails sent for active bookings
        verify(emailService, times(1)).sendDoctorScheduleCancellationEmail(eq("arthur@test.com"), any(), any(), any(), any(), any(), any(), any(), any(), anyDouble(), eq(true));
        verify(emailService, times(1)).sendDoctorScheduleCancellationEmail(eq("betty@test.com"), any(), any(), any(), any(), any(), any(), any(), any(), anyDouble(), eq(true));
        verify(emailService, times(1)).sendDoctorScheduleCancellationEmail(eq("charlie@test.com"), any(), any(), any(), any(), any(), any(), any(), any(), anyDouble(), eq(false));
    }

    @Test
    void test43_hospitalCancellation_respectsOwnership_andCancelsAcceptedScheduleWithBookings() {
        Schedule schedule = new Schedule();
        schedule.setId("schedHosp1");
        schedule.setHospitalId("hospAlpha");
        schedule.setDoctorId("doc1");
        schedule.setDate("2026-11-28");
        schedule.setStartTime("02:00 PM");
        schedule.setEndTime("05:00 PM");
        schedule.setType("PHYSICAL");
        schedule.setStatus("ACCEPTED");

        Hospital hospAlpha = new Hospital();
        hospAlpha.setId("hospAlpha");
        hospAlpha.setEmail("alpha@hospital.com");
        hospAlpha.setName("Alpha Hospital");

        Hospital hospBeta = new Hospital();
        hospBeta.setId("hospBeta");
        hospBeta.setEmail("beta@hospital.com");
        hospBeta.setName("Beta Hospital");

        Doctor doctor = new Doctor();
        doctor.setId("doc1");
        doctor.setFirstName("Eric");
        doctor.setLastName("Foreman");

        Patient patient = new Patient();
        patient.setId("patAlpha");
        patient.setFirstName("Dan");
        patient.setEmail("dan@test.com");

        Appointment paidBooking = new Appointment();
        paidBooking.setId("apptAlpha");
        paidBooking.setScheduleId("schedHosp1");
        paidBooking.setHospitalId("hospAlpha");
        paidBooking.setStatus("CONFIRMED");
        paidBooking.setPaid(true);
        paidBooking.setPaymentStatus("PAID");
        paidBooking.setAmount(1800.0);
        paidBooking.setPatientId("patAlpha");
        paidBooking.setAppointmentNumber("APT-001");

        when(scheduleRepository.findById("schedHosp1")).thenReturn(Optional.of(schedule));
        when(hospitalRepository.findByEmail("alpha@hospital.com")).thenReturn(Optional.of(hospAlpha));
        when(hospitalRepository.findByEmail("beta@hospital.com")).thenReturn(Optional.of(hospBeta));
        when(doctorRepository.findById("doc1")).thenReturn(Optional.of(doctor));
        when(patientRepository.findById("patAlpha")).thenReturn(Optional.of(patient));
        when(appointmentRepository.findByScheduleId("schedHosp1")).thenReturn(List.of(paidBooking));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1. Beta hospital tries to cancel Alpha's schedule -> SecurityException
        assertThrows(SecurityException.class, () ->
                scheduleService.cancelScheduleByHospital("schedHosp1", "beta@hospital.com", "Unauthorized cancel")
        );

        // 2. Alpha hospital cancels its own schedule -> Success
        Schedule cancelled = scheduleService.cancelScheduleByHospital("schedHosp1", "alpha@hospital.com", "Facility Maintenance");
        assertEquals("CANCELLED", cancelled.getStatus());
        assertEquals("Facility Maintenance", cancelled.getCancellationReason());

        // Booking cascade: 100% refund in AWAITING_BANK_DETAILS
        assertEquals("CANCELLED", paidBooking.getStatus());
        assertEquals("HOSPITAL", paidBooking.getCancelledBy());
        assertEquals(100.0, paidBooking.getRefundPercentage());
        assertEquals(1800.0, paidBooking.getRefundAmount());
        assertEquals("AWAITING_BANK_DETAILS", paidBooking.getRefundStatus());
        assertNull(paidBooking.getRefundedAt());

        verify(emailService, times(1)).sendHospitalScheduleCancellationEmail(
                eq("dan@test.com"),
                eq("Dan"),
                eq("Dr. Eric Foreman"),
                eq("APT-001"),
                eq("appointment"),
                eq("2026-11-28"),
                eq("02:00 PM"),
                eq("Alpha Hospital"),
                eq("Facility Maintenance"),
                eq(1800.0),
                eq(true)
        );
    }

    @Test
    void test44_acceptSchedule_onlyPendingScheduleCanBeAccepted() {
        Schedule pendingSchedule = new Schedule();
        pendingSchedule.setId("schedP");
        pendingSchedule.setDate("2026-11-25");
        pendingSchedule.setStartTime("09:00 AM");
        pendingSchedule.setEndTime("11:00 AM");
        pendingSchedule.setStatus("PENDING");

        Schedule alreadyAcceptedSchedule = new Schedule();
        alreadyAcceptedSchedule.setId("schedA");
        alreadyAcceptedSchedule.setDate("2026-11-25");
        alreadyAcceptedSchedule.setStartTime("09:00 AM");
        alreadyAcceptedSchedule.setEndTime("11:00 AM");
        alreadyAcceptedSchedule.setStatus("ACCEPTED");

        when(scheduleRepository.findById("schedP")).thenReturn(Optional.of(pendingSchedule));
        when(scheduleRepository.findById("schedA")).thenReturn(Optional.of(alreadyAcceptedSchedule));
        when(scheduleRepository.save(any(Schedule.class))).thenAnswer(inv -> inv.getArgument(0));

        // Pending schedule is accepted successfully
        Schedule accepted = scheduleService.acceptSchedule("schedP");
        assertEquals("ACCEPTED", accepted.getStatus());

        // Accepting non-pending schedule throws IllegalStateException
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                scheduleService.acceptSchedule("schedA")
        );
        assertTrue(ex.getMessage().contains("Only pending schedules can be accepted"));
    }

    @Test
    void test45_bookingProtection_acceptedFutureScheduleCanBeBooked() {
        Schedule acceptedSchedule = new Schedule();
        acceptedSchedule.setId("schedAcc");
        acceptedSchedule.setHospitalId("hosp1");
        acceptedSchedule.setDate("2026-12-15");
        acceptedSchedule.setStartTime("10:00 AM");
        acceptedSchedule.setEndTime("12:00 PM");
        acceptedSchedule.setStatus("ACCEPTED");

        Appointment appt = new Appointment();
        appt.setPatientId("patient1");
        appt.setDoctorId("doc1");
        appt.setScheduleId("schedAcc");
        appt.setAppointmentNumber("1");

        when(scheduleRepository.findById("schedAcc")).thenReturn(Optional.of(acceptedSchedule));
        when(appointmentRepository.existsByPatientIdAndDoctorIdAndScheduleId("patient1", "doc1", "schedAcc")).thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        Appointment booked = appointmentService.bookAppointment(appt);

        assertNotNull(booked);
        assertEquals("APT-001", booked.getAppointmentNumber());
        assertEquals("PENDING", booked.getStatus());
        assertEquals("PENDING", booked.getPaymentStatus());
        assertFalse(booked.isPaid());
        assertEquals("hosp1", booked.getHospitalId());
        verify(appointmentRepository, times(1)).save(any(Appointment.class));
    }
}
