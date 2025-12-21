package com.example.vibebank.utils;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

/**
 * VNPAY Mock Service - Simulates payment gateway behavior
 * THIS IS A SIMULATION - NO REAL API CALLS
 * 
 * Features:
 * - Simulates 2-5 second network delay
 * - 95% success rate, 5% random failure
 * - Generates fake VNPAY transaction references
 * - Calculates transfer fees (2,500 VND for external banks)
 */
public class VNPayMockService {
    
    private static final String TAG = "VNPayMockService";
    public static final long TRANSFER_FEE = 2500; // 2,500 VND for external transfers
    private static final float SUCCESS_RATE = 0.95f; // 95% success rate
    
    private final Handler handler;
    private final Random random;
    
    public interface PaymentCallback {
        void onSuccess(PaymentResult result);
        void onFailure(String errorMessage);
    }
    
    public static class PaymentRequest {
        public String senderAccountNumber;
        public String receiverBankBIN;
        public String receiverAccountNumber;
        public String receiverName;
        public long amount;
        public String description;
        
        public PaymentRequest(String senderAccNum, String receiverBIN, String receiverAccNum, 
                            String receiverName, long amount, String description) {
            this.senderAccountNumber = senderAccNum;
            this.receiverBankBIN = receiverBIN;
            this.receiverAccountNumber = receiverAccNum;
            this.receiverName = receiverName;
            this.amount = amount;
            this.description = description;
        }
    }
    
    public static class PaymentResult {
        public String vnpayTransactionId;
        public String bankTransactionId;
        public long amount;
        public long fee;
        public long totalAmount;
        public String status; // "SUCCESS" or "FAILED"
        public String message;
        public String timestamp;
        
        public PaymentResult() {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", 
                new Locale.Builder().setLanguage("vi").setRegion("VN").build());
            this.timestamp = sdf.format(new Date());
        }
    }
    
    public VNPayMockService() {
        this.handler = new Handler(Looper.getMainLooper());
        this.random = new Random();
    }
    
    /**
     * Process payment request through VNPAY simulation
     * Simulates 2-5 second delay for realistic experience
     * 
     * @param request Payment request details
     * @param callback Success/failure callback
     */
    public void processPayment(PaymentRequest request, PaymentCallback callback) {
        // Simulate network delay (2-5 seconds)
        int delay = 2000 + random.nextInt(3000);
        
        Log.d(TAG, "Processing VNPAY payment - Amount: " + request.amount + 
              ", Delay: " + delay + "ms");
        
        handler.postDelayed(() -> {
            // Simulate success/failure based on success rate
            boolean isSuccess = random.nextFloat() < SUCCESS_RATE;
            
            if (isSuccess) {
                PaymentResult result = createSuccessResult(request);
                Log.d(TAG, "Payment SUCCESS - Txn ID: " + result.vnpayTransactionId);
                callback.onSuccess(result);
            } else {
                String errorMessage = getRandomErrorMessage();
                Log.w(TAG, "Payment FAILED - " + errorMessage);
                callback.onFailure(errorMessage);
            }
        }, delay);
    }
    
    /**
     * Create successful payment result
     */
    private PaymentResult createSuccessResult(PaymentRequest request) {
        PaymentResult result = new PaymentResult();
        
        // Generate fake VNPAY transaction ID
        result.vnpayTransactionId = generateVNPayTransactionId();
        result.bankTransactionId = generateBankTransactionId();
        
        // Calculate amounts
        result.amount = request.amount;
        result.fee = TRANSFER_FEE;
        result.totalAmount = request.amount + TRANSFER_FEE;
        
        result.status = "SUCCESS";
        result.message = "Giao dịch thành công qua VNPAY";
        
        return result;
    }
    
    /**
     * Generate fake VNPAY transaction ID
     * Format: VNP_YYYYMMDD_XXXXXXXX
     */
    private String generateVNPayTransactionId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.US);
        String date = sdf.format(new Date());
        String randomPart = String.format("%08d", random.nextInt(100000000));
        return "VNP_" + date + "_" + randomPart;
    }
    
    /**
     * Generate fake bank transaction ID
     * Format: FT25XXXXXXXX
     */
    private String generateBankTransactionId() {
        String randomPart = String.format("%010d", random.nextInt(1000000000));
        return "FT25" + randomPart;
    }
    
    /**
     * Get random error message for failed transactions
     */
    private String getRandomErrorMessage() {
        String[] errorMessages = {
            "Kết nối với ngân hàng bị gián đoạn",
            "Tài khoản người nhận tạm thời không khả dụng",
            "Hạn mức giao dịch đã vượt quá",
            "Lỗi xử lý từ cổng thanh toán",
            "Không thể xác thực giao dịch"
        };
        return errorMessages[random.nextInt(errorMessages.length)];
    }
    
    /**
     * Get transfer fee for external bank transfers
     */
    public static long getTransferFee() {
        return TRANSFER_FEE;
    }
    
    /**
     * Calculate total amount (amount + fee)
     */
    public static long calculateTotalAmount(long amount) {
        return amount + TRANSFER_FEE;
    }
    
    /**
     * Verify payment status (for checking existing transactions)
     * In real implementation, this would query VNPAY API
     */
    public void verifyPayment(String vnpayTransactionId, PaymentCallback callback) {
        // Simulate verification delay
        handler.postDelayed(() -> {
            PaymentResult result = new PaymentResult();
            result.vnpayTransactionId = vnpayTransactionId;
            result.status = "SUCCESS";
            result.message = "Giao dịch đã được xác nhận";
            callback.onSuccess(result);
        }, 1000);
    }
}
