package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Value("${spring.mail.host:${MAIL_HOST:smtp-relay.brevo.com}}")
    private String host;

    @Value("${spring.mail.port:${MAIL_PORT:587}}")
    private int port;

    @Value("${spring.mail.username:${MAIL_USERNAME:}}")
    private String username;

    @Value("${spring.mail.password:${MAIL_PASSWORD:}}")
    private String password;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        String resolvedHost = (host != null && !host.isBlank()) ? host.trim() : "smtp-relay.brevo.com";
        int resolvedPort = (port > 0) ? port : 587;

        mailSender.setHost(resolvedHost);
        mailSender.setPort(resolvedPort);

        // Sanitize username and password (removes accidentally copied spaces, especially common with Google App Passwords)
        String cleanUser = (username != null) ? username.trim() : "";
        String cleanPass = (password != null) ? password.replace(" ", "").trim() : "";

        mailSender.setUsername(cleanUser);
        mailSender.setPassword(cleanPass);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");

        // Dynamic support for port 465 (SSL) or port 587 / 2525 (STARTTLS)
        if (resolvedPort == 465) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }

        // Trust certificate & standard TLS protocols (Crucial for Docker containers & Render hosting)
        props.put("mail.smtp.ssl.trust", "*");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");

        // Timeouts to prevent hanging requests
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        // Enable debug in logs so you can inspect SMTP negotiation directly in Render logs
        props.put("mail.debug", "true");

        return mailSender;
    }
}