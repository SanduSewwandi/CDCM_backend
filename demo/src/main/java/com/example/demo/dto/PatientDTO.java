package com.example.demo.dto;

import com.example.demo.model.Patient;

public class PatientDTO {
    private String id;
    private String patientId; // Formatted ID like P-00001
    private String name;
    private String email;
    private String contactNumber;
    private String gender;
    private String dateOfBirth;
    private String lastVisit;
    private boolean verified;
    private String profileImage;

    public PatientDTO() {}

    public PatientDTO(Patient patient) {
        this.id = patient.getId();
        this.patientId = generatePatientId(patient.getId());
        this.name = patient.getTitle() + " " + patient.getFirstName() + " " + patient.getLastName();
        this.email = patient.getEmail();
        this.contactNumber = patient.getContactNumber();
        this.gender = extractGender(patient.getNicOrPassport());
        this.dateOfBirth = patient.getDateOfBirth() != null ? patient.getDateOfBirth().toString() : "";
        this.verified = patient.isVerified();
        this.profileImage = patient.getProfileImage();
    }

    private String generatePatientId(String id) {
        try {
            String numeric = id.replaceAll("[^0-9]", "");
            if (numeric.length() >= 5) {
                numeric = numeric.substring(0, 5);
            } else if (numeric.length() > 0) {
                numeric = String.format("%05d", numeric.hashCode() % 100000);
            } else {
                numeric = String.format("%05d", Math.abs(id.hashCode()) % 100000);
            }
            return "P-" + numeric;
        } catch (Exception e) {
            return "P-" + id.substring(Math.max(0, id.length() - 5));
        }
    }

    private String extractGender(String nicOrPassport) {
        if (nicOrPassport != null && !nicOrPassport.isEmpty()) {
            String lastChar = nicOrPassport.substring(nicOrPassport.length() - 1);
            if (lastChar.matches("[vVxX]")) {
                // Sri Lankan NIC
                String digits = nicOrPassport.replaceAll("[^0-9]", "");
                if (digits.length() >= 5) {
                    int lastDigit = Integer.parseInt(digits.substring(digits.length() - 1));
                    return lastDigit % 2 == 0 ? "Female" : "Male";
                }
            } else if (lastChar.matches("\\d")) {
                int digit = Integer.parseInt(lastChar);
                return digit % 2 == 0 ? "Female" : "Male";
            }
        }
        return "Not Specified";
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getLastVisit() { return lastVisit; }
    public void setLastVisit(String lastVisit) { this.lastVisit = lastVisit; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }
}