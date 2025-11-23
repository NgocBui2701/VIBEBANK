package com.example.vibebank;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class TransferEnterAccountActivity extends AppCompatActivity {
    private ImageView btnBack;
    private TextView txtBankName;
    private EditText edtAccountNumber;
    private MaterialButton btnContinue;

    private String bankCode;
    private String bankName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_enter_account);

        // Initialize views
        btnBack = findViewById(R.id.btnBack);
        txtBankName = findViewById(R.id.txtBankName);
        edtAccountNumber = findViewById(R.id.edtAccountNumber);
        btnContinue = findViewById(R.id.btnContinue);

        // Get bank info from intent
        bankCode = getIntent().getStringExtra("bankCode");
        bankName = getIntent().getStringExtra("bankName");

        if (bankName != null) {
            txtBankName.setText(bankName);
        }

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Enable/disable continue button based on input
        edtAccountNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnContinue.setEnabled(s.length() >= 6);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Continue button
        btnContinue.setOnClickListener(v -> {
            String accountNumber = edtAccountNumber.getText().toString().trim();
            
            if (accountNumber.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập số tài khoản", Toast.LENGTH_SHORT).show();
                return;
            }

            // Simulate account validation - in real app, call API here
            String accountName = "NGUYEN QUOC BAO"; // Mock data

            Intent intent = new Intent(this, TransferDetailsActivity.class);
            intent.putExtra("bank", bankName);
            intent.putExtra("accountNumber", accountNumber);
            intent.putExtra("accountName", accountName);
            startActivity(intent);
        });
    }
}
