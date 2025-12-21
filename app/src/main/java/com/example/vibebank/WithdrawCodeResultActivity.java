package com.example.vibebank;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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

public class WithdrawCodeResultActivity extends AppCompatActivity {

    private ImageView btnClose;
    private TextView txtWithdrawCode, txtAmount, txtAccount, txtExpiry, txtStatus;
    private MaterialButton btnCopyCode, btnDone;

    private String withdrawCode;
    private long amount;
    private String accountNumber;
    private long expiryTimeMillis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withdraw_code_result);

        getIntentData();
        initViews();
        setupListeners();
        displayCodeInfo();
    }

    private void getIntentData() {
        Intent intent = getIntent();
        withdrawCode = intent.getStringExtra("WITHDRAW_CODE");
        amount = intent.getLongExtra("AMOUNT", 0);
        accountNumber = intent.getStringExtra("ACCOUNT_NUMBER");
        expiryTimeMillis = intent.getLongExtra("EXPIRY_TIME", 0);
    }

    private void initViews() {
        btnClose = findViewById(R.id.btnClose);
        txtWithdrawCode = findViewById(R.id.txtWithdrawCode);
        txtAmount = findViewById(R.id.txtAmount);
        txtAccount = findViewById(R.id.txtAccount);
        txtExpiry = findViewById(R.id.txtExpiry);
        txtStatus = findViewById(R.id.txtStatus);
        btnCopyCode = findViewById(R.id.btnCopyCode);
        btnDone = findViewById(R.id.btnDone);
    }

    private void setupListeners() {
        btnClose.setOnClickListener(v -> navigateToHome());

        btnCopyCode.setOnClickListener(v -> copyCodeToClipboard());

        btnDone.setOnClickListener(v -> navigateToHome());
    }

    private void displayCodeInfo() {
        // Display code
        txtWithdrawCode.setText(withdrawCode);

        // Display amount
        txtAmount.setText(formatCurrency(amount) + " đ");

        // Display account - format if needed
        if (accountNumber != null && !accountNumber.contains(" ")) {
            String formatted = accountNumber.replaceAll("(.{4})", "$1 ").trim();
            txtAccount.setText(formatted);
        } else {
            txtAccount.setText(accountNumber);
        }

        // Display expiry time from intent data
        if (expiryTimeMillis > 0) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN"));
            txtExpiry.setText(dateFormat.format(new Date(expiryTimeMillis)));
        }

        // Display status
        txtStatus.setText("Chưa sử dụng");
        txtStatus.setTextColor(0xFF4CAF50); // Green
    }

    private void copyCodeToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Withdraw Code", withdrawCode);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Đã sao chép mã: " + withdrawCode, Toast.LENGTH_SHORT).show();
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private String formatCurrency(long amount) {
        return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(amount);
    }

    @Override
    public void onBackPressed() {
        navigateToHome();
    }
}
