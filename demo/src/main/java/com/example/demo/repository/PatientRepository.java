package com.example.demo.repository;

<<<<<<< HEAD
import com.example.demo.model.Patient;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PatientRepository extends MongoRepository<Patient, String> {
    Optional<Patient> findByEmail(String email);
=======
public class PatientRepository {
>>>>>>> 89e3041278f620dff59a1d43465db79ea34a6c27
}
