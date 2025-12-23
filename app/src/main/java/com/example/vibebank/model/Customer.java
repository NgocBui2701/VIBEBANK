package com.example.vibebank.model;

public class Customer {
    private String userId;
    private String fullName;
    private String phoneNumber;
    private String email;
    private String accountNumber;
    private String kycStatus; // "pending", "verified", "rejected"
    private double balance;

    public Customer() {}

    public Customer(String userId, String fullName, String phoneNumber, String email, 
                   String accountNumber, String kycStatus, double balance) {
        this.userId = userId;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.accountNumber = accountNumber;
        this.kycStatus = kycStatus;
        this.balance = balance;
    }

    // Getters
    public String getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getEmail() { return email; }
    public String getAccountNumber() { return accountNumber; }
    public String getKycStatus() { return kycStatus; }
    public double getBalance() { return balance; }

    // Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setEmail(String email) { this.email = email; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public void setKycStatus(String kycStatus) { this.kycStatus = kycStatus; }
    public void setBalance(double balance) { this.balance = balance; }

    public String getKycStatusDisplay() {
        if ("pending".equals(kycStatus)) {
            return "Chờ duyệt";
        } else if ("verified".equals(kycStatus)) {
            return "Đã xác minh";
        } else if ("rejected".equals(kycStatus)) {
            return "Từ chối";
        }
        return "Chưa rõ";
    }
}


