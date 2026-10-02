package com.example.demo.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // Account verification
    public void sendOtp(String to, String otp) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("Verify your account");
        msg.setText("Your verification code is: " + otp + "\nExpires in 5 minutes.");
        mailSender.send(msg);
    }

    // NEW → Password reset email
    public void sendPasswordResetEmail(String to, String token) {

        String resetLink = "https://cdcm-frontend.vercel.app/reset-password/" + token;

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("Password Reset Request");
        msg.setText(
                "Click the link below to reset your password:\n\n" +
                        resetLink +
                        "\n\nThis link expires in 1 hour."
        );

        mailSender.send(msg);
    }

    public void sendHospitalWelcomeEmail(
            String to,
            String hospitalName,
            String temporaryPassword) {

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setTo(to);

        message.setSubject(
                "Your CDCM Hospital Account"
        );

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

        mailSender.send(message);
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

        SimpleMailMessage message = new SimpleMailMessage();

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

        mailSender.send(message);
    }

    // Medical history temporary access OTP email
    public void sendMedicalHistoryAccessOtp(
            String to,
            String patientName,
            String doctorName,
            String appointmentNumber,
            String otp) {

        SimpleMailMessage message = new SimpleMailMessage();
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

        mailSender.send(message);
    }
}