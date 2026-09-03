package com.example.demo.service;

import com.example.demo.dto.PaymentNotificationDTO;
import com.example.demo.model.Notification;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.LabTestRepository;
import com.example.demo.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

@Service
public class PaymentService {

    @Autowired
    private AppointmentRepository appointmentRepo;

    @Autowired
    private LabTestRepository labRepo;

    @Autowired
    private NotificationRepository notificationRepo; // Added for patient alerts

    // This value is pulled from your .env file via application.properties
    @Value("${payhere.merchant.secret}")
    private String merchantSecret;


    public void processNotification(PaymentNotificationDTO dto) {
        if (verifyMd5Sig(dto)) { // Ensure this verification passes
            if ("2".equals(dto.getStatus_code())) { // Status 2 = Success
                updateRecordStatus(dto.getOrder_id(), dto.getPayment_id()); //

                Notification note = new Notification();
                note.setUserId(dto.getCustom_1()); //
                note.setTitle("Appointment Confirmed");
                note.setMessage("Payment Successful for Appointment #" + dto.getOrder_id()); //
                note.setCreatedAt(LocalDateTime.now()); //

                notificationRepo.save(note);
            }
        }
    }

    private boolean verifyMd5Sig(PaymentNotificationDTO dto) {
        try {
            // PayHere Signature Logic: Upper(MD5( merchant_id + order_id + amount + currency + status_code + Upper(MD5(secret)) ))
            String secretHash = md5(merchantSecret).toUpperCase();
            String mainString = dto.getMerchant_id() + dto.getOrder_id() + dto.getPayhere_amount()
                    + dto.getPayhere_currency() + dto.getStatus_code() + secretHash;

            return md5(mainString).toUpperCase().equals(dto.getMd5sig());
        } catch (Exception e) {
            return false;
        }
    }

    private void updateRecordStatus(String orderId, String payhereId) {
        LocalDateTime now = LocalDateTime.now();

        // Check if the orderId belongs to an Appointment
        appointmentRepo.findById(orderId).ifPresent(a -> {
            a.setPaymentStatus("PAID");
            a.setPayhereId(payhereId);
            a.setPaid(true);
            a.setPaidAt(now);
            a.setStatus("CONFIRMED");   // Transition from PENDING to CONFIRMED
            appointmentRepo.save(a);
        });

    }

    private String md5(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] messageDigest = md.digest(input.getBytes());
        BigInteger no = new BigInteger(1, messageDigest);
        String hashtext = no.toString(16);
        while (hashtext.length() < 32) {
            hashtext = "0" + hashtext;
        }
        return hashtext;
    }

    public String generatePaymentHash(String merchantId, String orderId, String amount, String currency) {
        try {
            String secretHash = md5(merchantSecret).toUpperCase();
            String data = merchantId + orderId + amount + currency + secretHash;
            return md5(data).toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException("Error generating hash", e);
        }
    }
}