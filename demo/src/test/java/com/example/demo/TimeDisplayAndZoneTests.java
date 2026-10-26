package com.example.demo;

import com.example.demo.dto.AppointmentResponseDTO;
import com.example.demo.model.Appointment;
import com.example.demo.model.Notification;
import com.example.demo.service.EmailService;
import com.example.demo.service.ScheduleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

class TimeDisplayAndZoneTests {

    private TimeZone originalTimeZone;
    private static final ZoneId SRI_LANKA_ZONE = ZoneId.of("Asia/Colombo");

    @BeforeEach
    void setUp() {
        originalTimeZone = TimeZone.getDefault();
    }

    @AfterEach
    void tearDown() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    @DisplayName("EmailService.formatTimeString formats single times to AM/PM")
    void testFormatTimeStringSingleTimes() {
        assertEquals("09:00 AM", EmailService.formatTimeString("09:00"));
        assertEquals("02:30 PM", EmailService.formatTimeString("14:30"));
        assertEquals("12:00 PM", EmailService.formatTimeString("12:00"));
        assertEquals("12:00 AM", EmailService.formatTimeString("00:00"));
        assertEquals("11:59 PM", EmailService.formatTimeString("23:59"));
    }

    @Test
    @DisplayName("EmailService.formatTimeString formats time ranges correctly")
    void testFormatTimeStringRanges() {
        assertEquals("09:00 AM - 11:00 AM", EmailService.formatTimeString("09:00 - 11:00"));
        assertEquals("08:30 AM - 10:30 AM", EmailService.formatTimeString("08:30-10:30"));
        assertEquals("02:00 PM - 04:00 PM", EmailService.formatTimeString("14:00 - 16:00"));
    }

    @Test
    @DisplayName("EmailService.formatTimeString formats near-midnight crossing ranges correctly")
    void testFormatTimeStringMidnightBoundary() {
        // Crossing midnight
        assertEquals("11:30 PM - 12:30 AM", EmailService.formatTimeString("23:30 - 00:30"));
        assertEquals("11:00 PM - 01:00 AM", EmailService.formatTimeString("23:00 - 01:00"));
    }

    @Test
    @DisplayName("EmailService.formatTimeString preserves already formatted AM/PM strings")
    void testFormatTimeStringAlreadyFormatted() {
        assertEquals("09:00 AM - 11:00 AM", EmailService.formatTimeString("09:00 AM - 11:00 AM"));
        assertEquals("10:30 AM", EmailService.formatTimeString("10:30 AM"));
    }

    @Test
    @DisplayName("EmailService.formatTimeString handles null and empty safely by returning placeholder")
    void testFormatTimeStringNullAndEmpty() {
        assertEquals("—", EmailService.formatTimeString(null));
        assertEquals("—", EmailService.formatTimeString(""));
        assertEquals("—", EmailService.formatTimeString("   "));
    }

    @Test
    @DisplayName("Notification.createdAt serializes as an ISO-8601 UTC instant with trailing Z")
    void testNotificationCreatedAtSerialization() throws Exception {
        Notification notification = new Notification();
        notification.setTitle("Appointment Confirmed");
        notification.setMessage("Your appointment has been confirmed");
        Instant fixedInstant = Instant.parse("2026-10-23T15:30:00Z");
        notification.setCreatedAt(fixedInstant);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String json = mapper.writeValueAsString(notification);

        assertTrue(json.contains("\"createdAt\":\"2026-10-23T15:30:00Z\""),
                "Expected ISO UTC instant with trailing 'Z', but was: " + json);
    }

    @Test
    @DisplayName("Appointment creation and retrieval preserves intended date and time strings")
    void testAppointmentPreservesIntendedDateTime() {
        String intendedDate = "2026-11-15";
        String intendedTime = "09:00 - 11:00";

        Appointment appointment = new Appointment();
        appointment.setDate(intendedDate);
        appointment.setTime(intendedTime);
        appointment.setPatientName("Sunil Silva");

        // Verify entity storage
        assertEquals("2026-11-15", appointment.getDate());
        assertEquals("09:00 - 11:00", appointment.getTime());

        // Verify DTO response mapping
        AppointmentResponseDTO dto = new AppointmentResponseDTO();
        dto.setDate(appointment.getDate());
        dto.setTime(appointment.getTime());
        dto.setPatientName(appointment.getPatientName());

        assertEquals("2026-11-15", dto.getDate());
        assertEquals("09:00 - 11:00", dto.getTime());
        assertEquals("Sunil Silva", dto.getPatientName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"UTC", "America/New_York", "Europe/London", "Asia/Colombo", "Australia/Sydney"})
    @DisplayName("Schedule expiration check gives consistent results across different JVM time zones")
    void testScheduleExpirationAcrossTimeZones(String timeZoneId) {
        TimeZone.setDefault(TimeZone.getTimeZone(timeZoneId));

        // An appointment scheduled for 2 days ahead in Sri Lanka should NEVER be past, regardless of server timezone
        ZonedDateTime slNow = ZonedDateTime.now(SRI_LANKA_ZONE);
        LocalDate futureDate = slNow.toLocalDate().plusDays(2);
        assertFalse(ScheduleService.isSchedulePast(futureDate.toString(), "12:00"),
                "Future schedule in Sri Lanka should not be past in JVM timezone " + timeZoneId);

        // A schedule from 2 days ago in Sri Lanka should ALWAYS be past
        LocalDate pastDate = slNow.toLocalDate().minusDays(2);
        assertTrue(ScheduleService.isSchedulePast(pastDate.toString(), "12:00"),
                "Past schedule in Sri Lanka should be past in JVM timezone " + timeZoneId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"UTC", "America/New_York", "Europe/London", "Asia/Colombo", "Australia/Sydney"})
    @DisplayName("Schedule expiration evaluated relative to Sri Lanka produces identical boolean across JVM timezones")
    void testScheduleExpirationRelativeToSriLanka(String timeZoneId) {
        TimeZone.setDefault(TimeZone.getTimeZone(timeZoneId));

        ZonedDateTime slNow = ZonedDateTime.now(SRI_LANKA_ZONE);

        // Schedule that occurred yesterday in Sri Lanka
        String yesterdayStr = slNow.toLocalDate().minusDays(1).toString();
        assertTrue(ScheduleService.isSchedulePast(yesterdayStr, "10:00"),
                "Yesterday's schedule must be past under JVM timezone " + timeZoneId);

        // Schedule that occurs tomorrow in Sri Lanka
        String tomorrowStr = slNow.toLocalDate().plusDays(1).toString();
        assertFalse(ScheduleService.isSchedulePast(tomorrowStr, "10:00"),
                "Tomorrow's schedule must NOT be past under JVM timezone " + timeZoneId);
    }

    @Test
    @DisplayName("Date-only appointment string does not shift when converted to LocalDate")
    void testDateOnlyDoesNotShift() {
        String appointmentDate = "2026-10-25";
        LocalDate parsed = LocalDate.parse(appointmentDate);

        assertEquals(2026, parsed.getYear());
        assertEquals(10, parsed.getMonthValue());
        assertEquals(25, parsed.getDayOfMonth());
        assertEquals("2026-10-25", parsed.toString());
    }
}
