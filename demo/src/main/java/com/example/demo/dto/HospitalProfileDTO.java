package com.example.demo.dto;

public class HospitalProfileDTO {

    private String id;
    private String name;
    private String email;
    private String contactNumber;
    private String address;
    private String licenseNumber;
    private String managerName;
    private String location;
    private String profileImage;
    private boolean verified;
    private boolean mustChangePassword;

    public HospitalProfileDTO() {
    }

    public HospitalProfileDTO(
            String id,
            String name,
            String email,
            String contactNumber,
            String address,
            String licenseNumber,
            String managerName,
            String location,
            String profileImage,
            boolean verified,
            boolean mustChangePassword) {

        this.id = id;
        this.name = name;
        this.email = email;
        this.contactNumber = contactNumber;
        this.address = address;
        this.licenseNumber = licenseNumber;
        this.managerName = managerName;
        this.location = location;
        this.profileImage = profileImage;
        this.verified = verified;
        this.mustChangePassword = mustChangePassword;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }
}