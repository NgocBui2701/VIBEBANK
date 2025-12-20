package com.example.vibebank.data.manager;

public class RegisterDataManager {
    private static RegisterDataManager instance;

    // Dữ liệu từ Register 1
    private String email;
    private String phone;
    private String cccd;

    // Dữ liệu từ Register 2
    private String fullName;
    private String birthDate;
    private String gender;
    private String address;
    private String issueDate;

    // Dữ liệu từ Register 3
    private String frontIdCardPath;
    private String backIdCardPath;

    // Dữ liệu từ Register 5
    private String password;

    private RegisterDataManager() {}

    public static synchronized RegisterDataManager getInstance() {
        if (instance == null) {
            instance = new RegisterDataManager();
        }
        return instance;
    }

    public void clearData() {
        email = null; phone = null; cccd = null;
        fullName = null; birthDate = null; gender = null;
        address = null; issueDate = null;
        frontIdCardPath = null; backIdCardPath = null;
    }

    // Getters & Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getCccd() { return cccd; }
    public void setCccd(String cccd) { this.cccd = cccd; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getIssueDate() { return issueDate; }
    public void setIssueDate(String issueDate) { this.issueDate = issueDate; }

    public String getFrontIdCardPath() { return frontIdCardPath; }
    public void setFrontIdCardPath(String path) { this.frontIdCardPath = path; }

    public String getBackIdCardPath() { return backIdCardPath; }
    public void setBackIdCardPath(String path) { this.backIdCardPath = path; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getUsername() { return phone; }
    public void setUsername(String username) { this.phone = username; }
}