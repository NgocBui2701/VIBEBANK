package com.example.vibebank;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vibebank.ui.home.HomeActivity;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class TransferResultActivity extends AppCompatActivity {
    private ImageView btnClose, imgResultIcon;
    private TextView txtResultStatus, txtResultAmount;
    private TextView txtDetailRecipientName, txtDetailRecipientAccount, txtDetailBank;
    private TextView txtDetailMessage, txtDetailDate, txtDetailTransactionId, txtDetailReferenceId;
    private MaterialButton btnComplete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_result);

        // Initialize views
        btnClose = findViewById(R.id.btnClose);
        imgResultIcon = findViewById(R.id.imgResultIcon);
        txtResultStatus = findViewById(R.id.txtResultStatus);
        txtResultAmount = findViewById(R.id.txtResultAmount);
        txtDetailRecipientName = findViewById(R.id.txtDetailRecipientName);
        txtDetailRecipientAccount = findViewById(R.id.txtDetailRecipientAccount);
        txtDetailBank = findViewById(R.id.txtDetailBank);
        txtDetailMessage = findViewById(R.id.txtDetailMessage);
        txtDetailDate = findViewById(R.id.txtDetailDate);
        txtDetailTransactionId = findViewById(R.id.txtDetailTransactionId);
        txtDetailReferenceId = findViewById(R.id.txtDetailReferenceId);
        btnComplete = findViewById(R.id.btnComplete);

        // Get transfer info from intent
        boolean success = getIntent().getBooleanExtra("success", true);
        String amount = getIntent().getStringExtra("amount");
        String bank = getIntent().getStringExtra("bank");
        String accountNumber = getIntent().getStringExtra("accountNumber");
        String accountName = getIntent().getStringExtra("accountName");
        String message = getIntent().getStringExtra("message");

        // Display result
        displayResult(success, amount, bank, accountNumber, accountName, message);

        // Close button
        btnClose.setOnClickListener(v -> {
            // Go back to home
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });


        // Complete button
        btnComplete.setOnClickListener(v -> {
            // Go back to home
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void displayResult(boolean success, String amount, String bank, 
                               String accountNumber, String accountName, String message) {
        if (success) {
            imgResultIcon.setImageResource(R.drawable.ic_check_circle);
            txtResultStatus.setText("Chuyển tiền thành công");
            txtResultStatus.setTextColor(0xFF4CAF50);
        } else {
            imgResultIcon.setImageResource(R.drawable.ic_error);
            txtResultStatus.setText("Chuyển tiền thất bại");
            txtResultStatus.setTextColor(0xFFF44336);
        }

        // Format and display amount
        try {
            long amountValue = Long.parseLong(amount.replaceAll("[^0-9]", ""));
            NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
            txtResultAmount.setText(formatter.format(amountValue) + " VND");
        } catch (Exception e) {
            txtResultAmount.setText(amount + " VND");
        }

        // Display recipient details
        if (accountName != null) txtDetailRecipientName.setText(accountName);
        if (accountNumber != null) txtDetailRecipientAccount.setText(accountNumber);
        if (bank != null) txtDetailBank.setText(bank);
        if (message != null && !message.isEmpty()) {
            txtDetailMessage.setText(message);
        } else {
            txtDetailMessage.setText("Không có");
        }

        // Display transaction date/time
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", new Locale("vi", "VN"));
        txtDetailDate.setText(dateFormat.format(new Date()));

        // Generate mock transaction IDs
        txtDetailTransactionId.setText(generateTransactionId());
        txtDetailReferenceId.setText(generateReferenceId());
    }

    private String generateTransactionId() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyDDD", Locale.US);
        String datePart = dateFormat.format(new Date());
        Random random = new Random();
        long numberPart = 10000000 + random.nextInt(90000000);
        return "FT" + datePart + numberPart;
    }

    private String generateReferenceId() {
        Random random = new Random();
        long numberPart = 100000000 + random.nextInt(900000000);
        return "REF" + numberPart;
    }

    @Override
    public void onBackPressed() {
        // Prevent going back, force user to use Complete button
        Toast.makeText(this, "Vui lòng nhấn nút Hoàn thành", Toast.LENGTH_SHORT).show();
    }
}
