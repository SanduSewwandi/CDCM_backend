package com.example.demo;

import com.example.demo.dto.RefundPreviewDTO;
import com.example.demo.model.Appointment;
import com.example.demo.model.CancellationRefundPolicy;
import com.example.demo.model.Doctor;
import com.example.demo.model.Hospital;
import com.example.demo.model.Patient;
import com.example.demo.model.Schedule;
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
import static org.mockito.ArgumentMatchers.any;
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
        assertEquals("REFUNDED", cancelled.getRefundStatus());
        assertNotNull(cancelled.getRefundedAt());
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
        assertNull(cancelled.getRefundStatus());
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
        assertEquals("REFUNDED", cancelled.getRefundStatus());
        assertNotNull(cancelled.getRefundedAt());
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

        // Paid appointment receives 100% refund
        assertEquals("CANCELLED", paidAppt.getStatus());
        assertEquals("DOCTOR", paidAppt.getCancelledBy());
        assertEquals(100.0, paidAppt.getRefundPercentage());
        assertEquals(2500.0, paidAppt.getRefundAmount());
        assertEquals("REFUNDED", paidAppt.getRefundStatus());
        assertNotNull(paidAppt.getRefundedAt());

        // Unpaid appointment receives no refund
        assertEquals("CANCELLED", unpaidAppt.getStatus());
        assertEquals("DOCTOR", unpaidAppt.getCancelledBy());
        assertEquals(0.0, unpaidAppt.getRefundPercentage());
        assertEquals(0.0, unpaidAppt.getRefundAmount());
        assertNull(unpaidAppt.getRefundStatus());
    }
}
