package com.example.demo.controller;

import com.example.demo.model.TestCategory;
import com.example.demo.repository.TestCategoryRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hospital/tests")
@CrossOrigin(origins = "http://localhost:5173")
public class TestCategoryController {

    private final TestCategoryRepository repo;

    public TestCategoryController(TestCategoryRepository repo) {
        this.repo = repo;
    }

    // ➕ CREATE TEST
    @PostMapping("/{hospitalId}")
    public TestCategory createTest(
            @PathVariable String hospitalId,
            @RequestBody TestCategory test
    ) {
        test.setHospitalId(hospitalId);
        return repo.save(test);
    }

    // 📥 GET TESTS
    @GetMapping("/{hospitalId}")
    public List<TestCategory> getTests(@PathVariable String hospitalId) {
        return repo.findByHospitalId(hospitalId);
    }

    // ✏ UPDATE TEST
    @PutMapping("/{id}")
    public TestCategory updateTest(
            @PathVariable String id,
            @RequestBody TestCategory updated
    ) {
        TestCategory existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Test not found"));

        existing.setTestName(updated.getTestName());
        existing.setPrice(updated.getPrice());

        return repo.save(existing);
    }

    // ❌ DELETE TEST
    @DeleteMapping("/{id}")
    public void deleteTest(@PathVariable String id) {
        repo.deleteById(id);
    }
}