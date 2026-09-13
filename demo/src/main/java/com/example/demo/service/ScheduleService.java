package com.example.demo.service;

import com.example.demo.dto.ScheduleRequest;
import com.example.demo.model.Schedule;
import com.example.demo.model.Doctor;
import com.example.demo.model.Hospital;
import com.example.demo.repository.*;
import com.example.demo.model.Appointment;

import org.springframework.stereotype.Service;
import com.example.demo.model.Notification;
import com.example.demo.model.Patient;

import java.util.List;

@Service
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final DoctorRepository doctorRepository;
    private final HospitalRepository hospitalRepository;
    private final NotificationRepository notificationRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final SmsService smsService;
    private final EmailService emailService;


    public ScheduleService(
            ScheduleRepository scheduleRepository,
            DoctorRepository doctorRepository,
            HospitalRepository hospitalRepository,
            NotificationRepository notificationRepository,
            AppointmentRepository appointmentRepository,
            PatientRepository patientRepository,
            SmsService smsService,
            EmailService emailService
    ) {
        this.scheduleRepository = scheduleRepository;
        this.doctorRepository = doctorRepository;
        this.hospitalRepository = hospitalRepository;
        this.notificationRepository = notificationRepository;
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.smsService = smsService;
        this.emailService = emailService;
    }


    // ----------------- CREATE SCHEDULE -----------------
    public Schedule createSchedule(ScheduleRequest request) {
        Schedule schedule = new Schedule();

        schedule.setDoctorId(request.getDoctorId());
        schedule.setHospitalId(request.getHospitalId());
        schedule.setDate(request.getDate());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setStatus("PENDING");

        String type = request.getType();

        // FORCE VALID TYPE
        if (type == null || type.isEmpty()) {
            type = "PHYSICAL";
        }

        schedule.setType(type);

        // ONLY VIDEO HAS MEETING LINK
        if ("VIDEO".equalsIgnoreCase(type)) {
            schedule.setMeetingLink(request.getMeetingLink());
        } else {
            schedule.setMeetingLink(null);
        }

        return scheduleRepository.save(schedule);
    }

    // ----------------- DOCTOR SCHEDULES -----------------
    public List<Schedule> getDoctorSchedules(String doctorId) {
        List<Schedule> schedules = scheduleRepository.findByDoctorId(doctorId);
        populateDoctorAndHospitalInfo(schedules); //
        return schedules;
    }

    // ----------------- HOSPITAL SCHEDULES -----------------
    public List<Schedule> getHospitalSchedules(String hospitalId) {
        List<Schedule> schedules = scheduleRepository.findByHospitalId(hospitalId);
        populateDoctorAndHospitalInfo(schedules);
        return schedules;
    }

    public List<Schedule> getHospitalSchedulesByDate(String hospitalId, String date) {
        List<Schedule> schedules = scheduleRepository.findByHospitalIdAndDate(hospitalId, date);
        populateDoctorAndHospitalInfo(schedules);
        return schedules;
    }

    // ----------------- ACCEPT / REJECT -----------------
    public Schedule acceptSchedule(String id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found with id: " + id));
        schedule.setStatus("ACCEPTED");
        return scheduleRepository.save(schedule);
    }

    public Schedule rejectSchedule(String id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found with id: " + id));
        schedule.setStatus("REJECTED");
        return scheduleRepository.save(schedule);
    }

    // ----------------- CANCEL SCHEDULE -----------------
    public Schedule cancelSchedule(String id) {

        try {

            // =========================================================
            // 1. FIND SCHEDULE
            // =========================================================

            Schedule schedule = scheduleRepository.findById(id)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "Schedule not found with id: " + id
                            )
                    );


            // =========================================================
            // 2. ONLY ACCEPTED SCHEDULE CAN BE CANCELLED
            // =========================================================

            if (!"ACCEPTED".equalsIgnoreCase(schedule.getStatus())) {

                throw new RuntimeException(
                        "Only accepted schedules can be cancelled."
                );
            }


            // =========================================================
            // 3. GET DOCTOR INFORMATION
            // =========================================================

            Doctor doctor = doctorRepository
                    .findById(schedule.getDoctorId())
                    .orElse(null);

            String doctorName = "Doctor";

            if (doctor != null) {

                doctorName =
                        "Dr. "
                                + doctor.getFirstName()
                                + " "
                                + doctor.getLastName();
            }


            // =========================================================
            // 4. GET HOSPITAL INFORMATION
            // =========================================================

            Hospital hospital = hospitalRepository
                    .findById(schedule.getHospitalId())
                    .orElse(null);

            String hospitalName = "the hospital";

            if (hospital != null) {
                hospitalName = hospital.getName();
            }


            // =========================================================
            // 5. DETERMINE SCHEDULE TYPE
            // =========================================================

            boolean isVideo =
                    "VIDEO".equalsIgnoreCase(schedule.getType());

            String appointmentType;

            if (isVideo) {
                appointmentType = "video consultation";
            } else {
                appointmentType = "appointment";
            }


            // =========================================================
            // 6. CANCEL THE SCHEDULE
            // =========================================================

            schedule.setStatus("CANCELLED");

            Schedule updatedSchedule =
                    scheduleRepository.save(schedule);


            // =========================================================
            // 7. NOTIFY HOSPITAL
            // Doctor → Hospital
            // =========================================================

            Notification hospitalNotification =
                    new Notification();

            hospitalNotification.setUserId(
                    schedule.getHospitalId()
            );

            hospitalNotification.setHospitalId(
                    schedule.getHospitalId()
            );

            hospitalNotification.setScheduleId(
                    schedule.getId()
            );

            hospitalNotification.setScheduleType(
                    schedule.getType()
            );

            hospitalNotification.setDate(
                    schedule.getDate()
            );

            hospitalNotification.setTime(
                    schedule.getStartTime()
                            + " - "
                            + schedule.getEndTime()
            );

            hospitalNotification.setDoctorId(
                    schedule.getDoctorId()
            );

            hospitalNotification.setDoctorName(
                    doctorName
            );


            // Different title for video / physical
            if (isVideo) {

                hospitalNotification.setTitle(
                        "Video Consultation Cancelled"
                );

            } else {

                hospitalNotification.setTitle(
                        "Appointment Schedule Cancelled"
                );
            }


            hospitalNotification.setMessage(
                    doctorName
                            + " has cancelled the "
                            + appointmentType
                            + " schedule on "
                            + schedule.getDate()
                            + " from "
                            + schedule.getStartTime()
                            + " to "
                            + schedule.getEndTime()
                            + "."
            );

            hospitalNotification.setRead(false);

            notificationRepository.save(
                    hospitalNotification
            );


            // =========================================================
            // 8. FIND ALL APPOINTMENTS FOR THIS SCHEDULE
            // =========================================================

            List<Appointment> appointments =
                    appointmentRepository.findByScheduleId(
                            schedule.getId()
                    );


            System.out.println(
                    "=============================================="
            );

            System.out.println(
                    "Cancelled Schedule ID: "
                            + schedule.getId()
            );

            System.out.println(
                    "Schedule Type: "
                            + schedule.getType()
            );

            System.out.println(
                    "Appointments found: "
                            + appointments.size()
            );


            // =========================================================
