package com.example.vibebank;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class TransferActivity extends AppCompatActivity {
    private ImageView btnBack;
    private EditText edtSearch;
    private LinearLayout btnNewRecipient;
    private LinearLayout btnRecipient1, btnRecipient2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);

        // Initialize views
        btnBack = findViewById(R.id.btnBack);
        edtSearch = findViewById(R.id.edtSearch);
        btnNewRecipient = findViewById(R.id.btnNewRecipient);
        btnRecipient1 = findViewById(R.id.btnRecipient1);
        btnRecipient2 = findViewById(R.id.btnRecipient2);

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // New recipient button
        btnNewRecipient.setOnClickListener(v -> {
            Intent intent = new Intent(this, TransferSelectBankActivity.class);
            startActivity(intent);
        });

        // Saved recipients
        btnRecipient1.setOnClickListener(v -> {
            Intent intent = new Intent(this, TransferDetailsActivity.class);
            intent.putExtra("bank", "VIBEBANK");
            intent.putExtra("accountNumber", "0365349666");
            intent.putExtra("accountName", "BUI THI BICH NGOC");
            startActivity(intent);
        });

        btnRecipient2.setOnClickListener(v -> {
            Intent intent = new Intent(this, TransferDetailsActivity.class);
            intent.putExtra("bank", "VIBEBANK");
            intent.putExtra("accountNumber", "0365349666");
            intent.putExtra("accountName", "NGUYEN QUOC BAO");
            startActivity(intent);
        });

        // Search functionality
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Implement search filter here
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
}
