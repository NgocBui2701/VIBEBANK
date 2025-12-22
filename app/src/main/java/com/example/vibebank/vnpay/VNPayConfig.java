package com.example.vibebank.vnpay;

/**
 * VNPay Configuration for Sandbox Environment
 */
public class VNPayConfig {
    
    // VNPay Sandbox Credentials
    public static final String VNP_TMN_CODE = "3N4JYM8F";
    public static final String VNP_HASH_SECRET = "6G2LM45ZQAN6N5YQJDJL9TOLE84KS7I6";
    public static final String VNP_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    
    // API Version
    public static final String VNP_VERSION = "2.1.0";
    public static final String VNP_COMMAND = "pay";
    public static final String VNP_CURR_CODE = "VND";
    
    // Return URL - App will handle this via deep link
    public static final String VNP_RETURN_URL = "vibebank://vnpay-return";
    
    // Order Type Categories
    public static final String ORDER_TYPE_TOPUP = "topup";           // Nạp tiền điện thoại
    public static final String ORDER_TYPE_BILL_PAYMENT = "billpayment"; // Thanh toán hóa đơn
    public static final String ORDER_TYPE_FASHION = "fashion";       // Thời trang
    public static final String ORDER_TYPE_OTHER = "other";           // Khác
    
    // Locale
    public static final String LOCALE_VN = "vn";
    public static final String LOCALE_EN = "en";
    
    // Bank Codes (Optional - for direct bank selection)
    public static final String BANK_CODE_VNPAYQR = "VNPAYQR";  // QR Code
    public static final String BANK_CODE_VNBANK = "VNBANK";     // Thẻ ATM/Internet Banking
    public static final String BANK_CODE_INTCARD = "INTCARD";   // Thẻ quốc tế
}