// 9. PROCESS PAID AND PENDING APPOINTMENTS
// =========================================================

            for (Appointment appt : appointments) {

                System.out.println(
                        "Appointment ID: "
                                + appt.getId()
                                + " | Patient ID: "
                                + appt.getPatientId()
                                + " | Status: "
                                + appt.getStatus()
                                + " | Payment Status: "
                                + appt.getPaymentStatus()
                                + " | Is Paid: "
                                + appt.isPaid()
                );

                // Determine payment status
                boolean isPaid =
                        appt.isPaid()
                                || "PAID".equalsIgnoreCase(
                                appt.getPaymentStatus()
                        );

                boolean isPending =
                        "PENDING".equalsIgnoreCase(
                                appt.getPaymentStatus()
                        );

                // Only process PAID or PENDING appointments
                if (!isPaid && !isPending) {
                    System.out.println(
                            "Skipping appointment because payment status is: "
                                    + appt.getPaymentStatus()
                    );
                    continue;
                }


                // =========================================================
                // 10. CANCEL PATIENT APPOINTMENT
                // =========================================================

                appt.setStatus("CANCELLED");

                appointmentRepository.save(appt);


                // =========================================================
                // 11. FIND PATIENT
                // =========================================================

                Patient patient = patientRepository
                        .findById(appt.getPatientId())
                        .orElse(null);

                if (patient == null) {

                    System.out.println(
                            "Patient not found: "
                                    + appt.getPatientId()
                    );

                    continue;
                }


                // =========================================================
                // 12. GET PATIENT CONTACT DETAILS
               // =========================================================

                String phoneNumber = patient.getContactNumber();
                String email = patient.getEmail();

                if (phoneNumber == null || phoneNumber.trim().isEmpty()) {

                    System.out.println(
                            "Patient has no contact number: "
                                    + patient.getId()
                    );
                }


                // =========================================================
                // 13. CREATE PATIENT IN-APP NOTIFICATION
                // Hospital → Patient
                // =========================================================

                Notification patientNotification =
                        new Notification();

                patientNotification.setUserId(
                        appt.getPatientId()
                );

                patientNotification.setHospitalId(
                        schedule.getHospitalId()
                );

                patientNotification.setScheduleId(
                        schedule.getId()
                );

                patientNotification.setScheduleType(
                        schedule.getType()
                );

                patientNotification.setDate(
                        schedule.getDate()
                );

                patientNotification.setTime(
                        schedule.getStartTime()
                                + " - "
                                + schedule.getEndTime()
                );

                patientNotification.setDoctorId(
                        schedule.getDoctorId()
                );

                patientNotification.setDoctorName(
                        doctorName
                );


                // =========================================================
                // 14. CREATE NOTIFICATION MESSAGE
                // =========================================================

                if (isVideo) {

                    patientNotification.setTitle(
                            "Video Consultation Cancelled"
                    );

                    patientNotification.setMessage(
                            "Your video consultation with "
                                    + doctorName
                                    + " on "
                                    + schedule.getDate()
                                    + " from "
                                    + schedule.getStartTime()
                                    + " to "
                                    + schedule.getEndTime()
                                    + " has been cancelled by "
                                    + hospitalName
                                    + "."
                    );

                } else {

                    patientNotification.setTitle(
                            "Appointment Cancelled"
                    );

                    patientNotification.setMessage(
                            "Your appointment with "
                                    + doctorName
                                    + " on "
                                    + schedule.getDate()
                                    + " from "
                                    + schedule.getStartTime()
                                    + " to "
                                    + schedule.getEndTime()
                                    + " has been cancelled by "
                                    + hospitalName
                                    + "."
                    );
                }

                patientNotification.setRead(false);

                notificationRepository.save(
                        patientNotification
                );


                // =========================================================
                // 15. CREATE SMS MESSAGE
                // =========================================================

                String smsMessage;

                if (isPaid) {

                    smsMessage =
                            "Dear "
                                    + patient.getFirstName()
                                    + ", your "
                                    + appointmentType
                                    + " with "
                                    + doctorName
                                    + " on "
                                    + schedule.getDate()
                                    + " at "
                                    + schedule.getStartTime()
                                    + " has been cancelled by "
                                    + hospitalName
                                    + ". Your payment has already been received. "
                                    + "Please contact the hospital regarding your refund.";

                } else {

                    smsMessage =
                            "Dear "
                                    + patient.getFirstName()
                                    + ", your "
                                    + appointmentType
                                    + " with "
                                    + doctorName
                                    + " on "
                                    + schedule.getDate()
                                    + " at "
                                    + schedule.getStartTime()
                                    + " has been cancelled by "
                                    + hospitalName
                                    + ". Please contact the hospital for more information.";
                }

                  // =========================================================
                 // 16. SEND SMS
                 // =========================================================

                if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {

                    try {

                        smsService.sendSms(
                                phoneNumber,
                                smsMessage
                        );

                        System.out.println(
                                "SMS sent successfully to: "
                                        + phoneNumber
                        );

                    } catch (Exception smsException) {

                        // SMS failure should NOT stop schedule cancellation

                        System.err.println(
                                "Failed to send SMS to "
                                        + phoneNumber
                                        + ": "
                                        + smsException.getMessage()
                        );
                    }

                } else {

                    System.out.println(
                            "SMS skipped because patient has no phone number."
                    );
                }

                // =========================================================
// 17. SEND EMAIL
// =========================================================

                if (email != null && !email.trim().isEmpty()) {

                    try {

                        emailService.sendAppointmentCancellationEmail(
                                email,
                                patient.getFirstName(),
                                doctorName,
                                appointmentType,
                                schedule.getDate(),
                                schedule.getStartTime(),
                                hospitalName,
                                isPaid
                        );

                        System.out.println(
                                "Cancellation email sent successfully to: "
                                        + email
                        );

                    } catch (Exception emailException) {

                        // Email failure should NOT stop schedule cancellation

                        System.err.println(
                                "Failed to send cancellation email to "
                                        + email
                                        + ": "
                                        + emailException.getMessage()
                        );
                    }

                } else {

                    System.out.println(
                            "Patient has no email address: "
                                    + patient.getId()
                    );
                }




                System.out.println(
                        "Patient appointment cancelled: "
                                + appt.getId()
                );
            }


            System.out.println(
                    "=============================================="
            );




            // =========================================================
            // 14. RETURN UPDATED SCHEDULE
            // =========================================================

            return updatedSchedule;


        } catch (Exception e) {

            throw new RuntimeException(
                    "Error while cancelling schedule: "
                            + e.getMessage()
            );
        }
    }



    private void populateDoctorAndHospitalInfo(List<Schedule> schedules) {
        for (Schedule s : schedules) {

            // Populate doctor info
            if (s.getDoctorId() != null) {
                Doctor doctor = doctorRepository.findById(s.getDoctorId()).orElse(null);
                if (doctor != null) {
                    s.setDoctorName("Dr. " + doctor.getFirstName() + " " + doctor.getLastName());
                    s.setSpecialty(doctor.getSpecialization());
                }
            }

            // Populate hospital info
            if (s.getHospitalId() != null) {
                Hospital hospital = hospitalRepository.findById(s.getHospitalId()).orElse(null);
                if (hospital != null) {
                    s.setHospitalName(hospital.getName());
                    s.setHospitalLocation(hospital.getLocation());
                }
            }
        }
    }
}