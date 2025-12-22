package com.example.vibebank;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.Locale;

public class AccountManagementActivity extends AppCompatActivity {
    private static final String TAG = "AccountManagement";
    
    // Firebase
    private FirebaseFirestore db;
    private String currentUserId;
    
    // Header
    private ImageView btnBack;
    
    // Tabs
    private MaterialButton btnTabPayment;
    private MaterialButton btnTabSaving;
    private MaterialButton btnTabCredit;
    
    // Content
    private TextView txtDeposit;
    private TextView txtInterestRate;
    private TextView txtMonthlyProfit;
    private ImageView btnToggleDeposit;
    private ProgressBar progressBar;
    
    // State
    private int currentTab = 0; // 0: Payment, 1: Saving, 2: Credit
    private boolean isDepositVisible = true;
    
    // Data for 3 account types
    private double paymentBalance = 0.0;
    private double savingBalance = 0.0;
    private double creditLimit = 0.0;
    private double creditDebt = 0.0; // Công nợ thẻ credit
    
    private static final double PAYMENT_INTEREST = 0.01 / 12; // 1% năm = 0.083% tháng
    private static final double SAVING_INTEREST = 0.06 / 12;  // 6% năm = 0.5% tháng
    private static final double CREDIT_INTEREST = 0.18 / 12;  // 18% năm = 1.5% tháng (lãi nợ)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_account_management);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.accountManagement), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        
        // Get current user ID
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

