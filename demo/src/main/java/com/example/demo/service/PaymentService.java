package com.example.demo.service;

import com.example.demo.dto.PaymentHistoryDTO;
import com.example.demo.dto.PaymentNotificationDTO;
import com.example.demo.model.Appointment;
import com.example.demo.model.Doctor;
import com.example.demo.model.Hospital;
import com.example.demo.model.LabTest;
import com.example.demo.model.Notification;
import com.example.demo.model.Schedule;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.DoctorRepository;
import com.example.demo.repository.HospitalRepository;
import com.example.demo.repository.LabTestRepository;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.repository.ScheduleRepository;
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

    @Autowired
    private ScheduleRepository scheduleRepo;

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

        // Ensure hospitalId is populated on the appointment if missing
        if ((appointment.getHospitalId() == null || appointment.getHospitalId().trim().isEmpty() || "null".equalsIgnoreCase(appointment.getHospitalId()))
                && appointment.getScheduleId() != null && scheduleRepo != null) {
            try {
                scheduleRepo.findById(appointment.getScheduleId()).ifPresent(s -> {
                    if (s.getHospitalId() != null && !s.getHospitalId().trim().isEmpty()) {
                        appointment.setHospitalId(s.getHospitalId());
                    }
                });
            } catch (Exception ignored) {}
        }
        // Fallback: If still missing, check doctor's associated hospitals
        if ((appointment.getHospitalId() == null || appointment.getHospitalId().trim().isEmpty() || "null".equalsIgnoreCase(appointment.getHospitalId()))
                && appointment.getDoctorId() != null && doctorRepo != null) {
            try {
                doctorRepo.findById(appointment.getDoctorId()).ifPresent(d -> {
                    if (d.getHospitals() != null && !d.getHospitals().isEmpty()) {
                        appointment.setHospitalId(d.getHospitals().get(0));
                    }
                });
            } catch (Exception ignored) {}
        }

        Appointment savedAppointment = appointmentRepo.save(appointment);

        // Fetch Doctor Name for notifications
        String doctorName = "";
        if (appointment.getDoctorId() != null && doctorRepo != null) {
            try {
                doctorName = doctorRepo.findById(appointment.getDoctorId())
                        .map(d -> ((d.getTitle() != null ? d.getTitle() : "Dr.") + " " +
                                (d.getFirstName() != null ? d.getFirstName() : "") + " " +
                                (d.getLastName() != null ? d.getLastName() : "")).trim())
                        .orElse("");
            } catch (Exception ignored) {}
        }

        String doctorSnippet = !doctorName.isEmpty() ? " with " + doctorName : "";
        String apptNumStr = appointment.getAppointmentNumber() != null ? appointment.getAppointmentNumber() : orderId;
        String dateStr = appointment.getDate() != null ? appointment.getDate() : "";
        String timeStr = appointment.getTime() != null && !appointment.getTime().isBlank() ? " at " + appointment.getTime() : "";

        // 1. Notify Patient
        try {
            if (appointment.getPatientId() != null && notificationRepo != null) {
                Notification note = new Notification();
                note.setUserId(appointment.getPatientId());
                note.setTitle("Appointment Confirmed");
                note.setMessage("Payment Successful for Appointment #" + apptNumStr + doctorSnippet + 
                        (!dateStr.isEmpty() ? " on " + dateStr : "") + timeStr);
                note.setCreatedAt(now);
                note.setRead(false);
                if (appointment.getDoctorId() != null) {
                    note.setDoctorId(appointment.getDoctorId());
                }
                if (!doctorName.isEmpty()) {
                    note.setDoctorName(doctorName);
                }
                if (appointment.getHospitalId() != null) {
                    note.setHospitalId(appointment.getHospitalId());
                }
                note.setScheduleType(appointment.getConsultationType() != null ? appointment.getConsultationType() : "PHYSICAL");
                notificationRepo.save(note);
            }
        } catch (Exception e) {
            System.err.println("Failed to create patient payment notification: " + e.getMessage());
        }

        // 2. Notify Doctor
        try {
            if (appointment.getDoctorId() != null && notificationRepo != null) {
                Notification docNote = new Notification();
                docNote.setUserId(appointment.getDoctorId());
                docNote.setDoctorId(appointment.getDoctorId());
                docNote.setScheduleId(appointment.getScheduleId());
                docNote.setScheduleType(appointment.getConsultationType() != null ? appointment.getConsultationType() : "PHYSICAL");
                docNote.setDate(appointment.getDate());
                docNote.setTime(appointment.getTime());
                docNote.setHospitalId(appointment.getHospitalId());

                if ("VIDEO".equalsIgnoreCase(appointment.getConsultationType())) {
                    docNote.setTitle("Video Consultation Confirmed");
                    docNote.setMessage("Patient confirmed payment for video consultation on " + dateStr + timeStr + ".");
                } else {
                    docNote.setTitle("Appointment Confirmed & Paid");
                    docNote.setMessage("Patient confirmed payment for Appointment #" + apptNumStr + " on " + dateStr + timeStr + ".");
                }
                docNote.setCreatedAt(now);
                docNote.setRead(false);
                if (!doctorName.isEmpty()) {
                    docNote.setDoctorName(doctorName);
                }
                notificationRepo.save(docNote);
            }
        } catch (Exception e) {
            System.err.println("Failed to create doctor payment notification: " + e.getMessage());
        }

        // 3. Notify Hospital
        try {
            String targetHospitalId = appointment.getHospitalId();
            if (targetHospitalId != null && !targetHospitalId.trim().isEmpty() && !"null".equalsIgnoreCase(targetHospitalId) && notificationRepo != null) {
                Notification hospNote = new Notification();
                hospNote.setUserId(targetHospitalId);
                hospNote.setHospitalId(targetHospitalId);
                hospNote.setDoctorId(appointment.getDoctorId());
                hospNote.setDoctorName(doctorName);
                hospNote.setScheduleId(appointment.getScheduleId());
                hospNote.setScheduleType(appointment.getConsultationType() != null ? appointment.getConsultationType() : "PHYSICAL");
                hospNote.setDate(appointment.getDate());
                hospNote.setTime(appointment.getTime());
                hospNote.setTitle("Payment Confirmed");
                String amountDisplay = String.format("%.2f", appointment.getAmount() > 0 ? appointment.getAmount() : 1000.00);
                hospNote.setMessage("Payment of LKR " + amountDisplay + " confirmed for Appointment #" + apptNumStr + doctorSnippet + ".");
                hospNote.setCreatedAt(now);
                hospNote.setRead(false);
                notificationRepo.save(hospNote);
                System.out.println("Payment notification successfully created and saved for Hospital ID: " + targetHospitalId);
            } else {
                System.err.println("Warning: Hospital ID is missing for appointment " + orderId + ", unable to send hospital notification.");
            }
        } catch (Exception e) {
            System.err.println("Failed to create hospital payment notification: " + e.getMessage());
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