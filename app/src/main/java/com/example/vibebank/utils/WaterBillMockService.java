package com.example.vibebank.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Mock service for water bill data
 * Simulates checking and retrieving water bills
 */
public class WaterBillMockService {
    
    private static final String PREFS_NAME = "WaterBillPrefs";
    private static final String KEY_PAID_BILLS = "paid_bills";
    private static Context appContext;
    
    public static class WaterBill {
        private String customerId;
        private String customerName;
        private String address;
        private String period; // Kỳ hóa đơn (MM/YYYY)
        private int oldIndex; // Chỉ số cũ
        private int newIndex; // Chỉ số mới
        private int consumption; // Số nước tiêu thụ (m³)
        private double unitPrice; // Đơn giá (VND/m³)
        private double amount; // Tổng tiền
        private String status; // UNPAID, PAID
        private String dueDate; // Hạn thanh toán
        
        public WaterBill(String customerId, String customerName, String address, 
                        String period, int oldIndex, int newIndex, double unitPrice, 
                        String status, String dueDate) {
            this.customerId = customerId;
            this.customerName = customerName;
            this.address = address;
            this.period = period;
            this.oldIndex = oldIndex;
            this.newIndex = newIndex;
            this.consumption = newIndex - oldIndex;
            this.unitPrice = unitPrice;
            this.amount = consumption * unitPrice;
            this.status = status;
            this.dueDate = dueDate;
        }
        
        // Getters
        public String getCustomerId() { return customerId; }
        public String getCustomerName() { return customerName; }
        public String getAddress() { return address; }
        public String getPeriod() { return period; }
        public int getOldIndex() { return oldIndex; }
        public int getNewIndex() { return newIndex; }
        public int getConsumption() { return consumption; }
        public double getUnitPrice() { return unitPrice; }
        public double getAmount() { return amount; }
        public String getStatus() { return status; }
        public String getDueDate() { return dueDate; }
        
        public void setStatus(String status) { this.status = status; }
    }
    
    // Mock database
    private static Map<String, WaterBill> billDatabase = new HashMap<>();
    
