package com.example.demo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.Date;

@Document(collection = "patients")
public class Patient {

    @Id
    private String id;
    private String title;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String nicOrPassport;
    private String contactNumber;
    private String residentialAddress;
    private String email;
    private boolean verified;
    private String verificationCode;
    private Date verificationExpiry;  
    private String password;
    private String profileImage;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getNicOrPassport() { return nicOrPassport; }
    public void setNicOrPassport(String nicOrPassport) { this.nicOrPassport = nicOrPassport; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getResidentialAddress() { return residentialAddress; }
    public void setResidentialAddress(String residentialAddress) { this.residentialAddress = residentialAddress; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isVerified() {return verified;}
    public void setVerified(boolean verified) {this.verified = verified;}

    public String getVerificationCode() {return verificationCode;}
    public void setVerificationCode(String verificationCode) {this.verificationCode = verificationCode;}

    public Date getVerificationExpiry() {return verificationExpiry;}
    public void setVerificationExpiry(Date verificationExpiry) {this.verificationExpiry = verificationExpiry;}

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

}
