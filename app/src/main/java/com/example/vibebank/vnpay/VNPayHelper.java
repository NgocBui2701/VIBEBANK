package com.example.vibebank.vnpay;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * VNPay Helper - Build payment URL and verify signatures
 */
public class VNPayHelper {
    
    private static final String TAG = "VNPayHelper";
    
    /**
     * Build VNPay payment URL
     * 
     * @param txnRef     Transaction reference (unique order ID)
     * @param amount     Amount in VND (will be multiplied by 100)
     * @param orderInfo  Order description (no Vietnamese accents)
     * @param ipAddr     Customer IP address
     * @return Payment URL to redirect customer
     */
    public static String buildPaymentUrl(String txnRef, long amount, String orderInfo, String ipAddr) {
        return buildPaymentUrl(txnRef, amount, orderInfo, ipAddr, null, VNPayConfig.LOCALE_VN);
    }
    
    /**
     * Build VNPay payment URL with bank code
     * 
     * @param txnRef     Transaction reference (unique order ID)
     * @param amount     Amount in VND (will be multiplied by 100)
     * @param orderInfo  Order description (no Vietnamese accents)
     * @param ipAddr     Customer IP address
     * @param bankCode   Bank code (optional, null to let user choose at VNPay)
     * @param locale     Locale (vn or en)
     * @return Payment URL to redirect customer
     */
    public static String buildPaymentUrl(String txnRef, long amount, String orderInfo, 
                                        String ipAddr, String bankCode, String locale) {
        try {
            // Create timestamp (GMT+7)
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            String vnp_CreateDate = formatter.format(new Date());
            
            // Expire date: 15 minutes from now
            String vnp_ExpireDate = formatter.format(new Date(System.currentTimeMillis() + 15 * 60 * 1000));
            
            // Amount in smallest unit (multiply by 100 to remove decimal)
            long vnp_Amount = amount * 100;
            
            // Build parameter map
            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", VNPayConfig.VNP_VERSION);
            vnp_Params.put("vnp_Command", VNPayConfig.VNP_COMMAND);
            vnp_Params.put("vnp_TmnCode", VNPayConfig.VNP_TMN_CODE);
            vnp_Params.put("vnp_Amount", String.valueOf(vnp_Amount));
            vnp_Params.put("vnp_CurrCode", VNPayConfig.VNP_CURR_CODE);
            vnp_Params.put("vnp_TxnRef", txnRef);
            vnp_Params.put("vnp_OrderInfo", orderInfo);
            vnp_Params.put("vnp_OrderType", VNPayConfig.ORDER_TYPE_OTHER);
            vnp_Params.put("vnp_Locale", locale);
            vnp_Params.put("vnp_ReturnUrl", VNPayConfig.VNP_RETURN_URL);
            vnp_Params.put("vnp_IpAddr", ipAddr);
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
            vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);
            
            // Add bank code if specified
            if (bankCode != null && !bankCode.isEmpty()) {
                vnp_Params.put("vnp_BankCode", bankCode);
            }
            
            // Sort parameters by key (required for checksum)
            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames);
            
            // Build hash data and query string
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();
            
