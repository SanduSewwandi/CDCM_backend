package com.example.demo.dto;

import java.util.List;

public class DoctorAccountDTO {
    private String specialization;     // use existing field
    private List<String> qualifications;
    private String experience;
    private List<String> hospitals;

    public DoctorAccountDTO() {}

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public List<String> getQualifications() { return qualifications; }
    public void setQualifications(List<String> qualifications) { this.qualifications = qualifications; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public List<String> getHospitals() { return hospitals; }
    public void setHospitals(List<String> hospitals) { this.hospitals = hospitals; }
}