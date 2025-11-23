package com.example.vibebank;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class TransferDetailsActivity extends AppCompatActivity {
    private ImageView btnBack;
    private TextView txtRecipientBank, txtRecipientAccount, txtRecipientName;
    private CheckBox cbSaveRecipient;
    private EditText edtAmount, edtMessage;
    private MaterialButton btnTransfer;

    private String bank;
    private String accountNumber;
    private String accountName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_details);

        // Initialize views
        btnBack = findViewById(R.id.btnBack);
        txtRecipientBank = findViewById(R.id.txtRecipientBank);
        txtRecipientAccount = findViewById(R.id.txtRecipientAccount);
        txtRecipientName = findViewById(R.id.txtRecipientName);
        cbSaveRecipient = findViewById(R.id.cbSaveRecipient);
        edtAmount = findViewById(R.id.edtAmount);
        edtMessage = findViewById(R.id.edtMessage);
        btnTransfer = findViewById(R.id.btnTransfer);

        // Get recipient info from intent
        bank = getIntent().getStringExtra("bank");
        accountNumber = getIntent().getStringExtra("accountNumber");
        accountName = getIntent().getStringExtra("accountName");

        // Display recipient info
        if (bank != null) txtRecipientBank.setText(bank);
        if (accountNumber != null) txtRecipientAccount.setText(accountNumber);
        if (accountName != null) txtRecipientName.setText(accountName);

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Enable/disable transfer button based on amount input
        edtAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnTransfer.setEnabled(s.length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Transfer button
        btnTransfer.setOnClickListener(v -> {
            String amount = edtAmount.getText().toString().trim();
            String message = edtMessage.getText().toString().trim();
            boolean saveRecipient = cbSaveRecipient.isChecked();

            if (amount.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
                return;
            }

            // Simulate transfer processing
            processTransfer(amount, message, saveRecipient);
        });
    }

    private void processTransfer(String amount, String message, boolean saveRecipient) {
        // In real app, call API here to process transfer
        // For now, simulate success and go to result screen

        Intent intent = new Intent(this, TransferResultActivity.class);
        intent.putExtra("success", true);
        intent.putExtra("amount", amount);
        intent.putExtra("bank", bank);
        intent.putExtra("accountNumber", accountNumber);
        intent.putExtra("accountName", accountName);
        intent.putExtra("message", message);
        intent.putExtra("saveRecipient", saveRecipient);
        startActivity(intent);

        // Clear the back stack so user can't go back to transfer form
        finish();
    }
}
