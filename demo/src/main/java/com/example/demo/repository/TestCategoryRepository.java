package com.example.demo.repository;

import com.example.demo.model.TestCategory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TestCategoryRepository extends MongoRepository<TestCategory, String> {

    List<TestCategory> findByHospitalId(String hospitalId);
}