            for (String fieldName : fieldNames) {
                String fieldValue = vnp_Params.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    // Build hash data
                    if (hashData.length() > 0) {
                        hashData.append('&');
                    }
                    hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                    
                    // Build query string
                    if (query.length() > 0) {
                        query.append('&');
                    }
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()))
                         .append('=')
                         .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                }
            }
            
            // Create secure hash using HMAC SHA512
            String vnp_SecureHash = hmacSHA512(VNPayConfig.VNP_HASH_SECRET, hashData.toString());
            query.append("&vnp_SecureHash=").append(vnp_SecureHash);
            
            // Build final payment URL
            String paymentUrl = VNPayConfig.VNP_URL + "?" + query.toString();
            
            Log.d(TAG, "✓ Payment URL created successfully");
            Log.d(TAG, "TxnRef: " + txnRef + ", Amount: " + amount + " VND");
            
            return paymentUrl;
            
        } catch (Exception e) {
            Log.e(TAG, "✗ Error building payment URL: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Verify VNPay response signature
     * CRITICAL: KHÔNG encode params values khi verify! Giữ nguyên encoded như EasyBank
     * @param params Response parameters from VNPay (RAW, NOT DECODED)
     * @return true if signature is valid
     */
    public static boolean verifySignature(Map<String, String> params) {
        try {
            Log.d(TAG, "========== VNPay Signature Validation ==========");
            
            String vnp_SecureHash = params.get("vnp_SecureHash");
            if (vnp_SecureHash == null) {
                Log.e(TAG, "✗ Missing vnp_SecureHash in response");
                return false;
            }
            
            Log.d(TAG, "Params count: " + params.size());
            Log.d(TAG, "Response code: " + params.get("vnp_ResponseCode"));
            Log.d(TAG, "Amount: " + params.get("vnp_Amount"));
            Log.d(TAG, "VNPay hash: " + vnp_SecureHash);
            
            // Remove hash from params and use TreeMap for auto-sorting
            Map<String, String> fields = new TreeMap<>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key = entry.getKey();
                if (!"vnp_SecureHash".equals(key) && !"vnp_SecureHashType".equals(key)) {
                    // CRITICAL: Giữ nguyên value ENCODED, KHÔNG decode!
                    fields.put(key, entry.getValue());
                }
            }
            
            // Build hash data WITHOUT encoding (values already encoded)
            // Format: key1=encodedValue1&key2=encodedValue2
            StringBuilder hashData = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> entry : fields.entrySet()) {
                String fieldValue = entry.getValue();
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    if (!first) {
                        hashData.append('&');
                    }
                    // CRITICAL: KHÔNG encode lại! Giữ nguyên encoded value
                    hashData.append(entry.getKey()).append('=').append(fieldValue);
                    first = false;
                }
            }
            
            String queryString = hashData.toString();
            Log.d(TAG, "Query string: " + queryString);
            
            // Calculate expected hash
            String expectedHash = hmacSHA512(VNPayConfig.VNP_HASH_SECRET, queryString);
            Log.d(TAG, "Calculated: " + expectedHash);
            
            // Compare hashes (case-insensitive)
            boolean valid = expectedHash.equalsIgnoreCase(vnp_SecureHash);
            
            if (valid) {
                Log.d(TAG, "✅ SIGNATURE VALID");
            } else {
                Log.e(TAG, "❌ SIGNATURE INVALID");
                Log.e(TAG, "Expected: " + expectedHash);
                Log.e(TAG, "Received: " + vnp_SecureHash);
            }
            
            Log.d(TAG, "===============================================");
            
            return valid;
            
        } catch (Exception e) {
            Log.e(TAG, "✗ Error verifying signature: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Parse callback URL parameters
     * CRITICAL: KHÔNG decode values! VNPay tính signature trên dữ liệu ENCODED
     * @param url Callback URL from VNPay
     * @return Map of parameters (RAW, NOT DECODED)
     */
    public static Map<String, String> parseCallbackUrl(String url) {
        Map<String, String> params = new HashMap<>();
        
        try {
            // Extract query string
            int queryStart = url.indexOf('?');
            if (queryStart == -1) {
                Log.e(TAG, "No query string found in URL");
                return params;
            }
            
            String queryString = url.substring(queryStart + 1);
            Log.d(TAG, "Parsing query string from URL");
            
            // Parse parameters WITHOUT URL decoding
            // VNPay tính signature trên dữ liệu ENCODED (Nap+tien, %7C, %3A...)
            // Nếu decode thì signature sẽ không khớp
            String[] paramPairs = queryString.split("&");
            for (String pair : paramPairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    // CRITICAL: KHÔNG decode - giữ nguyên giá trị encoded!
                    params.put(keyValue[0], keyValue[1]);
                    Log.d(TAG, "Param: " + keyValue[0] + " = " + keyValue[1]);
                }
            }
            
            Log.d(TAG, "Parsed " + params.size() + " parameters");
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing callback URL: " + e.getMessage());
            e.printStackTrace();
        }
        
        return params;
    }
    
    /**
     * Check if transaction is successful
     * @param responseCode Response code from VNPay
     * @return true if responseCode is "00"
     */
    public static boolean isTransactionSuccess(String responseCode) {
        return "00".equals(responseCode);
    }
    
    /**
     * Calculate HMAC SHA512 hash
     */
    private static String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] hashBytes = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            StringBuilder result = new StringBuilder();
            for (byte b : hashBytes) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating HMAC SHA512: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Remove Vietnamese accents from string
     */
    public static String removeAccents(String text) {
        if (text == null) return "";
        
        String result = text;
        result = result.replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a");
        result = result.replaceAll("[ÀÁẠẢÃĂẰẮẶẲẴÂẦẤẬẨẪ]", "A");
        result = result.replaceAll("[èéẹẻẽêềếệểễ]", "e");
        result = result.replaceAll("[ÈÉẸẺẼÊỀẾỆỂỄ]", "E");
        result = result.replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o");
        result = result.replaceAll("[ÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠ]", "O");
        result = result.replaceAll("[ìíịỉĩ]", "i");
        result = result.replaceAll("[ÌÍỊỈĨ]", "I");
        result = result.replaceAll("[ùúụủũưừứựửữ]", "u");
        result = result.replaceAll("[ƯỪỨỰỬỮÙÚỤỦŨ]", "U");
        result = result.replaceAll("[ỳýỵỷỹ]", "y");
        result = result.replaceAll("[ỲÝỴỶỸ]", "Y");
        result = result.replaceAll("đ", "d");
        result = result.replaceAll("Đ", "D");
        
        return result;
    }
}
