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

        String resetLink = "http://localhost:5173/reset-password/" + token;

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
            String temporaryPassword,
            String otp) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject("Your CDCM Hospital Account");

        message.setText(
                "Hello " + hospitalName + ",\n\n" +
                        "Your hospital account has been created.\n\n" +
                        "Login email: " + to + "\n" +
                        "Temporary password: " + temporaryPassword + "\n" +
                        "Verification code: " + otp + "\n\n" +
                        "This verification code expires in 5 minutes.\n" +
                        "After verification, you must create a new password."
        );

        mailSender.send(message);
    }
}