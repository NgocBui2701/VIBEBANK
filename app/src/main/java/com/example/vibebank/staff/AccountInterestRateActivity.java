package com.example.vibebank.staff;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vibebank.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AccountInterestRateActivity extends AppCompatActivity {

    private TextInputEditText edtPaymentRate, edtSavingRate, edtCreditRate;
    private MaterialButton btnSave;
    private ImageView btnBack;
    private FirebaseFirestore db;

    // Default rates
    private static final double DEFAULT_PAYMENT_RATE = 0.01; // 1% năm
    private static final double DEFAULT_SAVING_RATE = 0.06;  // 6% năm
    private static final double DEFAULT_CREDIT_RATE = 0.18;  // 18% năm

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_interest_rate);

        db = FirebaseFirestore.getInstance();

        initViews();
        loadCurrentRates();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        edtPaymentRate = findViewById(R.id.edtPaymentRate);
        edtSavingRate = findViewById(R.id.edtSavingRate);
        edtCreditRate = findViewById(R.id.edtCreditRate);
        btnSave = findViewById(R.id.btnSave);
    }

    private void loadCurrentRates() {
        db.collection("settings").document("interest_rates")
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Double paymentRate = doc.getDouble("payment");
                        Double savingRate = doc.getDouble("saving");
                        Double creditRate = doc.getDouble("credit");

                        edtPaymentRate.setText(String.format("%.2f", 
                                (paymentRate != null ? paymentRate : DEFAULT_PAYMENT_RATE) * 100));
                        edtSavingRate.setText(String.format("%.2f", 
                                (savingRate != null ? savingRate : DEFAULT_SAVING_RATE) * 100));
                        edtCreditRate.setText(String.format("%.2f", 
                                (creditRate != null ? creditRate : DEFAULT_CREDIT_RATE) * 100));
                    } else {
                        // Set default values
                        edtPaymentRate.setText(String.format("%.2f", DEFAULT_PAYMENT_RATE * 100));
                        edtSavingRate.setText(String.format("%.2f", DEFAULT_SAVING_RATE * 100));
                        edtCreditRate.setText(String.format("%.2f", DEFAULT_CREDIT_RATE * 100));
                    }
                });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveRates());
    }

    private void saveRates() {
        try {
            String paymentStr = edtPaymentRate.getText().toString().replace(",", ".").trim();
            String savingStr = edtSavingRate.getText().toString().replace(",", ".").trim();
            String creditStr = edtCreditRate.getText().toString().replace(",", ".").trim();
            
            if (paymentStr.isEmpty() || savingStr.isEmpty() || creditStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ lãi suất", Toast.LENGTH_SHORT).show();
                return;
            }
            
            double paymentRate = Double.parseDouble(paymentStr) / 100;
            double savingRate = Double.parseDouble(savingStr) / 100;
            double creditRate = Double.parseDouble(creditStr) / 100;

            Map<String, Object> rates = new HashMap<>();
            rates.put("payment", paymentRate);
            rates.put("saving", savingRate);
            rates.put("credit", creditRate);
            rates.put("updated_at", com.google.firebase.Timestamp.now());

            db.collection("settings").document("interest_rates")
                    .set(rates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Cập nhật lãi suất thành công", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Lãi suất không hợp lệ", Toast.LENGTH_SHORT).show();
        }
    }
}

