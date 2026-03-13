package com.example.demo.service;

import com.example.demo.dto.ScheduleRequest;
import com.example.demo.model.Schedule;
import com.example.demo.model.Doctor;
import com.example.demo.repository.ScheduleRepository;
import com.example.demo.repository.DoctorRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final DoctorRepository doctorRepository;

    public ScheduleService(ScheduleRepository scheduleRepository, DoctorRepository doctorRepository) {
        this.scheduleRepository = scheduleRepository;
        this.doctorRepository = doctorRepository;
    }

    // Hospital creates a schedule
    public Schedule createSchedule(ScheduleRequest request) {
        Schedule schedule = new Schedule();

        schedule.setDoctorId(request.getDoctorId());
        schedule.setHospitalId(request.getHospitalId());
        schedule.setDate(request.getDate());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());

        schedule.setStatus("PENDING");

        return scheduleRepository.save(schedule);
    }

    // Get schedules for a doctor
    public List<Schedule> getDoctorSchedules(String doctorId) {
        List<Schedule> schedules = scheduleRepository.findByDoctorId(doctorId);
        populateDoctorInfo(schedules);
        return schedules;
    }

    // Accept schedule
    public Schedule acceptSchedule(String id) {
        Schedule schedule = scheduleRepository.findById(id).orElseThrow();
        schedule.setStatus("ACCEPTED");
        return scheduleRepository.save(schedule);
    }

    // Reject schedule
    public Schedule rejectSchedule(String id) {
        Schedule schedule = scheduleRepository.findById(id).orElseThrow();
        schedule.setStatus("REJECTED");
        return scheduleRepository.save(schedule);
    }

    // Get all schedules for a hospital
    public List<Schedule> getHospitalSchedules(String hospitalId) {
        List<Schedule> schedules = scheduleRepository.findByHospitalId(hospitalId);
        populateDoctorInfo(schedules);
        return schedules;
    }

    // Get schedules for a hospital filtered by date
    public List<Schedule> getHospitalSchedulesByDate(String hospitalId, String date) {
        List<Schedule> schedules = scheduleRepository.findByHospitalIdAndDate(hospitalId, date);
        populateDoctorInfo(schedules);
        return schedules;
    }

    // Helper method to add doctor name and specialty
    private void populateDoctorInfo(List<Schedule> schedules) {
        for (Schedule s : schedules) {
            Doctor d = doctorRepository.findById(s.getDoctorId()).orElse(null);
            if (d != null) {
                s.setDoctorName("Dr. " + d.getFirstName() + " " + d.getLastName());
                s.setSpecialty(d.getSpecialization());
            }
        }
    }
}