package com.example.vibebank;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

/**
 * TopupOptionsActivity - Choose between account deposit or phone topup
 */
public class TopupOptionsActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topup_options);
        
        ImageView btnBack = findViewById(R.id.btnBack);
        CardView cardDepositVNPay = findViewById(R.id.cardDepositVNPay);
        CardView cardTopupPhone = findViewById(R.id.cardTopupPhone);
        
        btnBack.setOnClickListener(v -> finish());
        
        // Nạp tiền tài khoản qua VNPay
        cardDepositVNPay.setOnClickListener(v -> {
            Intent intent = new Intent(this, DepositActivity.class);
            intent.putExtra("accountType", 0); // 0 = Payment account deposit via VNPay
            startActivity(intent);
        });
        
        // Nạp tiền điện thoại
        cardTopupPhone.setOnClickListener(v -> {
            Intent intent = new Intent(this, TopupActivity.class);
            startActivity(intent);
        });
    }
}
