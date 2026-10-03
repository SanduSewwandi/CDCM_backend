package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:${MAIL_USERNAME:}}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    private String getSenderEmail() {
        if (fromEmail != null && !fromEmail.isBlank()) {
            return fromEmail.trim();
        }
        return "hiruireshasewwandi@gmail.com";
    }

    // Account verification
    public void sendOtp(String to, String otp) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(getSenderEmail());
            msg.setTo(to);
            msg.setSubject("Verify your account");
            msg.setText("Your verification code is: " + otp + "\nExpires in 5 minutes.");

            log.info("Sending OTP verification email from [{}] to [{}]", getSenderEmail(), to);
            mailSender.send(msg);
            log.info("Successfully sent OTP email to [{}]", to);
        } catch (Exception e) {
            log.error("Failed to send OTP verification email to [{}]: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send verification email: " + e.getMessage(), e);
        }
    }

    // Password reset email
    public void sendPasswordResetEmail(String to, String token) {
        try {
            String resetLink = "https://cdcm-frontend.vercel.app/reset-password/" + token;

            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(getSenderEmail());
            msg.setTo(to);
            msg.setSubject("Password Reset Request");
            msg.setText(
                    "Click the link below to reset your password:\n\n" +
                            resetLink +
                            "\n\nThis link expires in 1 hour."
            );

            log.info("Sending password reset email from [{}] to [{}]", getSenderEmail(), to);
            mailSender.send(msg);
            log.info("Successfully sent password reset email to [{}]", to);
        } catch (Exception e) {
            log.error("Failed to send password reset email to [{}]: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send password reset email: " + e.getMessage(), e);
        }
    }

    public void sendHospitalWelcomeEmail(
            String to,
            String hospitalName,
            String temporaryPassword) {

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(getSenderEmail());
            message.setTo(to);
            message.setSubject("Your CDCM Hospital Account");
            message.setText(
                    "Hello " + hospitalName + ",\n\n" +
                            "Your hospital account has been created.\n\n" +
                            "Login email: " + to + "\n" +
                            "Temporary password: " +
                            temporaryPassword + "\n\n" +
                            "Please log in to the CDCM system.\n" +
                            "After login, request a verification code.\n" +
                            "You must verify your email and create a new password " +
                            "before accessing the hospital dashboard."
            );

            log.info("Sending hospital welcome email from [{}] to [{}]", getSenderEmail(), to);
            mailSender.send(message);
            log.info("Successfully sent hospital welcome email to [{}]", to);
        } catch (Exception e) {
            log.error("Failed to send hospital welcome email to [{}]: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send welcome email: " + e.getMessage(), e);
        }
    }

    // Appointment cancellation email
    public void sendAppointmentCancellationEmail(
            String to,
            String patientName,
            String doctorName,
            String appointmentType,
            String date,
            String time,
            String hospitalName,
            boolean isPaid) {

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(getSenderEmail());
            message.setTo(to);

            if (isPaid) {
                message.setSubject("Appointment Cancelled - Payment Received");
                message.setText(
                        "Dear " + patientName + ",\n\n" +
                                "We regret to inform you that your " +
                                appointmentType +
                                " appointment with " +
                                doctorName +
                                " has been cancelled by " +
                                hospitalName +
                                ".\n\n" +
                                "Appointment Details:\n" +
                                "Doctor: " + doctorName + "\n" +
                                "Date: " + date + "\n" +
                                "Time: " + time + "\n" +
                                "Type: " + appointmentType + "\n\n" +
                                "Your payment has already been received for this appointment.\n" +
                                "Please contact " + hospitalName +
                                " regarding the refund process.\n\n" +
                                "We apologize for any inconvenience caused.\n\n" +
                                "Thank you,\n" +
                                "CDCM System"
                );
            } else {
                message.setSubject("Appointment Cancelled");
                message.setText(
                        "Dear " + patientName + ",\n\n" +
                                "We regret to inform you that your " +
                                appointmentType +
                                " appointment with " +
                                doctorName +
                                " has been cancelled by " +
                                hospitalName +
                                ".\n\n" +
                                "Appointment Details:\n" +
                                "Doctor: " + doctorName + "\n" +
                                "Date: " + date + "\n" +
                                "Time: " + time + "\n" +
                                "Type: " + appointmentType + "\n\n" +
                                "Please contact " + hospitalName +
                                " for more information.\n\n" +
                                "We apologize for any inconvenience caused.\n\n" +
                                "Thank you,\n" +
                                "CDCM System"
                );
            }

            log.info("Sending appointment cancellation email from [{}] to [{}]", getSenderEmail(), to);
            mailSender.send(message);
            log.info("Successfully sent appointment cancellation email to [{}]", to);
        } catch (Exception e) {
            log.error("Failed to send appointment cancellation email to [{}]: {}", to, e.getMessage(), e);
        }
    }

    // Medical history temporary access OTP email
    public void sendMedicalHistoryAccessOtp(
            String to,
            String patientName,
            String doctorName,
            String appointmentNumber,
            String otp) {

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(getSenderEmail());
            message.setTo(to);
            message.setSubject("Medical History Access Code - Appointment #" + (appointmentNumber != null ? appointmentNumber : ""));

            String greeting = (patientName != null && !patientName.isBlank()) ? "Dear " + patientName + ",\n\n" : "Dear Patient,\n\n";
            String doctorSnippet = (doctorName != null && !doctorName.isBlank()) ? doctorName : "your attending doctor";

            message.setText(
                    greeting +
                    "Your payment for appointment #" + (appointmentNumber != null ? appointmentNumber : "") + " has been successfully confirmed.\n\n" +
                    "To safeguard your privacy, your medical history is confidential and restricted. If you wish to allow " +
                    doctorSnippet + " to view your medical history during your consultation, please share the following 6-digit access code:\n\n" +
                    "Access Code: " + otp + "\n\n" +
                    "IMPORTANT PRIVACY NOTICE:\n" +
                    "- Provide this code ONLY to your doctor during your consultation.\n" +
                    "- Once verified by your doctor, medical history access will remain active for exactly 1 hour.\n" +
                    "- After 1 hour, access will automatically expire.\n" +
                    "- If you do not wish to share your medical history, simply do not provide this code.\n\n" +
                    "Thank you,\n" +
                    "CDCM System"
            );

            log.info("Sending medical history access OTP email from [{}] to [{}]", getSenderEmail(), to);
            mailSender.send(message);
            log.info("Successfully sent medical history access OTP email to [{}]", to);
        } catch (Exception e) {
            log.error("Failed to send medical history access OTP to [{}]: {}", to, e.getMessage(), e);
        }
    }
}