    /**
     * Initialize the service with application context
     */
    public static void initialize(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
        }
    }
    
    /**
     * Check if a bill is marked as paid in SharedPreferences
     */
    private static boolean isPaidInPrefs(String customerId) {
        if (appContext == null) {
            android.util.Log.e("WaterBillMockService", "Cannot check paid status: appContext is null");
            return false;
        }
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> paidBills = prefs.getStringSet(KEY_PAID_BILLS, new HashSet<>());
        boolean isPaid = paidBills.contains(customerId);
        android.util.Log.d("WaterBillMockService", "Checking " + customerId + " paid status: " + isPaid + ", Total paid bills: " + paidBills.size());
        return isPaid;
    }
    
    /**
     * Mark a bill as paid in SharedPreferences
     */
    private static void markAsPaidInPrefs(String customerId) {
        if (appContext == null) {
            android.util.Log.e("WaterBillMockService", "Cannot mark as paid: appContext is null");
            return;
        }
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> paidBills = new HashSet<>(prefs.getStringSet(KEY_PAID_BILLS, new HashSet<>()));
        paidBills.add(customerId);
        boolean success = prefs.edit().putStringSet(KEY_PAID_BILLS, paidBills).commit();
        android.util.Log.d("WaterBillMockService", "Marked " + customerId + " as paid. Success: " + success + ", Total paid: " + paidBills.size());
    }
    
    static {
        // Initialize some sample bills
        billDatabase.put("PW12345678", new WaterBill(
            "PW12345678", 
            "Nguyễn Văn An", 
            "123 Đường Lê Lợi, Quận 1, TP.HCM",
            "12/2025",
            150,
            175,
            15000.0,
            "UNPAID",
            "30/12/2025"
        ));
        
        billDatabase.put("PW87654321", new WaterBill(
            "PW87654321", 
            "Trần Thị Bình", 
            "456 Nguyễn Huệ, Quận 3, TP.HCM",
            "12/2025",
            200,
            235,
            15000.0,
            "UNPAID",
            "30/12/2025"
        ));
        
        billDatabase.put("PW11223344", new WaterBill(
            "PW11223344", 
            "Lê Minh Châu", 
            "789 Hai Bà Trưng, Quận 5, TP.HCM",
            "12/2025",
            180,
            210,
            15000.0,
            "UNPAID",
            "30/12/2025"
        ));
        
        billDatabase.put("PW99887766", new WaterBill(
            "PW99887766", 
            "Phạm Văn Đức", 
            "321 Võ Văn Tần, Quận 10, TP.HCM",
            "12/2025",
            300,
            345,
            15000.0,
            "UNPAID",
            "30/12/2025"
        ));
    }
    
    /**
     * Get bill by customer ID
     */
    public static WaterBill getBill(String customerId) {
        if (customerId == null || customerId.trim().isEmpty()) {
            return null;
        }
        
        android.util.Log.d("WaterBillMockService", "getBill called for: " + customerId + ", appContext: " + (appContext != null ? "available" : "null"));
        
        // Check in database
        WaterBill bill = null;
        if (billDatabase.containsKey(customerId)) {
            bill = billDatabase.get(customerId);
            android.util.Log.d("WaterBillMockService", "Found bill in database, initial status: " + bill.getStatus());
        } else if (customerId.toUpperCase().startsWith("PW") && customerId.length() >= 8) {
            // Generate random bill for any customer ID starting with "PW"
            bill = generateRandomBill(customerId);
            android.util.Log.d("WaterBillMockService", "Generated new bill, status: " + bill.getStatus());
        }
        
        // Update status from SharedPreferences
        if (bill != null) {
            boolean isPaid = isPaidInPrefs(customerId);
            if (isPaid) {
                android.util.Log.d("WaterBillMockService", "Setting bill status to PAID for " + customerId);
                bill.setStatus("PAID");
            } else {
                android.util.Log.d("WaterBillMockService", "Bill status remains: " + bill.getStatus() + " for " + customerId);
            }
        }
        
        return bill;
    }
    
    /**
     * Generate random bill for unknown customer ID
     */
    private static WaterBill generateRandomBill(String customerId) {
        Random random = new Random(customerId.hashCode());
        
        String[] firstNames = {"Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Phan", "Vũ", "Đặng", "Bùi", "Đỗ"};
        String[] middleNames = {"Văn", "Thị", "Minh", "Hữu", "Đức", "Anh", "Thanh", "Quốc", "Hồng", "Kim"};
        String[] lastNames = {"An", "Bình", "Châu", "Dũng", "Em", "Phong", "Giang", "Hà", "Hương", "Khanh"};
        
        String name = firstNames[random.nextInt(firstNames.length)] + " " +
                     middleNames[random.nextInt(middleNames.length)] + " " +
                     lastNames[random.nextInt(lastNames.length)];
        
        String[] streets = {"Lê Lợi", "Nguyễn Huệ", "Trần Hưng Đạo", "Hai Bà Trưng", "Võ Văn Tần", 
                           "Điện Biên Phủ", "Cách Mạng Tháng 8", "Phan Xích Long"};
        String[] districts = {"Quận 1", "Quận 3", "Quận 5", "Quận 10", "Quận Tân Bình", "Quận Bình Thạnh"};
        
        String address = (100 + random.nextInt(900)) + " " + 
                        streets[random.nextInt(streets.length)] + ", " +
                        districts[random.nextInt(districts.length)] + ", TP.HCM";
        
        int oldIndex = 100 + random.nextInt(300);
        int consumption = 20 + random.nextInt(30);
        int newIndex = oldIndex + consumption;
        
        WaterBill bill = new WaterBill(
            customerId,
            name,
            address,
            "12/2025",
            oldIndex,
            newIndex,
            15000.0,
            "UNPAID",
            "30/12/2025"
        );
        
        // Store for future queries
        billDatabase.put(customerId, bill);
        
        return bill;
    }
    
    /**
     * Mark bill as paid
     */
    public static boolean payBill(String customerId) {
        WaterBill bill = billDatabase.get(customerId);
        if (bill != null) {
            bill.setStatus("PAID");
            markAsPaidInPrefs(customerId);
            return true;
        }
        // Even if not in database yet, mark as paid in prefs
        markAsPaidInPrefs(customerId);
        return true;
    }
    
    /**
     * Check if bill exists and unpaid
     */
    public static boolean canPayBill(String customerId) {
        WaterBill bill = getBill(customerId);
        return bill != null && "UNPAID".equals(bill.getStatus());
    }
}
