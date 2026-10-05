package com.example.demo.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${resend.api.key:${RESEND_API_KEY:}}")
    private String resendApiKey;

    @Value("${resend.from.email:${RESEND_FROM_EMAIL:}}")
    private String resendFromEmail;

    @Value("${spring.mail.username:${MAIL_USERNAME:}}")
    private String fallbackUsername;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Resolves the sender email address.
     * Uses Resend configured sender or defaults to 'CDCM System <onboarding@resend.dev>'.
     * Auto-detects public domains like @gmail.com which Resend forbids without verified domain.
     */
    public String getSenderEmail() {
        String from = (resendFromEmail != null && !resendFromEmail.isBlank())
                ? resendFromEmail.trim()
                : "";

        if (from.isEmpty() && fallbackUsername != null && !fallbackUsername.isBlank()) {
            from = fallbackUsername.trim();
        }

        if (from.isEmpty()) {
            return "CDCM System <onboarding@resend.dev>";
        }

        // Resend forbids sending from public domains (@gmail.com, @yahoo.com, etc.) without domain verification
        String lower = from.toLowerCase();
        if (lower.contains("@gmail.com") || lower.contains("@yahoo.com") || lower.contains("@outlook.com") || lower.contains("@hotmail.com")) {
            log.warn("Configured sender [{}] is a public email provider that Resend does not allow without a verified domain. "
                    + "Defaulting to 'CDCM System <onboarding@resend.dev>'. "
                    + "To use your custom domain, verify it at https://resend.com/domains and set RESEND_FROM_EMAIL.", from);
            return "CDCM System <onboarding@resend.dev>";
        }

        return from;
    }

    /**
     * Generic email sender using Resend REST API (No SMTP required).
     * Falls back to SMTP only if RESEND_API_KEY is not configured and mailSender is available.
     */
    public void sendEmail(String to, String subject, String text, String html) {
        if (to == null || to.isBlank()) {
            log.error("Cannot send email: recipient address is empty.");
            throw new IllegalArgumentException("Recipient email address must not be empty.");
        }

        String trimmedTo = to.trim();
        String apiKey = (resendApiKey != null) ? resendApiKey.trim() : "";

        // Use Resend REST API if key is present and not the placeholder
        if (!apiKey.isEmpty() && !apiKey.equalsIgnoreCase("your_resend_api_key")) {
            sendViaResend(trimmedTo, subject, text, html, apiKey);
            return;
        }

        // Fallback to SMTP if RESEND_API_KEY is missing/placeholder and SMTP is configured
        if (mailSender != null && fallbackUsername != null && !fallbackUsername.isBlank() && !fallbackUsername.equalsIgnoreCase("your_email")) {
            log.warn("RESEND_API_KEY is not configured or still set to placeholder ('{}'). Falling back to SMTP mailSender.", apiKey);
            sendViaSmtp(trimmedTo, subject, text);
            return;
        }

        String msg = "Email sending failed: RESEND_API_KEY is not configured or still set to 'your_resend_api_key'. "
                + "Please set a valid Resend API key (starting with 're_') in your .env or environment variables. "
                + "Get a free API key at https://resend.com";
        log.error(msg);
        throw new IllegalStateException(msg);
    }

    public void sendEmail(String to, String subject, String text) {
        sendEmail(to, subject, text, null);
    }

    private void sendViaResend(String to, String subject, String text, String html, String apiKey) {
        String sender = getSenderEmail();
        log.info("Sending email via Resend API (HTTPS REST) from [{}] to [{}] with subject [{}]", sender, to, subject);

        try {
            Resend resend = new Resend(apiKey);
            CreateEmailOptions.Builder optionsBuilder = CreateEmailOptions.builder()
                    .from(sender)
                    .to(to)
                    .subject(subject);

            if (text != null && !text.isBlank()) {
                optionsBuilder.text(text);
            }
            if (html != null && !html.isBlank()) {
                optionsBuilder.html(html);
            }

            CreateEmailResponse response = resend.emails().send(optionsBuilder.build());
            String emailId = (response != null) ? response.getId() : "unknown";
            log.info("Successfully sent email to [{}] via Resend API. Resend Email ID: [{}]", to, emailId);
        } catch (ResendException e) {
            log.error("Resend API error sending email to [{}]: statusCode={}, errorName={}, responseBody={}, message={}",
                    to, e.getStatusCode(), e.getErrorName(), e.getResponseBody(), e.getMessage(), e);
            throw new RuntimeException("Resend API error: " + e.getMessage() + (e.getResponseBody() != null ? " (" + e.getResponseBody() + ")" : ""), e);
        } catch (Exception e) {
            log.error("Failed to send email to [{}] via Resend API: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email via Resend: " + e.getMessage(), e);
        }
    }

    private void sendViaSmtp(String to, String subject, String text) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fallbackUsername != null && !fallbackUsername.isBlank() ? fallbackUsername.trim() : "noreply@cdcm.com");
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(text);

            log.info("Sending email via SMTP fallback to [{}] with subject [{}]", to, subject);
            mailSender.send(msg);
            log.info("Successfully sent email to [{}] via SMTP fallback", to);
        } catch (Exception e) {
            log.error("Failed to send email to [{}] via SMTP fallback: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email via SMTP: " + e.getMessage(), e);
        }
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
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
                    + "<span style=\"font-size: 32px; font-weight: bold; letter-spacing: 6px; color: #0284c7;\">" + escapeHtml(otp) + "</span>"
                    + "</div>"
                    + "<p style=\"font-size: 13px; color: #64748b;\">This code expires in 5 minutes. If you did not request this code, please ignore this email.</p>"
                    + "<hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;\" />"
                    + "<p style=\"font-size: 12px; color: #94a3b8; margin: 0;\">CDCM Healthcare System</p>"
                    + "</div>";

            sendEmail(to, subject, text, html);
        } catch (Exception e) {
            log.error("Failed to send OTP verification email to [{}]: {}", to, e.getMessage(), e);
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
                    + "<a href=\"" + resetLink + "\" style=\"background-color: #0284c7; color: #ffffff; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;\">Reset Password</a>"
                    + "</div>"
                    + "<p style=\"font-size: 13px; color: #64748b;\">If the button doesn't work, copy and paste this link into your browser:<br/><a href=\"" + resetLink + "\" style=\"color: #0284c7;\">" + resetLink + "</a></p>"
                    + "<p style=\"font-size: 13px; color: #64748b;\">This link expires in 1 hour. If you didn't request a password reset, you can safely ignore this email.</p>"
                    + "<hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;\" />"
                    + "<p style=\"font-size: 12px; color: #94a3b8; margin: 0;\">CDCM Healthcare System</p>"
                    + "</div>";

            sendEmail(to, subject, text, html);
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
                    + "<p style=\"font-size: 15px; color: #475569;\">Hello <strong>" + escapeHtml(hospitalName) + "</strong>,</p>"
                    + "<p style=\"font-size: 15px; color: #475569;\">Your hospital account has been successfully created. Here are your initial login credentials:</p>"
                    + "<div style=\"background: #f8fafc; border: 1px solid #e2e8f0; padding: 16px; border-radius: 6px; margin: 20px 0;\">"
                    + "<p style=\"margin: 4px 0; color: #334155;\"><strong>Login Email:</strong> " + escapeHtml(to) + "</p>"
                    + "<p style=\"margin: 4px 0; color: #334155;\"><strong>Temporary Password:</strong> <code style=\"background: #e2e8f0; padding: 2px 6px; border-radius: 4px;\">" + escapeHtml(temporaryPassword) + "</code></p>"
                    + "</div>"
                    + "<p style=\"font-size: 14px; color: #475569;\">Please log in to the CDCM system. After login, you will be prompted to verify your email and set a new password before accessing the hospital dashboard.</p>"
                    + "<hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;\" />"
                    + "<p style=\"font-size: 12px; color: #94a3b8; margin: 0;\">CDCM Healthcare System</p>"
                    + "</div>";

            sendEmail(to, subject, text, html);
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
            String subject;
            String text;
            String html;

            String greeting = "Dear " + (patientName != null && !patientName.isBlank() ? patientName : "Patient") + ",\n\n";
            String details = "Appointment Details:\n" +
                    "Doctor: " + doctorName + "\n" +
                    "Date: " + date + "\n" +
                    "Time: " + time + "\n" +
                    "Type: " + appointmentType + "\n\n";

            if (isPaid) {
                subject = "Appointment Cancelled - Payment Received";
                text = greeting +
                        "We regret to inform you that your " + appointmentType + " appointment with " +
                        doctorName + " has been cancelled by " + hospitalName + ".\n\n" +
                        details +
                        "Your payment has already been received for this appointment.\n" +
                        "Please contact " + hospitalName + " regarding the refund process.\n\n" +
                        "We apologize for any inconvenience caused.\n\n" +
                        "Thank you,\nCDCM System";

                html = "<div style=\"font-family: Arial, sans-serif; max-width: 550px; margin: 0 auto; padding: 24px; border: 1px solid #fecaca; border-radius: 8px; background-color: #ffffff;\">"
                        + "<h2 style=\"color: #dc2626; margin-top: 0;\">Appointment Cancelled</h2>"
                        + "<p style=\"font-size: 15px; color: #475569;\">Dear <strong>" + escapeHtml(patientName) + "</strong>,</p>"
                        + "<p style=\"font-size: 15px; color: #475569;\">We regret to inform you that your <strong>" + escapeHtml(appointmentType) + "</strong> appointment with <strong>" + escapeHtml(doctorName) + "</strong> has been cancelled by <strong>" + escapeHtml(hospitalName) + "</strong>.</p>"
                        + "<div style=\"background: #fef2f2; border: 1px solid #fee2e2; padding: 16px; border-radius: 6px; margin: 20px 0;\">"
                        + "<p style=\"margin: 4px 0;\"><strong>Doctor:</strong> " + escapeHtml(doctorName) + "</p>"
                        + "<p style=\"margin: 4px 0;\"><strong>Date:</strong> " + escapeHtml(date) + "</p>"
                        + "<p style=\"margin: 4px 0;\"><strong>Time:</strong> " + escapeHtml(time) + "</p>"
                        + "<p style=\"margin: 4px 0;\"><strong>Type:</strong> " + escapeHtml(appointmentType) + "</p>"
                        + "</div>"
                        + "<p style=\"font-size: 14px; color: #991b1b; font-weight: bold;\">Your payment has already been received for this appointment. Please contact " + escapeHtml(hospitalName) + " regarding the refund process.</p>"
                        + "<hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;\" />"
                        + "<p style=\"font-size: 12px; color: #94a3b8; margin: 0;\">CDCM Healthcare System</p>"
                        + "</div>";
            } else {
                subject = "Appointment Cancelled";
                text = greeting +
                        "We regret to inform you that your " + appointmentType + " appointment with " +
                        doctorName + " has been cancelled by " + hospitalName + ".\n\n" +
                        details +
                        "Please contact " + hospitalName + " for more information.\n\n" +
                        "We apologize for any inconvenience caused.\n\n" +
                        "Thank you,\nCDCM System";

                html = "<div style=\"font-family: Arial, sans-serif; max-width: 550px; margin: 0 auto; padding: 24px; border: 1px solid #fed7aa; border-radius: 8px; background-color: #ffffff;\">"
                        + "<h2 style=\"color: #ea580c; margin-top: 0;\">Appointment Cancelled</h2>"
                        + "<p style=\"font-size: 15px; color: #475569;\">Dear <strong>" + escapeHtml(patientName) + "</strong>,</p>"
                        + "<p style=\"font-size: 15px; color: #475569;\">We regret to inform you that your <strong>" + escapeHtml(appointmentType) + "</strong> appointment with <strong>" + escapeHtml(doctorName) + "</strong> has been cancelled by <strong>" + escapeHtml(hospitalName) + "</strong>.</p>"
                        + "<div style=\"background: #fff7ed; border: 1px solid #ffedd5; padding: 16px; border-radius: 6px; margin: 20px 0;\">"
                        + "<p style=\"margin: 4px 0;\"><strong>Doctor:</strong> " + escapeHtml(doctorName) + "</p>"
                        + "<p style=\"margin: 4px 0;\"><strong>Date:</strong> " + escapeHtml(date) + "</p>"
                        + "<p style=\"margin: 4px 0;\"><strong>Time:</strong> " + escapeHtml(time) + "</p>"
                        + "<p style=\"margin: 4px 0;\"><strong>Type:</strong> " + escapeHtml(appointmentType) + "</p>"
                        + "</div>"
                        + "<p style=\"font-size: 14px; color: #475569;\">Please contact " + escapeHtml(hospitalName) + " for more information.</p>"
                        + "<hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;\" />"
                        + "<p style=\"font-size: 12px; color: #94a3b8; margin: 0;\">CDCM Healthcare System</p>"
                        + "</div>";
            }

            sendEmail(to, subject, text, html);
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
            String apptNumStr = (appointmentNumber != null ? appointmentNumber : "");
            String subject = "Medical History Access Code - Appointment #" + apptNumStr;
            String greeting = (patientName != null && !patientName.isBlank()) ? "Dear " + patientName + ",\n\n" : "Dear Patient,\n\n";
            String doctorSnippet = (doctorName != null && !doctorName.isBlank()) ? doctorName : "your attending doctor";

            String text = greeting +
                    "Your payment for appointment #" + apptNumStr + " has been successfully confirmed.\n\n" +
                    "To safeguard your privacy, your medical history is confidential and restricted. If you wish to allow " +
                    doctorSnippet + " to view your medical history during your consultation, please share the following 6-digit access code:\n\n" +
                    "Access Code: " + otp + "\n\n" +
                    "IMPORTANT PRIVACY NOTICE:\n" +
                    "- Provide this code ONLY to your doctor during your consultation.\n" +
                    "- Once verified by your doctor, medical history access will remain active for exactly 1 hour.\n" +
                    "- After 1 hour, access will automatically expire.\n" +
                    "- If you do not wish to share your medical history, simply do not provide this code.\n\n" +
                    "Thank you,\n" +
                    "CDCM System";

            String html = "<div style=\"font-family: Arial, sans-serif; max-width: 550px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 8px; background-color: #ffffff;\">"
                    + "<h2 style=\"color: #0f172a; margin-top: 0;\">Medical History Access Code</h2>"
                    + "<p style=\"font-size: 15px; color: #475569;\">Dear <strong>" + escapeHtml(patientName != null && !patientName.isBlank() ? patientName : "Patient") + "</strong>,</p>"
                    + "<p style=\"font-size: 15px; color: #475569;\">Your payment for appointment <strong>#" + escapeHtml(apptNumStr) + "</strong> has been successfully confirmed.</p>"
                    + "<div style=\"background: #f0fdf4; border: 1px solid #bbf7d0; padding: 20px; border-radius: 6px; text-align: center; margin: 20px 0;\">"
                    + "<p style=\"margin: 0 0 8px 0; font-size: 13px; color: #166534; text-transform: uppercase; font-weight: bold; letter-spacing: 1px;\">Temporary 6-Digit Access Code</p>"
                    + "<span style=\"font-size: 32px; font-weight: bold; letter-spacing: 6px; color: #15803d;\">" + escapeHtml(otp) + "</span>"
                    + "</div>"
                    + "<div style=\"background: #f8fafc; border-left: 4px solid #0284c7; padding: 12px 16px; margin: 20px 0;\">"
                    + "<p style=\"margin: 0; font-size: 13px; color: #334155; font-weight: bold;\">IMPORTANT PRIVACY NOTICE:</p>"
                    + "<ul style=\"margin: 8px 0 0 0; padding-left: 20px; font-size: 13px; color: #475569;\">"
                    + "<li>Provide this code <strong>ONLY to " + escapeHtml(doctorSnippet) + "</strong> during your consultation.</li>"
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
            log.error("Failed to send medical history access OTP to [{}]: {}", to, e.getMessage(), e);
        }
    }
}