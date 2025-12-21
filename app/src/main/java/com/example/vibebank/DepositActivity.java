package com.example.vibebank;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class DepositActivity extends AppCompatActivity {
    
    private FirebaseFirestore db;
    private String currentUserId;
    private int accountType; // 0: Payment, 1: Saving, 2: Credit
    private double paymentBalance = 0.0;
    private double creditDebt = 0.0;
    
    private ImageView btnBack;
    private TextView tvTitle, tvSubtitle, tvPaymentBalance;
    private EditText edtAmount;
    private Button btnConfirm;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deposit);
        
        db = FirebaseFirestore.getInstance();
        
        // Get user ID
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        } else {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            currentUserId = prefs.getString("current_user_id", null);
        }
        
        if (currentUserId == null) {
            Toast.makeText(this, "Lỗi phiên đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Get account type from intent
        accountType = getIntent().getIntExtra("accountType", 0);
        
        initializeViews();
        setupUI();
        loadPaymentBalance();
    }
    
    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvPaymentBalance = findViewById(R.id.tvPaymentBalance);
        edtAmount = findViewById(R.id.edtAmount);
        btnConfirm = findViewById(R.id.btnConfirm);
        
        btnBack.setOnClickListener(v -> finish());
        btnConfirm.setOnClickListener(v -> processDeposit());
        
        // Format amount input
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
                        double parsed = Double.parseDouble(cleanString);
                        String formatted = formatMoney(parsed);
                        current = formatted;
                        edtAmount.setText(formatted);
                        edtAmount.setSelection(formatted.length());
                    }
                    
                    edtAmount.addTextChangedListener(this);
                }
            }
        });
    }
    
    private void setupUI() {
        switch (accountType) {
            case 0: // Payment
                tvTitle.setText("Nạp tiền");
                tvSubtitle.setText("Nạp tiền vào tài khoản thanh toán");
                tvPaymentBalance.setVisibility(View.GONE);
                btnConfirm.setText("Xác nhận nạp tiền");
                break;
                
            case 1: // Saving
                tvTitle.setText("Gửi tiết kiệm");
                tvSubtitle.setText("Chuyển từ tài khoản thanh toán sang tài khoản tiết kiệm");
                tvPaymentBalance.setVisibility(View.VISIBLE);
                btnConfirm.setText("Xác nhận gửi");
                break;
                
            case 2: // Credit
                tvTitle.setText("Trả nợ thẻ");
                tvSubtitle.setText("Trả nợ thẻ credit từ tài khoản thanh toán");
                tvPaymentBalance.setVisibility(View.VISIBLE);
                btnConfirm.setText("Xác nhận trả nợ");
                loadCreditDebt();
                break;
        }
    }
    
    private void loadPaymentBalance() {
        if (accountType == 0) return; // Payment không cần load
        
        db.collection("accounts")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Double balance = doc.getDouble("balance");
                        paymentBalance = balance != null ? balance : 0.0;
                        tvPaymentBalance.setText("Số dư TK thanh toán: " + formatMoney(paymentBalance) + " VND");
                    }
                });
    }
    
    private void loadCreditDebt() {
        db.collection("credit_cards")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Double debt = doc.getDouble("debt");
                        creditDebt = debt != null ? debt : 0.0;
                    }
                });
    }
    
    private void processDeposit() {
        String amountStr = edtAmount.getText().toString().trim();
        
        if (amountStr.isEmpty()) {
            edtAmount.setError("Vui lòng nhập số tiền");
            return;
        }
        
        String cleanAmount = amountStr.replace(".", "").replace(",", "");
        double amount = Double.parseDouble(cleanAmount);
        
        if (amount <= 0) {
            edtAmount.setError("Số tiền phải lớn hơn 0");
            return;
        }
        
        switch (accountType) {
            case 0:
                depositToPayment(amount);
                break;
            case 1:
                transferToSaving(amount);
                break;
            case 2:
                payCredit(amount);
                break;
        }
    }
    
    private void depositToPayment(double amount) {
        // Nạp tiền vào TK thanh toán (giả lập - trong thực tế cần kết nối payment gateway)
        DocumentReference accountRef = db.collection("accounts").document(currentUserId);
        
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            Double currentBalance = transaction.get(accountRef).getDouble("balance");
            if (currentBalance == null) currentBalance = 0.0;
            
            double newBalance = currentBalance + amount;
            transaction.update(accountRef, "balance", newBalance);
            
            // Log transaction
            String transId = UUID.randomUUID().toString();
            Map<String, Object> log = new HashMap<>();
            log.put("userId", currentUserId);
            log.put("type", "RECEIVED");
            log.put("amount", amount);
            log.put("content", "Nạp tiền vào tài khoản");
            log.put("relatedAccountName", "ATM");
            log.put("timestamp", Timestamp.now());
            log.put("transactionId", transId);
            
            DocumentReference logRef = accountRef.collection("transactions").document(transId);
            transaction.set(logRef, log);
            
            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Nạp tiền thành công!", Toast.LENGTH_SHORT).show();
            
            // Return to AccountManagement
            Intent intent = new Intent(this, AccountManagementActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
    
    private void transferToSaving(double amount) {
        if (amount > paymentBalance) {
            Toast.makeText(this, "Số dư TK thanh toán không đủ", Toast.LENGTH_SHORT).show();
            return;
        }
        
        DocumentReference paymentRef = db.collection("accounts").document(currentUserId);
        DocumentReference savingRef = db.collection("savings").document(currentUserId);
        
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            // Read payment balance
            Double paymentBal = transaction.get(paymentRef).getDouble("balance");
            if (paymentBal == null) paymentBal = 0.0;
            
            if (paymentBal < amount) {
                throw new FirebaseFirestoreException("Insufficient balance", FirebaseFirestoreException.Code.ABORTED);
            }
            
            // Read saving balance
            Double savingBal = 0.0;
            try {
                savingBal = transaction.get(savingRef).getDouble("balance");
                if (savingBal == null) savingBal = 0.0;
            } catch (Exception e) {
                // Document doesn't exist yet
            }
            
            // Update balances
            transaction.update(paymentRef, "balance", paymentBal - amount);
            
            if (savingBal == 0.0) {
                // Create new saving account
                Map<String, Object> savingData = new HashMap<>();
                savingData.put("userId", currentUserId);
                savingData.put("balance", amount);
                savingData.put("createdAt", Timestamp.now());
                transaction.set(savingRef, savingData);
            } else {
                transaction.update(savingRef, "balance", savingBal + amount);
            }
            
            // Log transaction
            String transId = UUID.randomUUID().toString();
            Map<String, Object> log = new HashMap<>();
            log.put("userId", currentUserId);
            log.put("type", "SENT");
            log.put("amount", amount);
            log.put("content", "Gửi tiết kiệm");
            log.put("relatedAccountName", "Tài khoản tiết kiệm");
            log.put("timestamp", Timestamp.now());
            log.put("transactionId", transId);
            
            DocumentReference logRef = paymentRef.collection("transactions").document(transId);
            transaction.set(logRef, log);
            
            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Gửi tiết kiệm thành công!", Toast.LENGTH_SHORT).show();
            
            Intent intent = new Intent(this, AccountManagementActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
    
    private void payCredit(double amount) {
        if (amount > paymentBalance) {
            Toast.makeText(this, "Số dư TK thanh toán không đủ", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (amount > creditDebt) {
            Toast.makeText(this, "Số tiền trả vượt quá công nợ", Toast.LENGTH_SHORT).show();
            return;
        }
        
        DocumentReference paymentRef = db.collection("accounts").document(currentUserId);
        DocumentReference creditRef = db.collection("credit_cards").document(currentUserId);
        
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            // Read payment balance
            Double paymentBal = transaction.get(paymentRef).getDouble("balance");
            if (paymentBal == null) paymentBal = 0.0;
            
            if (paymentBal < amount) {
                throw new FirebaseFirestoreException("Insufficient balance", FirebaseFirestoreException.Code.ABORTED);
            }
            
            // Read credit debt
            Double debt = transaction.get(creditRef).getDouble("debt");
            if (debt == null) debt = 0.0;
            
            // Update balances
            transaction.update(paymentRef, "balance", paymentBal - amount);
            transaction.update(creditRef, "debt", debt - amount);
            
            // Log transaction
            String transId = UUID.randomUUID().toString();
            Map<String, Object> log = new HashMap<>();
            log.put("userId", currentUserId);
            log.put("type", "SENT");
            log.put("amount", amount);
            log.put("content", "Trả nợ thẻ tín dụng");
            log.put("relatedAccountName", "Thẻ tín dụng");
            log.put("timestamp", Timestamp.now());
            log.put("transactionId", transId);
            
            DocumentReference logRef = paymentRef.collection("transactions").document(transId);
            transaction.set(logRef, log);
            
            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Trả nợ thành công!", Toast.LENGTH_SHORT).show();
            
            Intent intent = new Intent(this, AccountManagementActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
    
    private String formatMoney(double amount) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        formatter.setMaximumFractionDigits(0);
        return formatter.format(amount);
    }
}
