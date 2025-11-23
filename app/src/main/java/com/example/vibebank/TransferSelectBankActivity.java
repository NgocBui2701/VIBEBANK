package com.example.vibebank;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class TransferSelectBankActivity extends AppCompatActivity {
    private ImageView btnBack;
    private LinearLayout btnBankMB, btnBankVibeBank, btnBankTechcombank;
    private LinearLayout btnBankVietcombank, btnBankBIDV, btnBankACB, btnBankTPBank;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_select_bank);

        // Initialize views
        btnBack = findViewById(R.id.btnBack);
        btnBankMB = findViewById(R.id.btnBankMB);
        btnBankVibeBank = findViewById(R.id.btnBankVibeBank);
        btnBankTechcombank = findViewById(R.id.btnBankTechcombank);
        btnBankVietcombank = findViewById(R.id.btnBankVietcombank);
        btnBankBIDV = findViewById(R.id.btnBankBIDV);
        btnBankACB = findViewById(R.id.btnBankACB);
        btnBankTPBank = findViewById(R.id.btnBankTPBank);

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Bank selection listeners
        btnBankMB.setOnClickListener(v -> selectBank("MB", "MB BANK"));
        btnBankVibeBank.setOnClickListener(v -> selectBank("VIBEBANK", "VIBEBANK"));
        btnBankTechcombank.setOnClickListener(v -> selectBank("TECHCOMBANK", "TECHCOMBANK"));
        btnBankVietcombank.setOnClickListener(v -> selectBank("VIETCOMBANK", "VIETCOMBANK"));
        btnBankBIDV.setOnClickListener(v -> selectBank("BIDV", "BIDV"));
        btnBankACB.setOnClickListener(v -> selectBank("ACB", "ACB"));
        btnBankTPBank.setOnClickListener(v -> selectBank("TPBANK", "TPBANK"));
    }

    private void selectBank(String bankCode, String bankName) {
        Intent intent = new Intent(this, TransferEnterAccountActivity.class);
        intent.putExtra("bankCode", bankCode);
        intent.putExtra("bankName", bankName);
        startActivity(intent);
    }
}
