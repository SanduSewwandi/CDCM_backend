package com.example.demo.repository;

import com.example.demo.model.Doctor;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends MongoRepository<Doctor, String> {

    Optional<Doctor> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByMedicalLicenseNumber(String medicalLicenseNumber);

    @Query("{ $or: [ " +
            "{ 'firstName': { $regex: ?0, $options: 'i' } }, " +
            "{ 'lastName': { $regex: ?0, $options: 'i' } }, " +
            "{ 'specialization': { $regex: ?0, $options: 'i' } }, " +
            "{ 'medicalLicenseNumber': { $regex: ?0, $options: 'i' } } " +
            "] }")
    List<Doctor> findBySearchTerm(String searchTerm);

    @Query("{ $and: [ " +
            "{ $or: [ " +
            "{ 'firstName': { $regex: ?0, $options: 'i' } }, " +
            "{ 'lastName': { $regex: ?0, $options: 'i' } }, " +
            "{ 'medicalLicenseNumber': { $regex: ?0, $options: 'i' } } " +
            "] }, " +
            "{ 'specialization': { $regex: ?1, $options: 'i' } } " +
            "] }")
    List<Doctor> findBySearchTermAndSpecialty(String searchTerm, String specialty);

    List<Doctor> findBySpecialization(String specialization);

    List<Doctor> findByVerifiedTrue();
}