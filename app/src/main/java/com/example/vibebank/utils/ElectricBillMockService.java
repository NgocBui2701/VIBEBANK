package com.example.vibebank.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Mock service for electric bill data
 * Simulates checking and retrieving electric bills
 */
public class ElectricBillMockService {
    
    private static final String PREFS_NAME = "ElectricBillPrefs";
    private static final String KEY_PAID_BILLS = "paid_bills";
    private static Context appContext;
    
    public static class ElectricBill {
        private String customerId;
        private String customerName;
        private String address;
        private String period; // Kỳ hóa đơn (MM/YYYY)
        private int oldIndex; // Chỉ số cũ
        private int newIndex; // Chỉ số mới
        private int consumption; // Số điện tiêu thụ (kWh)
        private double unitPrice; // Đơn giá (VND/kWh)
        private double amount; // Tổng tiền
        private String status; // UNPAID, PAID
        private String dueDate; // Hạn thanh toán
        
        public ElectricBill(String customerId, String customerName, String address, 
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
    private static Map<String, ElectricBill> billDatabase = new HashMap<>();
    
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
            android.util.Log.e("ElectricBillMockService", "Cannot check paid status: appContext is null");
            return false;
        }
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> paidBills = prefs.getStringSet(KEY_PAID_BILLS, new HashSet<>());
        boolean isPaid = paidBills.contains(customerId);
        android.util.Log.d("ElectricBillMockService", "Checking " + customerId + " paid status: " + isPaid + ", Total paid bills: " + paidBills.size());
        return isPaid;
    }
    
    /**
     * Mark a bill as paid in SharedPreferences
     */
    private static void markAsPaidInPrefs(String customerId) {
        if (appContext == null) {
            android.util.Log.e("ElectricBillMockService", "Cannot mark as paid: appContext is null");
            return;
        }
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> paidBills = new HashSet<>(prefs.getStringSet(KEY_PAID_BILLS, new HashSet<>()));
        paidBills.add(customerId);
        boolean success = prefs.edit().putStringSet(KEY_PAID_BILLS, paidBills).commit(); // Use commit() for immediate write
        android.util.Log.d("ElectricBillMockService", "Marked " + customerId + " as paid. Success: " + success + ", Total paid: " + paidBills.size());
    }
    
    static {
        // Initialize some sample bills
        billDatabase.put("PE12345678", new ElectricBill(
            "PE12345678", 
            "Nguyễn Văn An", 
            "123 Đường Lê Lợi, Quận 1, TP.HCM",
            "12/2025",
            1500,
            1750,
            2500.0,
            "UNPAID",
            "30/12/2025"
        ));
        
        billDatabase.put("PE87654321", new ElectricBill(
            "PE87654321", 
            "Trần Thị Bình", 
            "456 Nguyễn Huệ, Quận 3, TP.HCM",
            "12/2025",
            2000,
            2350,
            2500.0,
            "UNPAID",
            "30/12/2025"
        ));
        
        billDatabase.put("PE11223344", new ElectricBill(
            "PE11223344", 
            "Lê Minh Châu", 
            "789 Hai Bà Trưng, Quận 5, TP.HCM",
            "12/2025",
            1800,
            2100,
            2500.0,
            "UNPAID",
            "30/12/2025"
        ));
        
        billDatabase.put("PE99887766", new ElectricBill(
            "PE99887766", 
            "Phạm Văn Đức", 
            "321 Võ Văn Tần, Quận 10, TP.HCM",
            "12/2025",
            3000,
            3450,
            2500.0,
            "UNPAID",
            "30/12/2025"
        ));
    }
    
    /**
     * Get bill by customer ID
     */
    public static ElectricBill getBill(String customerId) {
        if (customerId == null || customerId.trim().isEmpty()) {
            return null;
        }
        
        android.util.Log.d("ElectricBillMockService", "getBill called for: " + customerId + ", appContext: " + (appContext != null ? "available" : "null"));
        
        // Check in database
        ElectricBill bill = null;
        if (billDatabase.containsKey(customerId)) {
            bill = billDatabase.get(customerId);
            android.util.Log.d("ElectricBillMockService", "Found bill in database, initial status: " + bill.getStatus());
        } else if (customerId.toUpperCase().startsWith("PE") && customerId.length() >= 8) {
            // Generate random bill for any customer ID starting with "PE"
            bill = generateRandomBill(customerId);
            android.util.Log.d("ElectricBillMockService", "Generated new bill, status: " + bill.getStatus());
        }
        
        // Update status from SharedPreferences
        if (bill != null) {
            boolean isPaid = isPaidInPrefs(customerId);
            if (isPaid) {
                android.util.Log.d("ElectricBillMockService", "Setting bill status to PAID for " + customerId);
                bill.setStatus("PAID");
            } else {
                android.util.Log.d("ElectricBillMockService", "Bill status remains: " + bill.getStatus() + " for " + customerId);
            }
        }
        
        return bill;
    }
    
    /**
     * Generate random bill for unknown customer ID
     */
    private static ElectricBill generateRandomBill(String customerId) {
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
        
        int oldIndex = 1000 + random.nextInt(3000);
        int consumption = 200 + random.nextInt(300);
        int newIndex = oldIndex + consumption;
        
        ElectricBill bill = new ElectricBill(
            customerId,
            name,
            address,
            "12/2025",
            oldIndex,
            newIndex,
            2500.0,
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
        ElectricBill bill = billDatabase.get(customerId);
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
        ElectricBill bill = getBill(customerId);
        return bill != null && "UNPAID".equals(bill.getStatus());
    }
}
