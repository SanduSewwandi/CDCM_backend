package com.example.demo.repository;

import com.example.demo.model.Schedule;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ScheduleRepository extends MongoRepository<Schedule, String> {

    List<Schedule> findByDoctorId(String doctorId);
    List<Schedule> findByHospitalId(String hospitalId);

}