        initializeViews();
        setupListeners();
        updateTabSelection(0);
        loadAccountData();
    }
    
    private void initializeViews() {
        // Header
        btnBack = findViewById(R.id.btnBack);
        
        // Tabs
        btnTabPayment = findViewById(R.id.btnTabPayment);
        btnTabSaving = findViewById(R.id.btnTabSaving);
        btnTabCredit = findViewById(R.id.btnTabCredit);
        
        // Content
        txtDeposit = findViewById(R.id.txtDeposit);
        txtInterestRate = findViewById(R.id.txtInterestRate);
        txtMonthlyProfit = findViewById(R.id.txtMonthlyProfit);
        btnToggleDeposit = findViewById(R.id.btnToggleDeposit);
        progressBar = findViewById(R.id.progressBar);
    }
    
    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnTabPayment.setOnClickListener(v -> {
            currentTab = 0;
            updateTabSelection(0);
            loadAccountData();
        });
        
        btnTabSaving.setOnClickListener(v -> {
            currentTab = 1;
            updateTabSelection(1);
            loadAccountData();
        });
        
        btnTabCredit.setOnClickListener(v -> {
            currentTab = 2;
            updateTabSelection(2);
            loadAccountData();
        });
        
        btnToggleDeposit.setOnClickListener(v -> {
            isDepositVisible = !isDepositVisible;
            updateDepositVisibility();
        });
    }
    
    private void updateTabSelection(int selectedTab) {
        // Reset all tabs
        btnTabPayment.setBackgroundTintList(getColorStateList(R.color.white));
        btnTabPayment.setTextColor(getColor(R.color.black));
        btnTabPayment.setStrokeColorResource(R.color.black);
        btnTabPayment.setStrokeWidth(2);
        
        btnTabSaving.setBackgroundTintList(getColorStateList(R.color.white));
        btnTabSaving.setTextColor(getColor(R.color.black));
        btnTabSaving.setStrokeColorResource(R.color.black);
        btnTabSaving.setStrokeWidth(2);
        
        btnTabCredit.setBackgroundTintList(getColorStateList(R.color.white));
        btnTabCredit.setTextColor(getColor(R.color.black));
        btnTabCredit.setStrokeColorResource(R.color.black);
        btnTabCredit.setStrokeWidth(2);
        
        // Set selected tab
        MaterialButton selectedButton = selectedTab == 0 ? btnTabPayment : 
                                       selectedTab == 1 ? btnTabSaving : btnTabCredit;
        selectedButton.setBackgroundTintList(getColorStateList(R.color.black));
        selectedButton.setTextColor(getColor(R.color.white));
        selectedButton.setStrokeWidth(0);
    }
    
    private void updateContent() {
        double displayAmount = 0.0;
        double interestRate = 0.0;
        double monthlyProfit = 0.0;
        
        switch (currentTab) {
            case 0: // Payment Account
                displayAmount = paymentBalance;
                interestRate = PAYMENT_INTEREST;
                monthlyProfit = paymentBalance * interestRate;
                break;
                
            case 1: // Saving Account
                displayAmount = savingBalance;
                interestRate = SAVING_INTEREST;
                monthlyProfit = savingBalance * interestRate;
                break;
                
            case 2: // Credit Card
                displayAmount = creditLimit - creditDebt; // Hạn mức còn lại
                interestRate = CREDIT_INTEREST;
                monthlyProfit = creditDebt * interestRate; // Lãi phải trả trên công nợ
                break;
        }
        
        txtInterestRate.setText(formatPercent(interestRate * 12)); // Hiển thị % năm
        txtMonthlyProfit.setText(formatMoney(monthlyProfit));
        
        // Reset visibility to visible when switching tabs
        isDepositVisible = true;
        updateDepositVisibility();
    }
    
    private void updateDepositVisibility() {
        double displayAmount = 0.0;
        
        switch (currentTab) {
            case 0:
                displayAmount = paymentBalance;
                break;
            case 1:
                displayAmount = savingBalance;
                break;
            case 2:
                displayAmount = creditLimit - creditDebt;
                break;
        }
        
        if (isDepositVisible) {
            txtDeposit.setText(formatMoney(displayAmount));
            btnToggleDeposit.setImageResource(R.drawable.ic_eye);
        } else {
            txtDeposit.setText("******");
            btnToggleDeposit.setImageResource(R.drawable.ic_eye_off);
        }
    }
    
    private void loadAccountData() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        
        switch (currentTab) {
            case 0:
                loadPaymentAccount();
                break;
            case 1:
                loadSavingAccount();
                break;
            case 2:
                loadCreditCard();
                break;
        }
    }
    
    private void loadPaymentAccount() {
        db.collection("accounts")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    
                    if (documentSnapshot.exists()) {
                        Double balanceValue = documentSnapshot.getDouble("balance");
                        paymentBalance = balanceValue != null ? balanceValue : 0.0;
                        updateContent();
                    } else {
                        Toast.makeText(this, "Chưa có tài khoản thanh toán", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void loadSavingAccount() {
        db.collection("savings")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    
                    if (documentSnapshot.exists()) {
                        Double balanceValue = documentSnapshot.getDouble("balance");
                        savingBalance = balanceValue != null ? balanceValue : 0.0;
                        updateContent();
                    } else {
                        // Tạo tài khoản tiết kiệm mới với số dư 0
                        savingBalance = 0.0;
                        updateContent();
                    }
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void loadCreditCard() {
        db.collection("credit_cards")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    
                    if (documentSnapshot.exists()) {
                        Double limitValue = documentSnapshot.getDouble("credit_limit");
                        Double debtValue = documentSnapshot.getDouble("debt");
                        
                        creditLimit = limitValue != null ? limitValue : 10000000.0; // 10 triệu default
                        creditDebt = debtValue != null ? debtValue : 0.0;
                        updateContent();
                    } else {
                        // Tạo thẻ credit mới với hạn mức 10 triệu
                        creditLimit = 10000000.0;
                        creditDebt = 0.0;
                        updateContent();
                    }
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private String formatMoney(double amount) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        formatter.setMaximumFractionDigits(0);
        return formatter.format(amount);
    }
    
    private String formatPercent(double rate) {
        return String.format(Locale.US, "%.1f", rate * 100);
    }
}
