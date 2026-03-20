package com.example.demo.service;

import com.example.demo.dto.ScheduleRequest;
import com.example.demo.model.Schedule;
import com.example.demo.model.Doctor;
import com.example.demo.model.Hospital;
import com.example.demo.repository.ScheduleRepository;
import com.example.demo.repository.DoctorRepository;
import com.example.demo.repository.HospitalRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final DoctorRepository doctorRepository;
    private final HospitalRepository hospitalRepository;

    public ScheduleService(
            ScheduleRepository scheduleRepository,
            DoctorRepository doctorRepository,
            HospitalRepository hospitalRepository
    ) {
        this.scheduleRepository = scheduleRepository;
        this.doctorRepository = doctorRepository;
        this.hospitalRepository = hospitalRepository;
    }

    // ----------------- CREATE SCHEDULE -----------------
    public Schedule createSchedule(ScheduleRequest request) {
        Schedule schedule = new Schedule();

        schedule.setDoctorId(request.getDoctorId());
        schedule.setHospitalId(request.getHospitalId());
        schedule.setDate(request.getDate());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());

        schedule.setStatus("PENDING"); // default status

        return scheduleRepository.save(schedule);
    }

    // ----------------- DOCTOR SCHEDULES -----------------
    public List<Schedule> getDoctorSchedules(String doctorId) {
        List<Schedule> schedules = scheduleRepository.findByDoctorId(doctorId);
        populateDoctorAndHospitalInfo(schedules); // ✅ populate hospitalName
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
                    s.setHospitalName(hospital.getName());       // ✅ This fixes Unknown Hospital
                    s.setHospitalLocation(hospital.getLocation()); // optional
                }
            }
        }
    }
}