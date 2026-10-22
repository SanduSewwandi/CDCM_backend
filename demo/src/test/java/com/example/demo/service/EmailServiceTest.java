package com.example.demo.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "mailFrom", "hirunireshasewwandi@gmail.com");
        ReflectionTestUtils.setField(emailService, "mailHost", "smtp-relay.brevo.com");
        ReflectionTestUtils.setField(emailService, "mailPort", 587);

        lenient().when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    void testSendEmail_Success() {
        assertDoesNotThrow(() ->
            emailService.sendEmail("recipient@example.com", "Test Subject", "Test Text", "<h1>Test</h1>")
        );

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendEmail_PlainTextOnly_Success() {
        assertDoesNotThrow(() ->
            emailService.sendEmail("recipient@example.com", "Plain Subject", "Plain text message")
        );

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendEmail_EmptyRecipient_ThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            emailService.sendEmail("", "Subject", "Text")
        );
        assertEquals("Recipient email address must not be empty.", ex.getMessage());
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testSendEmail_NullRecipient_ThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            emailService.sendEmail(null, "Subject", "Text")
        );
        assertEquals("Recipient email address must not be empty.", ex.getMessage());
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testSendEmail_MailSenderThrows_PropagatesException() {
        doThrow(new MailSendException("SMTP connection error"))
                .when(mailSender).send(any(MimeMessage.class));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            emailService.sendEmail("recipient@example.com", "Subject", "Text")
        );

        assertTrue(ex.getMessage().contains("Failed to send email via Brevo SMTP"));
        assertTrue(ex.getMessage().contains("recipient@example.com"));
    }

    @Test
    void testSendOtp_Success() {
        assertDoesNotThrow(() ->
            emailService.sendOtp("user@example.com", "654321")
        );

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendPasswordResetEmail_Success() {
        assertDoesNotThrow(() ->
            emailService.sendPasswordResetEmail("user@example.com", "token-abc-123")
        );

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendHospitalWelcomeEmail_Success() {
        assertDoesNotThrow(() ->
            emailService.sendHospitalWelcomeEmail("hospital@example.com", "General Hospital", "TempPass123!")
        );

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendAppointmentCancellationEmail_Success() {
        assertDoesNotThrow(() ->
            emailService.sendAppointmentCancellationEmail(
                    "patient@example.com",
                    "John Doe",
                    "Dr. Smith",
                    "Cardiology",
                    "2026-11-01",
                    "10:00 AM",
                    "City Hospital",
                    true
            )
        );

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendMedicalHistoryAccessOtp_Success() {
        assertDoesNotThrow(() ->
            emailService.sendMedicalHistoryAccessOtp(
                    "patient@example.com",
                    "Jane Doe",
                    "Dr. Watson",
                    "APT-1001",
                    "123456"
            )
        );

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}
