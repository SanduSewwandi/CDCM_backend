package com.example.demo.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:${MAIL_FROM:hirunireshasewwandi@gmail.com}}")
    private String mailFrom;

    @Value("${spring.mail.host:${MAIL_HOST:smtp-relay.brevo.com}}")
    private String mailHost;

    @Value("${spring.mail.port:${MAIL_PORT:587}}")
    private int mailPort;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Resolves the configured sender email address.
     * Configurable through MAIL_FROM (must be a verified sender in Brevo).
     */
    public String getSenderEmail() {
        if (mailFrom != null && !mailFrom.isBlank()) {
            return mailFrom.trim();
        }
        return "hirunireshasewwandi@gmail.com";
    }

    /**
     * Sends email exclusively using Brevo SMTP.
     * Every sending attempt must either succeed or throw a meaningful exception.
     * Never silently swallows errors or switches providers.
     */
    public void sendEmail(String to, String subject, String text, String html) {
        if (to == null || to.isBlank()) {
            log.error("Cannot send email: recipient address is empty.");
            throw new IllegalArgumentException("Recipient email address must not be empty.");
        }

        if (mailSender == null) {
            log.error("Cannot send email: JavaMailSender bean is not configured.");
            throw new IllegalStateException("Email sending failed: JavaMailSender is not configured.");
        }

        String trimmedTo = to.trim();
        String sender = getSenderEmail();

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());

            if (sender.contains("<") && sender.contains(">")) {
                helper.setFrom(sender);
            } else {
                helper.setFrom(sender, "CDCM Healthcare System");
            }

            helper.setTo(trimmedTo);
            helper.setSubject(subject);

            if (html != null && !html.isBlank()) {
                if (text != null && !text.isBlank()) {
                    helper.setText(text, html);
                } else {
                    helper.setText(html, true);
                }
            } else {
                helper.setText(text != null ? text : "", false);
            }

            log.info("Sending email via Brevo SMTP ({}:{}) to recipient [{}] with subject [{}]",
                    mailHost, mailPort, trimmedTo, subject);

            mailSender.send(mimeMessage);

            // Log acceptance by SMTP server. Note: SMTP acceptance confirms delivery to relay,
            // but final inbox placement depends on recipient mail provider filters.
            log.info("Brevo SMTP successfully accepted email for recipient [{}] with subject [{}]",
                    trimmedTo, subject);

        } catch (Exception e) {
            log.error("Failed to send email via Brevo SMTP to recipient [{}] with subject [{}]: {}",
                    trimmedTo, subject, e.getMessage());
            throw new RuntimeException("Failed to send email via Brevo SMTP to " + trimmedTo + ": " + e.getMessage(), e);
        }
    }

    public void sendEmail(String to, String subject, String text) {
        sendEmail(to, subject, text, null);
    }

    private String escapeHtml(String input) {
        if (input == null)
            return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    // Account verification
    public void sendOtp(String to, String otp) {
        try {
            String subject = "Verify your account";
            String text = "Your verification code is: " + otp + "\nExpires in 5 minutes.";
            String html = "<div style=\"font-family: Arial, sans-serif; max-width: 500px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 8px; background-color: #ffffff;\">"
                    + "<h2 style=\"color: #0f172a; margin-top: 0;\">Verify Your Account</h2>"
                    + "<p style=\"font-size: 15px; color: #475569;\">Use the verification code below to verify your CDCM account:</p>"
                    + "<div style=\"background: #f1f5f9; padding: 16px; border-radius: 6px; text-align: center; margin: 20px 0;\">"
                    + "<span style=\"font-size: 32px; font-weight: bold; letter-spacing: 6px; color: #0284c7;\">"
                    + escapeHtml(otp) + "</span>"
                    + "</div>"
                    + "<p style=\"font-size: 13px; color: #64748b;\">This code expires in 5 minutes. If you did not request this code, please ignore this email.</p>"
                    + "<hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;\" />"
                    + "<p style=\"font-size: 12px; color: #94a3b8; margin: 0;\">CDCM Healthcare System</p>"
                    + "</div>";

            sendEmail(to, subject, text, html);
        } catch (Exception e) {
            log.error("Failed to send OTP verification email to [{}]: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send verification email: " + e.getMessage(), e);
        }
    }

    // Password reset email
    public void sendPasswordResetEmail(String to, String token) {
        try {
            String resetLink = "https://cdcm-frontend.vercel.app/reset-password/" + token;
            String subject = "Password Reset Request";
            String text = "Click the link below to reset your password:\n\n" +
                    resetLink +
                    "\n\nThis link expires in 1 hour.";
            String html = "<div style=\"font-family: Arial, sans-serif; max-width: 500px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 8px; background-color: #ffffff;\">"
                    + "<h2 style=\"color: #0f172a; margin-top: 0;\">Password Reset Request</h2>"
                    + "<p style=\"font-size: 15px; color: #475569;\">We received a request to reset your CDCM account password.</p>"
                    + "<div style=\"text-align: center; margin: 24px 0;\">"
                    + "<a href=\"" + resetLink
                    + "\" style=\"background-color: #0284c7; color: #ffffff; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;\">Reset Password</a>"
                    + "</div>"
                    + "<p style=\"font-size: 13px; color: #64748b;\">If the button doesn't work, copy and paste this link into your browser:<br/><a href=\""
                    + resetLink + "\" style=\"color: #0284c7;\">" + resetLink + "</a></p>"
                    + "<p style=\"font-size: 13px; color: #64748b;\">This link expires in 1 hour. If you didn't request a password reset, you can safely ignore this email.</p>"
                    + "<hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;\" />"
                    + "<p style=\"font-size: 12px; color: #94a3b8; margin: 0;\">CDCM Healthcare System</p>"
                    + "</div>";

            sendEmail(to, subject, text, html);
        } catch (Exception e) {
            log.error("Failed to send password reset email to [{}]: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send password reset email: " + e.getMessage(), e);
        }
    }

    public void sendHospitalWelcomeEmail(
            String to,
            String hospitalName,
            String temporaryPassword) {

        try {
            String subject = "Your CDCM Hospital Account";
            String text = "Hello " + hospitalName + ",\n\n" +
                    "Your hospital account has been created.\n\n" +
                    "Login email: " + to + "\n" +
                    "Temporary password: " + temporaryPassword + "\n\n" +
                    "Please log in to the CDCM system.\n" +
                    "After login, request a verification code.\n" +
                    "You must verify your email and create a new password " +
                    "before accessing the hospital dashboard.";
            String html = "<div style=\"font-family: Arial, sans-serif; max-width: 550px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 8px; background-color: #ffffff;\">"
                    + "<h2 style=\"color: #0f172a; margin-top: 0;\">Welcome to CDCM</h2>"
                    + "<p style=\"font-size: 15px; color: #475569;\">Hello <strong>" + escapeHtml(hospitalName)
                    + "</strong>,</p>"
                    + "<p style=\"font-size: 15px; color: #475569;\">Your hospital account has been successfully created. Here are your initial login credentials:</p>"
                    + "<div style=\"background: #f8fafc; border: 1px solid #e2e8f0; padding: 16px; border-radius: 6px; margin: 20px 0;\">"
                    + "<p style=\"margin: 4px 0; color: #334155;\"><strong>Login Email:</strong> " + escapeHtml(to)
                    + "</p>"
                    + "<p style=\"margin: 4px 0; color: #334155;\"><strong>Temporary Password:</strong> <code style=\"background: #e2e8f0; padding: 2px 6px; border-radius: 4px;\">"
                    + escapeHtml(temporaryPassword) + "</code></p>"
                    + "</div>"
                    + "<p style=\"font-size: 14px; color: #475569;\">Please log in to the CDCM system. After login, you will be prompted to verify your email and set a new password before accessing the hospital dashboard.</p>"
                    + "<hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;\" />"
                    + "<p style=\"font-size: 12px; color: #94a3b8; margin: 0;\">CDCM Healthcare System</p>"
                    + "</div>";

            sendEmail(to, subject, text, html);
        } catch (Exception e) {
            log.error("Failed to send hospital welcome email to [{}]: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send welcome email: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------
    // DISTINCT APPOINTMENT & SCHEDULE CANCELLATION EMAILS
    // -------------------------------------------------------------

    public void sendPatientCancellationEmail(
            String to,
            String patientName,
            String doctorName,
            String appointmentNumber,
            String appointmentType,
            String date,
            String time,
            String hospitalName,
            String reason,
            double originalAmount,
            double refundPercentage,
            double refundAmount,
            boolean isRefundEligible) {

        try {
            String subject = isRefundEligible
                    ? "Appointment Cancelled - Refund Action Required"
                    : "Appointment Cancelled";

            String greeting = "Dear " + (patientName != null && !patientName.isBlank() ? patientName : "Patient") + ",\n\n";
            String apptNumStr = appointmentNumber != null ? appointmentNumber : "—";
            String reasonStr = (reason != null && !reason.isBlank()) ? reason : "Cancelled by patient";

            StringBuilder text = new StringBuilder();
            text.append(greeting);
            text.append("Your appointment #").append(apptNumStr).append(" has been cancelled.\n\n");
            text.append("Details:\n");
            text.append("Doctor: ").append(doctorName).append("\n");
            text.append("Hospital: ").append(hospitalName).append("\n");
            text.append("Date: ").append(date).append("\n");
            text.append("Time: ").append(time).append("\n");
            text.append("Reason: ").append(reasonStr).append("\n\n");

            if (isRefundEligible && refundAmount > 0) {
                text.append("Refund Details:\n");
                text.append("Original Payment: LKR ").append(String.format(java.util.Locale.US, "%.2f", originalAmount)).append("\n");
                text.append("Applicable Refund: ").append(refundPercentage).append("%\n");
                text.append("Refund Amount Due: LKR ").append(String.format(java.util.Locale.US, "%.2f", refundAmount)).append("\n\n");
                text.append("Action Required:\n");
                text.append("Please log in to the CDCM Patient Portal (My Appointments -> Previous tab) to provide your bank account details securely. ");
                text.append("Once submitted, the hospital finance team will execute the bank transfer manually.\n\n");
                text.append("Note: Refunds are processed manually by the hospital and are not completed merely because the appointment is cancelled.\n\n");
            } else {
                text.append("According to the cancellation refund policy, no refund is applicable for this cancellation.\n\n");
            }
            text.append("Thank you,\nCDCM System");

            String htmlRefundSection = "";
            if (isRefundEligible && refundAmount > 0) {
                htmlRefundSection = "<div style=\"background: #ecfdf5; border: 1px solid #a7f3d0; padding: 16px; border-radius: 8px; margin: 20px 0;\">"
                        + "<h3 style=\"color: #065f46; margin-top: 0; font-size: 15px;\">Refund Information</h3>"
                        + "<p style=\"margin: 4px 0; color: #047857;\"><strong>Original Payment:</strong> LKR " + String.format(java.util.Locale.US, "%.2f", originalAmount) + "</p>"
                        + "<p style=\"margin: 4px 0; color: #047857;\"><strong>Refund Percentage:</strong> " + refundPercentage + "%</p>"
                        + "<p style=\"margin: 4px 0; color: #047857;\"><strong>Refund Amount Due:</strong> LKR " + String.format(java.util.Locale.US, "%.2f", refundAmount) + "</p>"
                        + "<div style=\"margin-top: 12px; padding: 10px; background: #ffffff; border-radius: 6px; border: 1px dashed #6ee7b7;\">"
                        + "<p style=\"margin: 0; font-size: 13px; color: #065f46;\"><strong>Action Required:</strong> Please sign in to the CDCM Patient Portal, open <em>My Appointments</em>, and submit your bank account details under the <em>Previous</em> tab. The hospital will disburse your refund manually.</p>"
                        + "</div>"
                        + "<p style=\"font-size: 12px; color: #047857; margin-top: 8px; margin-bottom: 0;\">Note: Refunds are processed manually by hospital administration and are not instant.</p>"
                        + "</div>";
            } else {
                htmlRefundSection = "<div style=\"background: #f8fafc; border: 1px solid #e2e8f0; padding: 14px; border-radius: 6px; margin: 16px 0;\">"
                        + "<p style=\"margin: 0; font-size: 13px; color: #64748b;\">Under the applicable hospital cancellation policy, no refund is due for this cancellation.</p>"
                        + "</div>";
            }

            String html = "<div style=\"font-family: Arial, sans-serif; max-width: 550px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 8px; background-color: #ffffff;\">"
                    + "<h2 style=\"color: #dc2626; margin-top: 0;\">Appointment Cancelled</h2>"
                    + "<p style=\"font-size: 15px; color: #475569;\">Dear <strong>" + escapeHtml(patientName) + "</strong>,</p>"
                    + "<p style=\"font-size: 14px; color: #475569;\">Your appointment has been cancelled as requested.</p>"
                    + "<div style=\"background: #f8fafc; border: 1px solid #e2e8f0; padding: 16px; border-radius: 6px; margin: 16px 0;\">"
                    + "<p style=\"margin: 4px 0; color: #334155;\"><strong>Appointment Number:</strong> " + escapeHtml(apptNumStr) + "</p>"
                    + "<p style=\"margin: 4px 0; color: #334155;\"><strong>Doctor:</strong> " + escapeHtml(doctorName) + "</p>"
                    + "<p style=\"margin: 4px 0; color: #334155;\"><strong>Hospital:</strong> " + escapeHtml(hospitalName) + "</p>"
                    + "<p style=\"margin: 4px 0; color: #334155;\"><strong>Date & Time:</strong> " + escapeHtml(date) + " at " + escapeHtml(time) + "</p>"
                    + "<p style=\"margin: 4px 0; color: #334155;\"><strong>Reason:</strong> " + escapeHtml(reasonStr) + "</p>"
                    + "</div>"
                    + htmlRefundSection
                    + "<hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;\" />"
                    + "<p style=\"font-size: 12px; color: #94a3b8; margin: 0;\">CDCM Healthcare System</p>"
                    + "</div>";

            sendEmail(to, subject, text.toString(), html);
        } catch (Exception e) {
            log.error("Failed to send patient cancellation email to [{}]: {}", to, e.getMessage());
        }
    }

    public void sendDoctorScheduleCancellationEmail(
            String to,
            String patientName,
            String doctorName,
            String appointmentNumber,
            String appointmentType,
            String date,
            String time,
            String hospitalName,
            String reason,
            double originalAmount,
            boolean isPaid) {

        try {
            String subject = "Shift Cancelled by Doctor - 100% Refund Information";
            String greeting = "Dear " + (patientName != null && !patientName.isBlank() ? patientName : "Patient") + ",\n\n";
            String apptNumStr = appointmentNumber != null ? appointmentNumber : "—";
            String reasonStr = (reason != null && !reason.isBlank()) ? reason : "Doctor unavailable";

            StringBuilder text = new StringBuilder();
            text.append(greeting);
            text.append("We regret to inform you that Dr. ").append(doctorName)
                    .append(" has cancelled their scheduled shift on ").append(date)
                    .append(" at ").append(hospitalName).append(".\n\n");
            text.append("Reason: ").append(reasonStr).append("\n\n");

            if (isPaid) {
                text.append("Because this cancellation was initiated by the doctor, you are eligible for a 100% FULL REFUND of LKR ")
                        .append(String.format(java.util.Locale.US, "%.2f", originalAmount)).append(".\n\n");
                text.append("Action Required:\n");
                text.append("Please log in to your CDCM Patient Portal (My Appointments -> Previous tab) to securely submit your bank account details. ");
                text.append("The hospital will execute your refund directly via manual bank transfer.\n\n");
                text.append("Refunds are processed manually and are not instant upon cancellation.\n\n");
            } else {
                text.append("No payment had been charged for this appointment.\n\n");
            }
            text.append("We apologize for the inconvenience.\n\nThank you,\nCDCM System");

            String htmlRefundSection = "";
            if (isPaid) {
                htmlRefundSection = "<div style=\"background: #ecfdf5; border: 1px solid #a7f3d0; padding: 16px; border-radius: 8px; margin: 20px 0;\">"
                        + "<h3 style=\"color: #065f46; margin-top: 0; font-size: 15px;\">100% Full Refund Eligible</h3>"
                        + "<p style=\"margin: 4px 0; color: #047857;\"><strong>Refund Amount:</strong> LKR " + String.format(java.util.Locale.US, "%.2f", originalAmount) + " (100%)</p>"
                        + "<div style=\"margin-top: 12px; padding: 10px; background: #ffffff; border-radius: 6px; border: 1px dashed #6ee7b7;\">"
                        + "<p style=\"margin: 0; font-size: 13px; color: #065f46;\"><strong>Action Required:</strong> Please sign in to the CDCM Patient Portal, open <em>My Appointments</em>, and provide your bank account details under the <em>Previous</em> tab so the hospital finance team can process your refund.</p>"
                        + "</div>"
                        + "<p style=\"font-size: 12px; color: #047857; margin-top: 8px; margin-bottom: 0;\">Note: Refunds are processed manually by the hospital finance team.</p>"
                        + "</div>";
            }

            String html = "<div style=\"font-family: Arial, sans-serif; max-width: 550px; margin: 0 auto; padding: 24px; border: 1px solid #fecaca; border-radius: 8px; background-color: #ffffff;\">"
                    + "<h2 style=\"color: #dc2626; margin-top: 0;\">Schedule Cancelled by Doctor</h2>"
                    + "<p style=\"font-size: 15px; color: #475569;\">Dear <strong>" + escapeHtml(patientName) + "</strong>,</p>"
                    + "<p style=\"font-size: 14px; color: #475569;\">We regret to inform you that <strong>Dr. " + escapeHtml(doctorName) + "</strong> has cancelled their scheduled shift on <strong>" + escapeHtml(date) + "</strong> at <strong>" + escapeHtml(hospitalName) + "</strong>.</p>"
                    + "<div style=\"background: #f8fafc; border: 1px solid #e2e8f0; padding: 16px; border-radius: 6px; margin: 16px 0;\">"
                    + "<p style=\"margin: 4px 0; color: #334155;\"><strong>Appointment:</strong> #" + escapeHtml(apptNumStr) + "</p>"
                    + "<p style=\"margin: 4px 0; color: #334155;\"><strong>Time:</strong> " + escapeHtml(time) + "</p>"
                    + "<p style=\"margin: 4px 0; color: #334155;\"><strong>Reason:</strong> " + escapeHtml(reasonStr) + "</p>"
                    + "</div>"
                    + htmlRefundSection
                    + "<hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;\" />"
                    + "<p style=\"font-size: 12px; color: #94a3b8; margin: 0;\">CDCM Healthcare System</p>"
                    + "</div>";

            sendEmail(to, subject, text.toString(), html);
        } catch (Exception e) {
            log.error("Failed to send doctor schedule cancellation email to [{}]: {}", to, e.getMessage());
        }
    }

    public void sendHospitalScheduleCancellationEmail(
            String to,
            String patientName,
            String doctorName,
            String appointmentNumber,
            String appointmentType,
            String date,
            String time,
            String hospitalName,
            String reason,
            double originalAmount,
            boolean isPaid) {

        try {
            String subject = "Schedule Cancelled by Hospital - 100% Refund Information";
            String greeting = "Dear " + (patientName != null && !patientName.isBlank() ? patientName : "Patient") + ",\n\n";
            String apptNumStr = appointmentNumber != null ? appointmentNumber : "—";
            String reasonStr = (reason != null && !reason.isBlank()) ? reason : "Hospital operational requirements";

            StringBuilder text = new StringBuilder();
            text.append(greeting);
            text.append("We regret to inform you that ").append(hospitalName)
                    .append(" has cancelled the scheduled shift for Dr. ").append(doctorName)
                    .append(" on ").append(date).append(".\n\n");
            text.append("Reason: ").append(reasonStr).append("\n\n");

            if (isPaid) {
                text.append("Because this cancellation was initiated by the hospital, you are eligible for a 100% FULL REFUND of LKR ")
                        .append(String.format(java.util.Locale.US, "%.2f", originalAmount)).append(".\n\n");
                text.append("Action Required:\n");
                text.append("Please log in to your CDCM Patient Portal (My Appointments -> Previous tab) to securely submit your bank account details. ");
                text.append("The hospital will execute your refund directly via manual bank transfer.\n\n");
                text.append("Refunds are processed manually and are not instant upon cancellation.\n\n");
            } else {
                text.append("No payment had been charged for this appointment.\n\n");
            }
            text.append("We apologize for the inconvenience.\n\nThank you,\nCDCM System");

            String htmlRefundSection = "";
            if (isPaid) {
                htmlRefundSection = "<div style=\"background: #ecfdf5; border: 1px solid #a7f3d0; padding: 16px; border-radius: 8px; margin: 20px 0;\">"
                        + "<h3 style=\"color: #065f46; margin-top: 0; font-size: 15px;\">100% Full Refund Eligible</h3>"
                        + "<p style=\"margin: 4px 0; color: #047857;\"><strong>Refund Amount:</strong> LKR " + String.format(java.util.Locale.US, "%.2f", originalAmount) + " (100%)</p>"
                        + "<div style=\"margin-top: 12px; padding: 10px; background: #ffffff; border-radius: 6px; border: 1px dashed #6ee7b7;\">"
                        + "<p style=\"margin: 0; font-size: 13px; color: #065f46;\"><strong>Action Required:</strong> Please sign in to the CDCM Patient Portal, open <em>My Appointments</em>, and provide your bank account details under the <em>Previous</em> tab so the hospital finance team can process your refund.</p>"
                        + "</div>"
                        + "<p style=\"font-size: 12px; color: #047857; margin-top: 8px; margin-bottom: 0;\">Note: Refunds are processed manually by the hospital finance team.</p>"
                        + "</div>";
            }

            String html = "<div style=\"font-family: Arial, sans-serif; max-width: 550px; margin: 0 auto; padding: 24px; border: 1px solid #fecaca; border-radius: 8px; background-color: #ffffff;\">"
                    + "<h2 style=\"color: #dc2626; margin-top: 0;\">Schedule Cancelled by Hospital</h2>"
                    + "<p style=\"font-size: 15px; color: #475569;\">Dear <strong>" + escapeHtml(patientName) + "</strong>,</p>"
                    + "<p style=\"font-size: 14px; color: #475569;\">We regret to inform you that <strong>" + escapeHtml(hospitalName) + "</strong> has cancelled the scheduled shift for <strong>Dr. " + escapeHtml(doctorName) + "</strong> on <strong>" + escapeHtml(date) + "</strong>.</p>"
                    + "<div style=\"background: #f8fafc; border: 1px solid #e2e8f0; padding: 16px; border-radius: 6px; margin: 16px 0;\">"
                    + "<p style=\"margin: 4px 0; color: #334155;\"><strong>Appointment:</strong> #" + escapeHtml(apptNumStr) + "</p>"
                    + "<p style=\"margin: 4px 0; color: #334155;\"><strong>Time:</strong> " + escapeHtml(time) + "</p>"
                    + "<p style=\"margin: 4px 0; color: #334155;\"><strong>Reason:</strong> " + escapeHtml(reasonStr) + "</p>"
                    + "</div>"
                    + htmlRefundSection
                    + "<hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;\" />"
                    + "<p style=\"font-size: 12px; color: #94a3b8; margin: 0;\">CDCM Healthcare System</p>"
                    + "</div>";

            sendEmail(to, subject, text.toString(), html);
        } catch (Exception e) {
            log.error("Failed to send hospital schedule cancellation email to [{}]: {}", to, e.getMessage());
        }
    }

    public void sendRefundCompletedEmail(
            String to,
            String patientName,
            String appointmentNumber,
            double refundAmount,
            String transactionRef,
            String hospitalName) {

        try {
            String subject = "Refund Processed Successfully - Appointment #" + (appointmentNumber != null ? appointmentNumber : "");
            String greeting = "Dear " + (patientName != null && !patientName.isBlank() ? patientName : "Patient") + ",\n\n";
            String refStr = (transactionRef != null && !transactionRef.isBlank()) ? transactionRef : "N/A";

            String text = greeting +
                    "Your refund for appointment #" + (appointmentNumber != null ? appointmentNumber : "") +
                    " has been successfully processed and transferred by " + hospitalName + ".\n\n" +
                    "Refund Amount: LKR " + String.format(java.util.Locale.US, "%.2f", refundAmount) + "\n" +
                    "Transaction Reference: " + refStr + "\n\n" +
                    "Please check your bank statement. If you have any inquiries, please contact " + hospitalName + ".\n\n" +
                    "Thank you,\nCDCM System";

            String html = "<div style=\"font-family: Arial, sans-serif; max-width: 550px; margin: 0 auto; padding: 24px; border: 1px solid #a7f3d0; border-radius: 8px; background-color: #ffffff;\">"
                    + "<h2 style=\"color: #059669; margin-top: 0;\">Refund Transfer Completed</h2>"
                    + "<p style=\"font-size: 15px; color: #475569;\">Dear <strong>" + escapeHtml(patientName) + "</strong>,</p>"
                    + "<p style=\"font-size: 14px; color: #475569;\">Your refund has been successfully completed and transferred to your bank account by <strong>" + escapeHtml(hospitalName) + "</strong>.</p>"
                    + "<div style=\"background: #ecfdf5; border: 1px solid #a7f3d0; padding: 16px; border-radius: 6px; margin: 16px 0;\">"
                    + "<p style=\"margin: 4px 0; color: #065f46;\"><strong>Appointment Number:</strong> " + escapeHtml(appointmentNumber) + "</p>"
                    + "<p style=\"margin: 4px 0; color: #065f46;\"><strong>Refund Amount:</strong> LKR " + String.format(java.util.Locale.US, "%.2f", refundAmount) + "</p>"
                    + "<p style=\"margin: 4px 0; color: #065f46;\"><strong>Transaction Reference:</strong> " + escapeHtml(refStr) + "</p>"
                    + "</div>"
                    + "<p style=\"font-size: 13px; color: #64748b;\">Please check your bank statement. Depending on your bank, it may take 1-2 business days to reflect.</p>"
                    + "<hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;\" />"
                    + "<p style=\"font-size: 12px; color: #94a3b8; margin: 0;\">CDCM Healthcare System</p>"
                    + "</div>";

            sendEmail(to, subject, text, html);
        } catch (Exception e) {
            log.error("Failed to send refund completion email to [{}]: {}", to, e.getMessage());
        }
    }

    // Appointment cancellation email (legacy / fallback)
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
            sendDoctorScheduleCancellationEmail(to, patientName, doctorName, null, appointmentType, date, time, hospitalName, "Cancelled", 0.0, isPaid);
        } catch (Exception e) {
            log.error("Failed to send appointment cancellation email to [{}]: {}", to, e.getMessage());
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
            String apptNumStr = (appointmentNumber != null ? appointmentNumber : "");
            String subject = "Medical History Access Code - Appointment #" + apptNumStr;
            String greeting = (patientName != null && !patientName.isBlank()) ? "Dear " + patientName + ",\n\n"
                    : "Dear Patient,\n\n";
            String doctorSnippet = (doctorName != null && !doctorName.isBlank()) ? doctorName : "your attending doctor";

            String text = greeting +
                    "Your payment for appointment #" + apptNumStr + " has been successfully confirmed.\n\n" +
                    "To safeguard your privacy, your medical history is confidential and restricted. If you wish to allow "
                    + doctorSnippet
                    + " to view your medical history during your consultation, please share the following 6-digit access code:\n\n"
                    + "Access Code: " + otp + "\n\n"
                    + "IMPORTANT PRIVACY NOTICE:\n"
                    + "- Provide this code ONLY to your doctor during your consultation.\n"
                    + "- Once verified by your doctor, medical history access will remain active for exactly 1 hour.\n"
                    + "- After 1 hour, access will automatically expire.\n"
                    + "- If you do not wish to share your medical history, simply do not provide this code.\n\n"
                    + "Thank you,\n"
                    + "CDCM System";

            String html = "<div style=\"font-family: Arial, sans-serif; max-width: 550px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 8px; background-color: #ffffff;\">"
                    + "<h2 style=\"color: #0f172a; margin-top: 0;\">Medical History Access Code</h2>"
                    + "<p style=\"font-size: 15px; color: #475569;\">Dear <strong>"
                    + escapeHtml(patientName != null && !patientName.isBlank() ? patientName : "Patient")
                    + "</strong>,</p>"
                    + "<p style=\"font-size: 15px; color: #475569;\">Your payment for appointment <strong>#"
                    + escapeHtml(apptNumStr) + "</strong> has been successfully confirmed.</p>"
                    + "<div style=\"background: #f0fdf4; border: 1px solid #bbf7d0; padding: 20px; border-radius: 6px; text-align: center; margin: 20px 0;\">"
                    + "<p style=\"margin: 0 0 8px 0; font-size: 13px; color: #166534; text-transform: uppercase; font-weight: bold; letter-spacing: 1px;\">Temporary 6-Digit Access Code</p>"
                    + "<span style=\"font-size: 32px; font-weight: bold; letter-spacing: 6px; color: #15803d;\">"
                    + escapeHtml(otp) + "</span>"
                    + "</div>"
                    + "<div style=\"background: #f8fafc; border-left: 4px solid #0284c7; padding: 12px 16px; margin: 20px 0;\">"
                    + "<p style=\"margin: 0; font-size: 13px; color: #334155; font-weight: bold;\">IMPORTANT PRIVACY NOTICE:</p>"
                    + "<ul style=\"margin: 8px 0 0 0; padding-left: 20px; font-size: 13px; color: #475569;\">"
                    + "<li>Provide this code <strong>ONLY to " + escapeHtml(doctorSnippet)
                    + "</strong> during your consultation.</li>"
                    + "<li>Once verified, access will remain active for <strong>exactly 1 hour</strong>.</li>"
                    + "<li>After 1 hour, access automatically expires.</li>"
                    + "<li>If you do not wish to share your history, simply do not provide this code.</li>"
                    + "</ul>"
                    + "</div>"
                    + "<hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;\" />"
                    + "<p style=\"font-size: 12px; color: #94a3b8; margin: 0;\">CDCM Healthcare System</p>"
                    + "</div>";

            sendEmail(to, subject, text, html);
        } catch (Exception e) {
            log.error("Failed to send medical history access OTP to [{}]: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send medical history access code: " + e.getMessage(), e);
        }
    }
}