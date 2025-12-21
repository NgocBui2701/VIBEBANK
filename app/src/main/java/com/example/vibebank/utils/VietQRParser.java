package com.example.vibebank.utils;

import android.util.Log;

/**
 * VietQR Parser - EMVCo QR Code Standard
 * Parses real bank QR codes following the VietQR/EMVCo specification
 * Format: Tag (2 digits) - Length (2 digits) - Value (variable)
 */
public class VietQRParser {
    private static final String TAG = "VietQRParser";
    
    // EMVCo Tags
    private static final String TAG_MERCHANT_ACCOUNT = "38"; // Consumer Presented Mode
    private static final String TAG_TRANSACTION_AMOUNT = "54";
    private static final String TAG_ADDITIONAL_DATA = "62";
    private static final String TAG_CRC = "63";
    
    // Sub-tags for Merchant Account (Tag 38)
    private static final String SUBTAG_GUID = "00";
    private static final String SUBTAG_BANK_BIN = "01";
    private static final String SUBTAG_ACCOUNT_NUMBER = "02";
    
    // Sub-tags for Additional Data (Tag 62)
    private static final String SUBTAG_PURPOSE = "08";
    
    private String rawQRData;
    private String bankBIN;
    private String accountNumber;
    private String amount;
    private String description;
    private String guid;
    
    public VietQRParser(String qrData) {
        this.rawQRData = qrData;
        parse();
    }
    
    /**
     * Parse the QR code string following EMVCo standard
     */
    private void parse() {
        if (rawQRData == null || rawQRData.length() < 10) {
            Log.e(TAG, "Invalid QR data");
            return;
        }
        
        try {
            int index = 0;
            while (index < rawQRData.length() - 4) {
                // Read Tag (2 digits)
                String tag = rawQRData.substring(index, index + 2);
                index += 2;
                
                // Read Length (2 digits)
                int length = Integer.parseInt(rawQRData.substring(index, index + 2));
                index += 2;
                
                // Check bounds
                if (index + length > rawQRData.length()) {
                    Log.w(TAG, "Length exceeds data bounds at tag " + tag);
                    break;
                }
                
                // Read Value
                String value = rawQRData.substring(index, index + length);
                index += length;
                
                // Process based on tag
                switch (tag) {
                    case TAG_MERCHANT_ACCOUNT:
                        parseMerchantAccount(value);
                        break;
                    case TAG_TRANSACTION_AMOUNT:
                        this.amount = value;
                        break;
                    case TAG_ADDITIONAL_DATA:
                        parseAdditionalData(value);
                        break;
                    case TAG_CRC:
                        // CRC check - skip for simulation
                        break;
                }
            }
            
            Log.d(TAG, "Parsed VietQR - BIN: " + bankBIN + ", Account: " + accountNumber);
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing VietQR: " + e.getMessage());
        }
    }
    
    /**
     * Parse Merchant Account Information (Tag 38)
     */
    private void parseMerchantAccount(String data) {
        try {
            Log.d(TAG, "Parsing Merchant Account, data length: " + data.length());
            int index = 0;
            while (index < data.length() - 4) {
                String subTag = data.substring(index, index + 2);
                index += 2;
                
                int length = Integer.parseInt(data.substring(index, index + 2));
                index += 2;
                
                if (index + length > data.length()) {
                    Log.w(TAG, "Length overflow at subtag " + subTag);
                    break;
                }
                
                String value = data.substring(index, index + length);
                index += length;
                
                Log.d(TAG, "SubTag " + subTag + " = " + value);
                
                switch (subTag) {
                    case SUBTAG_GUID:
                        this.guid = value;
                        Log.d(TAG, "Set GUID: " + value);
                        break;
                    case SUBTAG_BANK_BIN:
                        // SubTag 01 may contain nested structure
                        // Try to extract 6-digit BIN from the value
                        parseNestedBankInfo(value);
                        break;
                    case SUBTAG_ACCOUNT_NUMBER:
                        // If we don't have account from nested structure, use this
                        if (this.accountNumber == null) {
                            this.accountNumber = value;
                            Log.d(TAG, "Set Account: " + value);
                        }
                        break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing merchant account: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Parse nested bank information from SubTag 01
     */
    private void parseNestedBankInfo(String data) {
        try {
            Log.d(TAG, "Parsing nested bank info: " + data);
            int index = 0;
            while (index < data.length() - 4) {
                String nestedTag = data.substring(index, index + 2);
                index += 2;
                
                int length = Integer.parseInt(data.substring(index, index + 2));
                index += 2;
                
                if (index + length > data.length()) break;
                
                String value = data.substring(index, index + length);
                index += length;
                
                Log.d(TAG, "Nested Tag " + nestedTag + " = " + value);
                
                // Tag 00 usually contains BIN (6 digits)
                if ("00".equals(nestedTag) && value.length() == 6) {
                    this.bankBIN = value;
                    Log.d(TAG, "Set BIN from nested: " + value);
                }
                // Tag 01 or 02 may contain account number
                else if (("01".equals(nestedTag) || "02".equals(nestedTag)) && value.length() >= 6) {
                    this.accountNumber = value;
                    Log.d(TAG, "Set Account from nested: " + value);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing nested bank info: " + e.getMessage());
        }
    }
    
    /**
     * Parse Additional Data Field (Tag 62)
     */
    private void parseAdditionalData(String data) {
        try {
            int index = 0;
            while (index < data.length() - 4) {
                String subTag = data.substring(index, index + 2);
                index += 2;
                
                int length = Integer.parseInt(data.substring(index, index + 2));
                index += 2;
                
                if (index + length > data.length()) break;
                
                String value = data.substring(index, index + length);
                index += length;
                
                if (subTag.equals(SUBTAG_PURPOSE)) {
                    this.description = value;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing additional data: " + e.getMessage());
        }
    }
    
    /**
     * Check if QR code is valid VietQR format
     */
    public boolean isValidVietQR() {
        Log.d(TAG, "Validation - BIN: " + bankBIN + " (len=" + (bankBIN != null ? bankBIN.length() : 0) + 
                   "), Account: " + accountNumber + " (len=" + (accountNumber != null ? accountNumber.length() : 0) + ")");
        
        boolean valid = bankBIN != null && accountNumber != null && 
               bankBIN.length() == 6 && accountNumber.length() >= 6;
        
        Log.d(TAG, "isValidVietQR: " + valid);
        return valid;
    }
    
    // Getters
    public String getBankBIN() {
        return bankBIN;
    }
    
    public String getAccountNumber() {
        return accountNumber;
    }
    
    public String getAmount() {
        return amount;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getGuid() {
        return guid;
    }
}
