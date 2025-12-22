package com.example.vibebank.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock service for mobile topup
 * Simulates mobile phone topup packages
 */
public class TopupMockService {
    
    public static class TopupPackage {
        private String name;
        private long amount;
        private String description;
        
        public TopupPackage(String name, long amount, String description) {
            this.name = name;
            this.amount = amount;
            this.description = description;
        }
        
        public String getName() { return name; }
        public long getAmount() { return amount; }
        public String getDescription() { return description; }
    }
    
    /**
     * Get all available topup packages
     */
    public static List<TopupPackage> getTopupPackages() {
        List<TopupPackage> packages = new ArrayList<>();
        
        packages.add(new TopupPackage("Gói 50K", 50000, "Nạp tiền điện thoại 50.000 VND"));
        packages.add(new TopupPackage("Gói 100K", 100000, "Nạp tiền điện thoại 100.000 VND"));
        packages.add(new TopupPackage("Gói 200K", 200000, "Nạp tiền điện thoại 200.000 VND"));
        packages.add(new TopupPackage("Gói 500K", 500000, "Nạp tiền điện thoại 500.000 VND"));
        
        return packages;
    }
    
    /**
     * Validate phone number format (Vietnamese phone numbers)
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return false;
        }
        
        // Remove spaces and special characters
        String cleaned = phoneNumber.replaceAll("[\\s\\-\\.]", "");
        
        // Vietnamese phone numbers: 10 digits starting with 0
        // or 9 digits starting with Vietnamese mobile prefixes
        if (cleaned.matches("^0\\d{9}$")) {
            return true;
        }
        
        // Also accept format with +84
        if (cleaned.matches("^\\+84\\d{9}$")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Format phone number for display
     */
    public static String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return phoneNumber;
        }
        
        // Remove all non-digit characters
        String cleaned = phoneNumber.replaceAll("[^\\d]", "");
        
        // If starts with 84, convert to 0
        if (cleaned.startsWith("84") && cleaned.length() == 11) {
            cleaned = "0" + cleaned.substring(2);
        }
        
        // Format as 0XXX XXX XXX
        if (cleaned.length() == 10 && cleaned.startsWith("0")) {
            return cleaned.substring(0, 4) + " " + cleaned.substring(4, 7) + " " + cleaned.substring(7);
        }
        
        return phoneNumber;
    }
    
    /**
     * Get carrier name from phone number prefix (for display purposes)
     */
    public static String getCarrierName(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return "Không xác định";
        }
        
        String cleaned = phoneNumber.replaceAll("[^\\d]", "");
        
        if (cleaned.startsWith("84")) {
            cleaned = "0" + cleaned.substring(2);
        }
        
        if (cleaned.length() < 4) {
            return "Không xác định";
        }
        
        String prefix = cleaned.substring(0, 4);
        
        // Viettel
        if (prefix.matches("^(086|096|097|098|032|033|034|035|036|037|038|039).*")) {
            return "Viettel";
        }
        // Vinaphone
        if (prefix.matches("^(088|091|094|083|084|085|081|082).*")) {
            return "Vinaphone";
        }
        // Mobifone
        if (prefix.matches("^(089|090|093|070|079|077|076|078).*")) {
            return "Mobifone";
        }
        // Vietnamobile
        if (prefix.matches("^(092|056|058).*")) {
            return "Vietnamobile";
        }
        // Gmobile
        if (prefix.matches("^(099|059).*")) {
            return "Gmobile";
        }
        
        return "Không xác định";
    }
}
