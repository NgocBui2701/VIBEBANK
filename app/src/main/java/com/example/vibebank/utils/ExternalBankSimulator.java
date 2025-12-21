package com.example.vibebank.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * External Bank Account Simulator
 * Mock database for external bank accounts to simulate account lookup
 * 
 * Real bank BIN codes in Vietnam:
 * - Vietcombank (VCB): 970436
 * - Techcombank (TCB): 970407
 * - BIDV: 970418
 * - Agribank: 970405
 * - VietinBank: 970415
 * - MBBank: 970422
 * - Sacombank: 970403
 * - ACB: 970416
 * - VPBank: 970432
 * - TPBank: 970423
 */
public class ExternalBankSimulator {
    
    private static ExternalBankSimulator instance;
    
    // Map: BankBIN -> (AccountNumber -> AccountName)
    private final Map<String, Map<String, AccountInfo>> bankDatabase;
    
    // Map: BankBIN -> BankName
    private final Map<String, String> bankNames;
    
    public static class AccountInfo {
        public String accountName;
        public String bankCode;
        public String bankName;
        
        public AccountInfo(String accountName, String bankCode, String bankName) {
            this.accountName = accountName;
            this.bankCode = bankCode;
            this.bankName = bankName;
        }
    }
    
    private ExternalBankSimulator() {
        bankDatabase = new HashMap<>();
        bankNames = new HashMap<>();
        initializeMockData();
    }
    
    public static synchronized ExternalBankSimulator getInstance() {
        if (instance == null) {
            instance = new ExternalBankSimulator();
        }
        return instance;
    }
    
    /**
     * Initialize mock bank data for simulation
     */
    private void initializeMockData() {
        // Bank names
        bankNames.put("970436", "Vietcombank");
        bankNames.put("970407", "Techcombank");
        bankNames.put("970418", "BIDV");
        bankNames.put("970405", "Agribank");
        bankNames.put("970415", "VietinBank");
        bankNames.put("970422", "MBBank");
        bankNames.put("970403", "Sacombank");
        bankNames.put("970416", "ACB");
        bankNames.put("970432", "VPBank");
        bankNames.put("970423", "TPBank");
        bankNames.put("VIBEBANK", "VIBEBANK"); // Our internal bank
        
        // Vietcombank accounts
        Map<String, AccountInfo> vcbAccounts = new HashMap<>();
        vcbAccounts.put("1234567890", new AccountInfo("NGUYEN VAN A", "VCB", "Vietcombank"));
        vcbAccounts.put("9876543210", new AccountInfo("TRAN THI B", "VCB", "Vietcombank"));
        vcbAccounts.put("5555666677", new AccountInfo("LE VAN C", "VCB", "Vietcombank"));
        bankDatabase.put("970436", vcbAccounts);
        
        // Techcombank accounts
        Map<String, AccountInfo> tcbAccounts = new HashMap<>();
        tcbAccounts.put("1111222233", new AccountInfo("PHAM VAN D", "TCB", "Techcombank"));
        tcbAccounts.put("4444555566", new AccountInfo("HOANG THI E", "TCB", "Techcombank"));
        tcbAccounts.put("7777888899", new AccountInfo("DANG VAN F", "TCB", "Techcombank"));
        bankDatabase.put("970407", tcbAccounts);
        
        // BIDV accounts
        Map<String, AccountInfo> bidvAccounts = new HashMap<>();
        bidvAccounts.put("2222333344", new AccountInfo("NGUYEN THI G", "BIDV", "BIDV"));
        bidvAccounts.put("6666777788", new AccountInfo("TRAN VAN H", "BIDV", "BIDV"));
        bankDatabase.put("970418", bidvAccounts);
        
        // Agribank accounts
        Map<String, AccountInfo> agriAccounts = new HashMap<>();
        agriAccounts.put("3333444455", new AccountInfo("LE THI I", "Agribank", "Agribank"));
        agriAccounts.put("8888999900", new AccountInfo("PHAM VAN K", "Agribank", "Agribank"));
        bankDatabase.put("970405", agriAccounts);
        
        // VietinBank accounts
        Map<String, AccountInfo> vietinAccounts = new HashMap<>();
        vietinAccounts.put("1212343456", new AccountInfo("VO VAN L", "VietinBank", "VietinBank"));
        vietinAccounts.put("5656787878", new AccountInfo("BUI THI M", "VietinBank", "VietinBank"));
        bankDatabase.put("970415", vietinAccounts);
        
        // MBBank accounts
        Map<String, AccountInfo> mbAccounts = new HashMap<>();
        mbAccounts.put("9090121212", new AccountInfo("DO VAN N", "MBBank", "MBBank"));
        mbAccounts.put("3434565656", new AccountInfo("NGUYEN THI O", "MBBank", "MBBank"));
        mbAccounts.put("0375092732", new AccountInfo("NGUYEN VAN TEST", "MBBank", "MBBank"));
        bankDatabase.put("970422", mbAccounts);
    }
    
    /**
     * Find account name by BIN and account number
     * 
     * @param bankBIN Bank BIN code (6 digits)
     * @param accountNumber Account number
     * @return AccountInfo if found, null otherwise
     */
    public AccountInfo findAccountName(String bankBIN, String accountNumber) {
        if (bankBIN == null || accountNumber == null) {
            return null;
        }
        
        Map<String, AccountInfo> accounts = bankDatabase.get(bankBIN);
        if (accounts != null) {
            return accounts.get(accountNumber);
        }
        
        return null;
    }
    
    /**
     * Get bank name from BIN code
     */
    public String getBankName(String bankBIN) {
        return bankNames.getOrDefault(bankBIN, "Unknown Bank");
    }
    
    /**
     * Check if bank BIN exists
     */
    public boolean isBankSupported(String bankBIN) {
        return bankNames.containsKey(bankBIN);
    }
    
    /**
     * Check if this is internal VIBEBANK transfer
     */
    public boolean isInternalBank(String bankBIN) {
        return "VIBEBANK".equals(bankBIN);
    }
    
    /**
     * Generate random Vietnamese name for unknown accounts
     */
    public String getRandomVietnameseName(String accountNumber) {
        String[] firstNames = {
            "NGUYEN", "TRAN", "LE", "PHAM", "HOANG", "HUYNH", 
            "PHAN", "VU", "VO", "DANG", "BUI", "DO", "NGO", "DUONG"
        };
        
        String[] middleNames = {
            "VAN", "THI", "MINH", "THANH", "HONG", "ANH", "DUC", 
            "QUOC", "HUU", "KIM", "NGOC", "DINH", "BAO"
        };
        
        String[] lastNames = {
            "AN", "BINH", "CUONG", "DUNG", "HAI", "HIEU", "HUNG", 
            "KIET", "LINH", "LONG", "NAM", "PHUONG", "QUAN", "TAM", 
            "TUAN", "VINH", "YEN", "COT", "DUY", "HA"
        };
        
        // Use account number as seed for consistent name generation
        int seed = accountNumber.hashCode();
        Random random = new Random(seed);
        
        String firstName = firstNames[Math.abs(random.nextInt()) % firstNames.length];
        String middleName = middleNames[Math.abs(random.nextInt()) % middleNames.length];
        String lastName = lastNames[Math.abs(random.nextInt()) % lastNames.length];
        
        return firstName + " " + middleName + " " + lastName;
    }
}
