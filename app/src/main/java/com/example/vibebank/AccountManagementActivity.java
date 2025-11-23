package com.example.vibebank;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

public class AccountManagementActivity extends AppCompatActivity {
    private static final String TAG = "AccountManagement";
    
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
    
    // Bottom button
    private MaterialButton btnDeposit;
    
    // State
    private int currentTab = 0; // 0: Payment, 1: Saving, 2: Credit
    private boolean isDepositVisible = true;
    
    // Sample data for different account types
    private final String[][] accountData = {
        // Payment account: {deposit, interestRate, monthlyProfit}
        {"5,000", "2,1", "105"},
        // Saving account
        {"50,000", "4,5", "2,250"},
        // Credit account
        {"10,000", "1,8", "180"}
    };

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

        initializeViews();
        setupListeners();
        updateTabSelection(0);
        updateContent();
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
        
        // Bottom button
        btnDeposit = findViewById(R.id.btnDeposit);
    }
    
    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnTabPayment.setOnClickListener(v -> {
            currentTab = 0;
            updateTabSelection(0);
            updateContent();
        });
        
        btnTabSaving.setOnClickListener(v -> {
            currentTab = 1;
            updateTabSelection(1);
            updateContent();
        });
        
        btnTabCredit.setOnClickListener(v -> {
            currentTab = 2;
            updateTabSelection(2);
            updateContent();
        });
        
        btnToggleDeposit.setOnClickListener(v -> {
            isDepositVisible = !isDepositVisible;
            updateDepositVisibility();
        });
        
        btnDeposit.setOnClickListener(v -> {
            String accountType = currentTab == 0 ? "thanh toán" : 
                               currentTab == 1 ? "tiết kiệm" : "thẻ credit";
            Toast.makeText(this, "Nạp tiền vào tài khoản " + accountType, 
                         Toast.LENGTH_SHORT).show();
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
        String[] data = accountData[currentTab];
        txtDeposit.setText(data[0]);
        txtInterestRate.setText(data[1]);
        txtMonthlyProfit.setText(data[2]);
        
        // Reset visibility to visible when switching tabs
        isDepositVisible = true;
        updateDepositVisibility();
    }
    
    private void updateDepositVisibility() {
        if (isDepositVisible) {
            txtDeposit.setText(accountData[currentTab][0]);
            btnToggleDeposit.setImageResource(R.drawable.ic_eye);
        } else {
            txtDeposit.setText("******");
            btnToggleDeposit.setImageResource(R.drawable.ic_eye_off);
        }
    }
}
