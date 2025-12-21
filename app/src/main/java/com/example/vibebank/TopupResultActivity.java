package com.example.vibebank;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vibebank.utils.TopupMockService;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.Locale;

public class TopupResultActivity extends AppCompatActivity {

    private TextView tvPhoneNumber, tvCarrierName, tvPackageName, tvAmount;
    private MaterialButton btnBackToHome;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topup_result);

        initViews();
        displayResult();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber);
        tvCarrierName = findViewById(R.id.tvCarrierName);
        tvPackageName = findViewById(R.id.tvPackageName);
        tvAmount = findViewById(R.id.tvAmount);
        btnBackToHome = findViewById(R.id.btnBackToHome);
    }

    private void displayResult() {
        String phoneNumber = getIntent().getStringExtra("phoneNumber");
        String packageName = getIntent().getStringExtra("packageName");
        long amount = getIntent().getLongExtra("amount", 0);

        if (phoneNumber != null) {
            String formattedPhone = TopupMockService.formatPhoneNumber(phoneNumber);
            tvPhoneNumber.setText(formattedPhone);
            
            String carrier = TopupMockService.getCarrierName(phoneNumber);
            tvCarrierName.setText(carrier);
        }

        if (packageName != null) {
            tvPackageName.setText(packageName);
        }

        tvAmount.setText(formatMoney(amount) + " VND");
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> backToHome());
        btnBackToHome.setOnClickListener(v -> backToHome());
    }

    private void backToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        backToHome();
    }

    private String formatMoney(long amount) {
        return NumberFormat.getInstance(new Locale("vi", "VN")).format(amount);
    }
}
