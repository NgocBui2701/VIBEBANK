package com.example.vibebank;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.vibebank.ui.home.HomeActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TransferResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_result);

        TextView txtAmount = findViewById(R.id.txtResultAmount);
        TextView txtReceiverName = findViewById(R.id.txtDetailRecipientName);
        TextView txtReceiverAccount = findViewById(R.id.txtDetailRecipientAccount);
        TextView txtContent = findViewById(R.id.txtDetailMessage);
        TextView txtRef = findViewById(R.id.txtDetailReferenceId);
        TextView txtTime = findViewById(R.id.txtDetailDate);
        Button btnComplete = findViewById(R.id.btnComplete);
        View btnClose = findViewById(R.id.btnClose);

        double amount = getIntent().getDoubleExtra("amount", 0);
        String refId = getIntent().getStringExtra("refId");
        String receiverName = getIntent().getStringExtra("receiverName");
        String receiverAccount = getIntent().getStringExtra("receiverAccount");
        String content = getIntent().getStringExtra("content");

        // Format tiền tệ đơn giản
        if (txtAmount != null) {
            txtAmount.setText(String.format("%,.0f VND", amount));
        }

        // Hiển thị thông tin người nhận
        if (txtReceiverName != null) {
            txtReceiverName.setText(receiverName != null ? receiverName : "N/A");
        }

        if (txtReceiverAccount != null) {
            txtReceiverAccount.setText(receiverAccount != null ? receiverAccount : "N/A");
        }

        if (txtContent != null) {
            txtContent.setText(content != null ? content : "N/A");
        }

        // Hiển thị mã giao dịch
        if (txtRef != null) {
            txtRef.setText(refId != null ? refId : "N/A");
        }

        // Hiển thị thời gian hiện tại
        if (txtTime != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            txtTime.setText(sdf.format(new Date()));
        }

        btnComplete.setOnClickListener(v -> goHome());

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> goHome());
        }
    }

    private void goHome() {
        Intent intent = new Intent(TransferResultActivity.this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
