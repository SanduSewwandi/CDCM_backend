package com.example.demo.service;

import com.example.demo.dto.ScheduleRequest;
import com.example.demo.model.Schedule;
import com.example.demo.model.Doctor;
import com.example.demo.model.Hospital;
import com.example.demo.repository.ScheduleRepository;
import com.example.demo.repository.DoctorRepository;
import com.example.demo.repository.HospitalRepository;
import com.example.demo.model.Appointment;
import com.example.demo.repository.AppointmentRepository;

import org.springframework.stereotype.Service;
import com.example.demo.model.Notification;
import com.example.demo.repository.NotificationRepository;


import java.util.List;

@Service
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final DoctorRepository doctorRepository;
    private final HospitalRepository hospitalRepository;
    private final NotificationRepository notificationRepository;
    private final AppointmentRepository appointmentRepository;

    public ScheduleService(
            ScheduleRepository scheduleRepository,
            DoctorRepository doctorRepository,
            HospitalRepository hospitalRepository,
            NotificationRepository notificationRepository,
            AppointmentRepository appointmentRepository
    ) {
        this.scheduleRepository = scheduleRepository;
        this.doctorRepository = doctorRepository;
        this.hospitalRepository = hospitalRepository;
        this.notificationRepository = notificationRepository;
        this.appointmentRepository = appointmentRepository;
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

    public Schedule cancelSchedule(String id) {
        try {

            Schedule schedule = scheduleRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Schedule not found with id: " + id));

            schedule.setStatus("CANCELLED");
            Schedule updatedSchedule = scheduleRepository.save(schedule);


            Doctor doctor = doctorRepository.findById(schedule.getDoctorId()).orElse(null);
            Hospital hospital = hospitalRepository.findById(schedule.getHospitalId()).orElse(null);

            String doctorName = "Doctor";
            if (doctor != null) {
                doctorName = "Dr. " + doctor.getFirstName() + " " + doctor.getLastName();
            }

            String hospitalName = "the hospital";
            if (hospital != null) {
                hospitalName = hospital.getName();
            }


            List<Appointment> appointments =
                    appointmentRepository.findByScheduleId(schedule.getId());

            for (Appointment appt : appointments) {

                Notification patientNotification = new Notification();

                patientNotification.setUserId(appt.getPatientId());
                patientNotification.setHospitalId(schedule.getHospitalId());
                patientNotification.setScheduleId(schedule.getId());

                patientNotification.setScheduleType(schedule.getType());
                patientNotification.setDate(schedule.getDate());
                patientNotification.setTime(schedule.getStartTime() + " - " + schedule.getEndTime());
                patientNotification.setDoctorId(schedule.getDoctorId());
                patientNotification.setDoctorName(doctorName);

                patientNotification.setMessage(
                        "Your appointment with " + doctorName +
                                " on " + schedule.getDate() +
                                " has been cancelled by " + hospitalName + "."
                );

                patientNotification.setRead(false);

                notificationRepository.save(patientNotification);
            }


            Notification hospitalNotification = new Notification();
            hospitalNotification.setUserId(schedule.getHospitalId());
            hospitalNotification.setMessage(
                    doctorName + " has cancelled the schedule on "
                            + schedule.getDate()
                            + " from " + schedule.getStartTime()
                            + " to " + schedule.getEndTime()
                            + " at " + hospitalName + "."
            );

            hospitalNotification.setRead(false);
            notificationRepository.save(hospitalNotification);

            return updatedSchedule;

        } catch (Exception e) {
            throw new RuntimeException("Error while cancelling schedule: " + e.getMessage());
        }
    }
    // ----------------- HELPER METHOD -----------------
    /**
     * Populates doctorName, specialty, hospitalName, and hospitalLocation
     * for all schedules in the list.
     */
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