package com.example.demo.controller;

<<<<<<< HEAD
public class DoctorController {
=======
import com.example.demo.model.Doctor;
import com.example.demo.service.DoctorService;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    @Autowired
    private DoctorService doctorService;

    // Doctor Sign-up
    @PostMapping("/signup")
    public Doctor signup(@RequestBody Doctor doctor) {
        return doctorService.registerDoctor(doctor);
    }

    // Doctor Login
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        Doctor doctor = doctorService.loginDoctor(request.getEmail(), request.getPassword());
        if(doctor != null) {
            return new LoginResponse("Login Successful", doctor.getId());
        } else {
            return new LoginResponse("Invalid Email or Password", null);
        }
    }
>>>>>>> 89e3041278f620dff59a1d43465db79ea34a6c27
}
