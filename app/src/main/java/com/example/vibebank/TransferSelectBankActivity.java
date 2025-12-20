package com.example.vibebank;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class TransferSelectBankActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_select_bank);

        View btnBack = findViewById(R.id.btnBack);
        View layoutVibeBank = findViewById(R.id.btnBankVibeBank);
        View layoutOtherBank = findViewById(R.id.btnBankMB);
        btnBack.setOnClickListener(v -> finish());

        // Chọn ngân hàng đúng của đồ án
        View.OnClickListener selectCorrectBank = v -> {
            Intent intent = new Intent(TransferSelectBankActivity.this, TransferEnterAccountActivity.class);
            intent.putExtra("bankName", "VibeBank");
            startActivity(intent);
        };

        if (layoutOtherBank != null) {
            layoutOtherBank.setOnClickListener(v -> {
                Toast.makeText(this, "Hệ thống đang bảo trì kết nối ngân hàng này", Toast.LENGTH_SHORT).show();
            });
        }

        findViewById(R.id.btnBankVibeBank).setOnClickListener(selectCorrectBank);
    }
}