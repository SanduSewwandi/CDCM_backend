package com.example.demo.service;

import com.example.demo.dto.ScheduleRequest;
import com.example.demo.model.Schedule;
import com.example.demo.repository.ScheduleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;

    public ScheduleService(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    // Hospital creates schedule
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

    // Doctor schedules
    public List<Schedule> getDoctorSchedules(String doctorId) {
        return scheduleRepository.findByDoctorId(doctorId);
    }

    // Accept
    public Schedule acceptSchedule(String id) {
        Schedule schedule = scheduleRepository.findById(id).orElseThrow();
        schedule.setStatus("ACCEPTED");
        return scheduleRepository.save(schedule);
    }

    // Reject
    public Schedule rejectSchedule(String id) {
        Schedule schedule = scheduleRepository.findById(id).orElseThrow();
        schedule.setStatus("REJECTED");
        return scheduleRepository.save(schedule);
    }

    public List<Schedule> getHospitalSchedules(String hospitalId) {
        return scheduleRepository.findByHospitalId(hospitalId);
    }
}
