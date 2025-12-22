package com.example.vibebank;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vibebank.ui.home.HomeActivity;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ElectricBillResultActivity extends AppCompatActivity {

    private ImageView btnClose;
    private TextView tvCustomerId, tvCustomerName, tvPeriod, tvConsumption, tvAmount, tvTime;
    private MaterialButton btnDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_electric_bill_result);

        initViews();
        displayResult();
        setupListeners();
    }

    private void initViews() {
        btnClose = findViewById(R.id.btnClose);
        tvCustomerId = findViewById(R.id.tvCustomerId);
        tvCustomerName = findViewById(R.id.tvCustomerName);
        tvPeriod = findViewById(R.id.tvPeriod);
        tvConsumption = findViewById(R.id.tvConsumption);
        tvAmount = findViewById(R.id.tvAmount);
        tvTime = findViewById(R.id.tvTime);
        btnDone = findViewById(R.id.btnDone);
    }

    private void displayResult() {
        Intent intent = getIntent();
        String customerId = intent.getStringExtra("CUSTOMER_ID");
        String customerName = intent.getStringExtra("CUSTOMER_NAME");
        String period = intent.getStringExtra("PERIOD");
        double amount = intent.getDoubleExtra("AMOUNT", 0);
        int consumption = intent.getIntExtra("CONSUMPTION", 0);

        tvCustomerId.setText(customerId);
        tvCustomerName.setText(customerName);
        tvPeriod.setText(period);
        tvConsumption.setText(consumption + " kWh");
        tvAmount.setText(formatMoney((long)amount) + " VND");

        // Display current time
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", new Locale("vi", "VN"));
        tvTime.setText(dateFormat.format(new Date()));
    }

    private void setupListeners() {
        btnClose.setOnClickListener(v -> navigateToHome());
        btnDone.setOnClickListener(v -> navigateToHome());
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private String formatMoney(long amount) {
        return NumberFormat.getInstance(new Locale("vi", "VN")).format(amount);
    }

    @Override
    public void onBackPressed() {
        navigateToHome();
    }
}
