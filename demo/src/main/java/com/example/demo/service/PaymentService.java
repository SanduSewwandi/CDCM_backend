package com.example.demo.service;

import com.example.demo.dto.PaymentHistoryDTO;
import com.example.demo.dto.PaymentNotificationDTO;
import com.example.demo.model.Appointment;
import com.example.demo.model.Doctor;
import com.example.demo.model.Hospital;
import com.example.demo.model.LabTest;
import com.example.demo.model.Notification;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.DoctorRepository;
import com.example.demo.repository.HospitalRepository;
import com.example.demo.repository.LabTestRepository;
import com.example.demo.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PaymentService {

    @Autowired
    private AppointmentRepository appointmentRepo;

    @Autowired
    private LabTestRepository labRepo;

    @Autowired
    private NotificationRepository notificationRepo; // Added for patient alerts

    @Autowired
    private DoctorRepository doctorRepo;

    @Autowired
    private HospitalRepository hospitalRepo;

    // This value is pulled from your .env file via application.properties
    @Value("${payhere.merchant.secret}")
    private String merchantSecret;


    public void processNotification(PaymentNotificationDTO dto) {
        if (verifyMd5Sig(dto)) { // Ensure this verification passes
            if ("2".equals(dto.getStatus_code())) { // Status 2 = Success
                Double amount = null;
                try {
                    if (dto.getPayhere_amount() != null) {
                        amount = Double.parseDouble(dto.getPayhere_amount());
                    }
                } catch (Exception ignored) {}
                confirmPaymentSuccess(dto.getOrder_id(), dto.getPayment_id(), amount);
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

    public Appointment confirmPaymentSuccess(String orderId, String payhereId, Double amount) {
        LocalDateTime now = LocalDateTime.now();

        Appointment appointment = appointmentRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Appointment not found with id: " + orderId));

        // Idempotency check: if already confirmed/paid, return directly without creating duplicate notifications
        if (appointment.isPaid() && "PAID".equalsIgnoreCase(appointment.getPaymentStatus()) && "CONFIRMED".equalsIgnoreCase(appointment.getStatus())) {
            return appointment;
        }

        appointment.setPaymentStatus("PAID");
        if (payhereId != null && !payhereId.trim().isEmpty()) {
            appointment.setPayhereId(payhereId);
        }
        appointment.setPaid(true);
        appointment.setPaidAt(now);
        appointment.setStatus("CONFIRMED");

        if (amount != null && amount > 0) {
            appointment.setAmount(amount);
        } else if (appointment.getAmount() <= 0) {
            appointment.setAmount(1000.00);
        }

        Appointment savedAppointment = appointmentRepo.save(appointment);

        // Create patient payment confirmation notification
        if (appointment.getPatientId() != null && notificationRepo != null) {
            String doctorName = "";
            if (appointment.getDoctorId() != null && doctorRepo != null) {
                doctorName = doctorRepo.findById(appointment.getDoctorId())
                        .map(d -> ((d.getTitle() != null ? d.getTitle() : "Dr.") + " " +
                                (d.getFirstName() != null ? d.getFirstName() : "") + " " +
                                (d.getLastName() != null ? d.getLastName() : "")).trim())
                        .orElse("");
            }

            Notification note = new Notification();
            note.setUserId(appointment.getPatientId());
            note.setTitle("Appointment Confirmed");
            String doctorSnippet = !doctorName.isEmpty() ? " with " + doctorName : "";
            note.setMessage("Payment Successful for Appointment #" + 
                    (appointment.getAppointmentNumber() != null ? appointment.getAppointmentNumber() : orderId) + 
                    doctorSnippet + " on " + (appointment.getDate() != null ? appointment.getDate() : "") + 
                    " at " + (appointment.getTime() != null ? appointment.getTime() : ""));
            note.setCreatedAt(now);
            note.setRead(false);
            if (appointment.getDoctorId() != null) {
                note.setDoctorId(appointment.getDoctorId());
            }
            if (!doctorName.isEmpty()) {
                note.setDoctorName(doctorName);
            }
            note.setScheduleType("PHYSICAL");

            notificationRepo.save(note);
        }

        return savedAppointment;
    }

    private void updateRecordStatus(String orderId, String payhereId) {
        confirmPaymentSuccess(orderId, payhereId, null);
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

    public List<PaymentHistoryDTO> getPatientPaymentHistory(String patientId) {
        List<Appointment> appointments = appointmentRepo.findByPatientId(patientId);
        List<PaymentHistoryDTO> historyList = new ArrayList<>();

        if (appointments != null) {
            for (Appointment a : appointments) {
                PaymentHistoryDTO dto = new PaymentHistoryDTO();
                dto.setId(a.getId());
                dto.setTransactionId(a.getPayhereId() != null && !a.getPayhereId().isEmpty() ? a.getPayhereId() : a.getId());
                dto.setAppointmentNumber(a.getAppointmentNumber());
                dto.setDate(a.getDate());
                dto.setTime(a.getTime());
                dto.setAmount(a.getAmount() > 0 ? a.getAmount() : 1000.00);
                dto.setPaymentStatus(a.getPaymentStatus() != null ? a.getPaymentStatus() : (a.isPaid() ? "PAID" : "PENDING"));
                dto.setPaid(a.isPaid() || "PAID".equalsIgnoreCase(a.getPaymentStatus()) || "PAID".equalsIgnoreCase(a.getStatus()));
                dto.setPaidAt(a.getPaidAt());

                // Fetch Doctor Name
                if (a.getDoctorId() != null && doctorRepo != null) {
                    doctorRepo.findById(a.getDoctorId()).ifPresent(d -> {
                        String title = d.getTitle() != null && !d.getTitle().isEmpty() ? d.getTitle() : "Dr.";
                        String firstName = d.getFirstName() != null ? d.getFirstName() : "";
                        String lastName = d.getLastName() != null ? d.getLastName() : "";
                        dto.setDoctorName((title + " " + firstName + " " + lastName).trim());
                    });
                }

                // Fetch Hospital Name
                if (a.getHospitalId() != null && hospitalRepo != null) {
                    hospitalRepo.findById(a.getHospitalId()).ifPresent(h -> {
                        dto.setHospitalName(h.getName());
                    });
                }

                dto.setDescription(dto.getDoctorName() != null && !dto.getDoctorName().isEmpty() 
                        ? "Doctor Channeling - " + dto.getDoctorName() 
                        : (a.getAppointmentNumber() != null ? "Doctor Appointment (" + a.getAppointmentNumber() + ")" : "Doctor Appointment"));
                dto.setType("APPOINTMENT");

                historyList.add(dto);
            }
        }

        // Include lab tests for the patient
        if (labRepo != null) {
            List<LabTest> labTests = labRepo.findByPatientId(patientId);
            if (labTests != null) {
                for (LabTest lt : labTests) {
                    PaymentHistoryDTO dto = new PaymentHistoryDTO();
                    dto.setId(lt.getId());
                    dto.setTransactionId(lt.getId());
                    dto.setAmount(lt.getPrice());
                    dto.setDate(lt.getTestDate() != null ? lt.getTestDate().toString() : (lt.getCreatedAt() != null ? lt.getCreatedAt().toLocalDate().toString() : ""));
                    dto.setPaymentStatus(lt.isPaid() ? "PAID" : "PENDING");
                    dto.setPaid(lt.isPaid());
                    dto.setPaidAt(lt.getPaidAt());
                    dto.setDescription("Lab Test - " + (lt.getTestType() != null ? lt.getTestType() : "Medical Test"));
                    dto.setType("LAB_TEST");

                    if (lt.getHospitalId() != null && hospitalRepo != null) {
                        hospitalRepo.findById(lt.getHospitalId()).ifPresent(h -> {
                            dto.setHospitalName(h.getName());
                        });
                    }

                    historyList.add(dto);
                }
            }
        }

        // Sort payments by date descending (newest first)
        historyList.sort((a, b) -> {
            if (a.getDate() != null && b.getDate() != null) {
                return b.getDate().compareTo(a.getDate());
            }
            return 0;
        });

        return historyList;
    }
}