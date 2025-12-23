package com.example.vibebank;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.vibebank.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class WithdrawCodeActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView txtAccountNumber, txtBalance;
    private EditText edtAmount, edtPin;
    private MaterialButton btn500k, btn1m, btn2m, btn3m, btn5m, btn10m;
    private MaterialButton btnGenerateCode, btnViewHistory;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseFirestore db;
    private String currentUserId;
    private SessionManager sessionManager;
    
    // Account data
    private double currentBalance = 0;
    private String accountNumber = "";
    private final String DEFAULT_PIN = "123456"; // Demo PIN

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withdraw_code);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);
        currentUserId = sessionManager.getCurrentUserId();
        
        // Fallback: thử lấy từ FirebaseAuth nếu chưa có trong session
        if (currentUserId == null || currentUserId.isEmpty()) {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                currentUserId = auth.getCurrentUser().getUid();
            }
        }

        initViews();
        setupListeners();
        loadAccountInfo();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        txtAccountNumber = findViewById(R.id.txtAccountNumber);
        txtBalance = findViewById(R.id.txtBalance);
        edtAmount = findViewById(R.id.edtAmount);
        edtPin = findViewById(R.id.edtPin);

        // Quick amount buttons
        btn500k = findViewById(R.id.btn500k);
        btn1m = findViewById(R.id.btn1m);
        btn2m = findViewById(R.id.btn2m);
        btn3m = findViewById(R.id.btn3m);
        btn5m = findViewById(R.id.btn5m);
        btn10m = findViewById(R.id.btn10m);

        btnGenerateCode = findViewById(R.id.btnGenerateCode);
        btnViewHistory = findViewById(R.id.btnViewHistory);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // Quick amount buttons
        btn500k.setOnClickListener(v -> setAmount(500000));
        btn1m.setOnClickListener(v -> setAmount(1000000));
        btn2m.setOnClickListener(v -> setAmount(2000000));
        btn3m.setOnClickListener(v -> setAmount(3000000));
        btn5m.setOnClickListener(v -> setAmount(5000000));
        btn10m.setOnClickListener(v -> setAmount(10000000));

        // Amount input formatting
        edtAmount.addTextChangedListener(new TextWatcher() {
            private String current = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(current)) {
                    edtAmount.removeTextChangedListener(this);

                    String cleanString = s.toString().replaceAll("[,.]", "");
                    
                    if (!cleanString.isEmpty()) {
                        try {
                            long parsed = Long.parseLong(cleanString);
                            String formatted = NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(parsed);
                            current = formatted;
                            edtAmount.setText(formatted);
                            edtAmount.setSelection(formatted.length());
                        } catch (NumberFormatException e) {
                            edtAmount.setText("");
                        }
                    }

                    edtAmount.addTextChangedListener(this);
                }
            }
        });

        // Generate code button
        btnGenerateCode.setOnClickListener(v -> validateAndGenerateCode());
        
        // View history button
        btnViewHistory.setOnClickListener(v -> {
            android.util.Log.d("WithdrawCodeActivity", "View History button clicked");
            Intent intent = new Intent(WithdrawCodeActivity.this, WithdrawCodeHistoryActivity.class);
            startActivity(intent);
        });
    }

    private void loadAccountInfo() {
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Lấy account number từ session trước
        accountNumber = sessionManager.getAccountNumber();
        
        if (accountNumber != null && !accountNumber.isEmpty()) {
            // Format account number: 1234567890 -> 1234 5678 90
            String formatted = accountNumber.replaceAll("(.{4})", "$1 ").trim();
            txtAccountNumber.setText(formatted);
        } else {
            // Nếu chưa có trong session, lấy từ Firestore và lưu vào session
            loadAccountNumberFromFirestore();
        }

        // Load balance from accounts collection (cần realtime nên vẫn query)
        db.collection("accounts").document(currentUserId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Double balance = documentSnapshot.getDouble("balance");
                    if (balance != null) {
                        currentBalance = balance;
                        txtBalance.setText("Số dư: " + formatCurrency((long)currentBalance) + " đ");
                    }
                } else {
                    txtBalance.setText("Số dư: 0 đ");
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Lỗi tải thông tin tài khoản", Toast.LENGTH_SHORT).show();
            });
    }
    
    private void loadAccountNumberFromFirestore() {
        // Thử lấy từ users collection trước
        db.collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener(userDoc -> {
                if (userDoc.exists()) {
                    accountNumber = userDoc.getString("account_number");
                    if (accountNumber == null) {
                        accountNumber = userDoc.getString("accountNumber");
                    }
                }
                
                if (accountNumber != null && !accountNumber.isEmpty()) {
                    // Lưu vào session
                    sessionManager.saveAccountNumber(accountNumber);
                    // Format và hiển thị
                    String formatted = accountNumber.replaceAll("(.{4})", "$1 ").trim();
                    txtAccountNumber.setText(formatted);
                } else {
                    // Thử lấy từ accounts collection
                    loadAccountNumberFromAccounts();
                }
            })
            .addOnFailureListener(e -> {
                // Nếu lỗi, thử lấy từ accounts collection
                loadAccountNumberFromAccounts();
            });
    }

    private void loadAccountNumberFromAccounts() {
        db.collection("accounts").document(currentUserId)
            .get()
            .addOnSuccessListener(accountDoc -> {
                if (accountDoc.exists()) {
                    accountNumber = accountDoc.getString("account_number");
                    if (accountNumber == null) {
                        accountNumber = accountDoc.getString("accountNumber");
                    }
                }
                
                if (accountNumber == null || accountNumber.isEmpty()) {
                    // Generate account number based on userId
                    accountNumber = "VB" + currentUserId.substring(0, Math.min(10, currentUserId.length())).toUpperCase();
                }
                
                // Lưu vào session để lần sau không cần query
                sessionManager.saveAccountNumber(accountNumber);
                
                // Format and display
                String formatted = accountNumber.replaceAll("(.{4})", "$1 ").trim();
                txtAccountNumber.setText(formatted);
            });
    }

    private void setAmount(long amount) {
        edtAmount.setText(NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(amount));
    }

    private void validateAndGenerateCode() {
        String amountStr = edtAmount.getText().toString().replaceAll("[,.]", "");
        String pin = edtPin.getText().toString().trim();

        // Validate amount
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            edtAmount.requestFocus();
            return;
        }

        long amount = Long.parseLong(amountStr);

        if (amount < 50000) {
            Toast.makeText(this, "Số tiền rút tối thiểu là 50.000 đ", Toast.LENGTH_SHORT).show();
            edtAmount.requestFocus();
            return;
        }

        if (amount > 20000000) {
            Toast.makeText(this, "Số tiền rút tối đa là 20.000.000 đ/lần", Toast.LENGTH_SHORT).show();
            edtAmount.requestFocus();
            return;
        }

        if (amount % 50000 != 0) {
            Toast.makeText(this, "Số tiền phải là bội số của 50.000 đ", Toast.LENGTH_SHORT).show();
            edtAmount.requestFocus();
            return;
        }

        if (amount > currentBalance) {
            Toast.makeText(this, "Số dư không đủ", Toast.LENGTH_SHORT).show();
            edtAmount.requestFocus();
            return;
        }

        // Validate PIN
        if (pin.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập mã PIN", Toast.LENGTH_SHORT).show();
            edtPin.requestFocus();
            return;
        }

        if (pin.length() != 6) {
            Toast.makeText(this, "Mã PIN phải có 6 chữ số", Toast.LENGTH_SHORT).show();
            edtPin.requestFocus();
            return;
        }

        if (!pin.equals(DEFAULT_PIN)) {
            Toast.makeText(this, "Mã PIN không đúng", Toast.LENGTH_SHORT).show();
            edtPin.requestFocus();
            return;
        }

        // Generate code and navigate to result
        generateCode(amount);
    }

    private void generateCode(long amount) {
        btnGenerateCode.setEnabled(false);
        progressBar.setVisibility(android.view.View.VISIBLE);

        // Generate random 6-digit code
        String code = String.format("%06d", (int)(Math.random() * 1000000));

        // Calculate expiry time (24 hours from now)
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, 24);
        Timestamp expiryTime = new Timestamp(calendar.getTime());

        // Create withdrawal code document
        Map<String, Object> withdrawalCode = new HashMap<>();
        withdrawalCode.put("userId", currentUserId);
        withdrawalCode.put("code", code);
        withdrawalCode.put("amount", amount);
        withdrawalCode.put("accountNumber", accountNumber);
        withdrawalCode.put("status", "ACTIVE"); // ACTIVE, USED, EXPIRED
        withdrawalCode.put("createdAt", Timestamp.now());
        withdrawalCode.put("expiryTime", expiryTime);
        withdrawalCode.put("usedAt", null);

        DocumentReference accountRef = db.collection("accounts").document(currentUserId);
        
        // Use transaction to deduct balance and save withdrawal code
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            // Read current balance
            Double balance = transaction.get(accountRef).getDouble("balance");
            if (balance == null) balance = 0.0;
            
            // Deduct balance
            transaction.update(accountRef, "balance", balance - amount);
            
            // Save withdrawal code to Firestore
            DocumentReference codeRef = db.collection("withdrawal_codes").document();
            transaction.set(codeRef, withdrawalCode);
            
            // Log transaction
            String transId = UUID.randomUUID().toString();
            Map<String, Object> log = new HashMap<>();
            log.put("userId", currentUserId);
            log.put("type", "SENT");
            log.put("amount", amount);
            log.put("content", "Rút tiền bằng mã ATM");
            log.put("relatedAccountName", "ATM");
            log.put("timestamp", Timestamp.now());
            log.put("transactionId", transId);
            
            DocumentReference logRef = accountRef.collection("transactions").document(transId);
            transaction.set(logRef, log);
            
            return null;
        }).addOnSuccessListener(aVoid -> {
            progressBar.setVisibility(android.view.View.GONE);
            
            // Navigate to result screen
            Intent intent = new Intent(this, WithdrawCodeResultActivity.class);
            intent.putExtra("WITHDRAW_CODE", code);
            intent.putExtra("AMOUNT", amount);
            intent.putExtra("ACCOUNT_NUMBER", accountNumber);
            intent.putExtra("EXPIRY_TIME", expiryTime.toDate().getTime());
            startActivity(intent);
            finish();
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(android.view.View.GONE);
            btnGenerateCode.setEnabled(true);
            Toast.makeText(this, "Lỗi tạo mã rút tiền: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private String formatCurrency(long amount) {
        return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(amount);
    }
}
