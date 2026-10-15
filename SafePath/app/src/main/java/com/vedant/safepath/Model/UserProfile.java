package com.vedant.safepath.Model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_profile")
public class UserProfile {

    @PrimaryKey
    @NonNull
    private String uid;

    private String name;
    private String email;
    private String phone;
    private String address;
    private String bloodGroup;
    private String allergies;
    private String profileImageUrl;
    private String emergencyContact;

    public UserProfile() {}
    public UserProfile(@NonNull String uid, String name, String email, String phone, String address, String bloodGroup, String allergies, String profileImageUrl, String emergencyContact) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.bloodGroup = bloodGroup;
        this.allergies = allergies;
        this.profileImageUrl = profileImageUrl;
        this.emergencyContact = emergencyContact;
    }

    @NonNull
    public String getUid() { return uid; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getBloodGroup() { return bloodGroup; }
    public String getAllergies() { return allergies; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public String getEmergencyContact() { return emergencyContact; }

    public void setUid(@NonNull String uid) { this.uid = uid; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setAddress(String address) { this.address = address; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }
    public void setAllergies(String allergies) { this.allergies = allergies; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }
}
