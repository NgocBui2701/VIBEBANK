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
import java.text.NumberFormat;
import java.util.Locale;

public class WithdrawCodeActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView txtAccountNumber, txtBalance;
    private EditText edtAmount, edtPin;
    private MaterialButton btn500k, btn1m, btn2m, btn3m, btn5m, btn10m;
    private MaterialButton btnGenerateCode;

    private long currentBalance = 50000000; // 50 triệu
    private final String DEFAULT_PIN = "123456"; // Demo PIN

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withdraw_code);

        initViews();
        setupListeners();
        loadAccountInfo();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        txtAccountNumber = findViewById(R.id.txtAccountNumber);
        txtBalance = findViewById(R.id.txtBalance);
        edtAmount = findViewById(R.id.edtAmount);
        edtPin = findViewById(R.id.edtPin);

        // Quick amount buttons
        btn500k = findViewById(R.id.btn500k);
        btn1m = findViewById(R.id.btn1m);
        btn2m = findViewById(R.id.btn2m);
        btn3m = findViewById(R.id.btn3m);
        btn5m = findViewById(R.id.btn5m);
        btn10m = findViewById(R.id.btn10m);

        btnGenerateCode = findViewById(R.id.btnGenerateCode);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // Quick amount buttons
        btn500k.setOnClickListener(v -> setAmount(500000));
        btn1m.setOnClickListener(v -> setAmount(1000000));
        btn2m.setOnClickListener(v -> setAmount(2000000));
        btn3m.setOnClickListener(v -> setAmount(3000000));
        btn5m.setOnClickListener(v -> setAmount(5000000));
        btn10m.setOnClickListener(v -> setAmount(10000000));

        // Amount input formatting
        edtAmount.addTextChangedListener(new TextWatcher() {
            private String current = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(current)) {
                    edtAmount.removeTextChangedListener(this);

                    String cleanString = s.toString().replaceAll("[,.]", "");
                    
                    if (!cleanString.isEmpty()) {
                        try {
                            long parsed = Long.parseLong(cleanString);
                            String formatted = NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(parsed);
                            current = formatted;
                            edtAmount.setText(formatted);
                            edtAmount.setSelection(formatted.length());
                        } catch (NumberFormatException e) {
                            edtAmount.setText("");
                        }
                    }

                    edtAmount.addTextChangedListener(this);
                }
            }
        });

        // Generate code button
        btnGenerateCode.setOnClickListener(v -> validateAndGenerateCode());
    }

    private void loadAccountInfo() {
        // TODO: Load from database
        txtAccountNumber.setText("1234 5678 9012");
        txtBalance.setText("Số dư: " + formatCurrency(currentBalance) + " đ");
    }

    private void setAmount(long amount) {
        edtAmount.setText(NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(amount));
    }

    private void validateAndGenerateCode() {
        String amountStr = edtAmount.getText().toString().replaceAll("[,.]", "");
        String pin = edtPin.getText().toString().trim();

        // Validate amount
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            edtAmount.requestFocus();
            return;
        }

        long amount = Long.parseLong(amountStr);

        if (amount < 50000) {
            Toast.makeText(this, "Số tiền rút tối thiểu là 50.000 đ", Toast.LENGTH_SHORT).show();
            edtAmount.requestFocus();
            return;
        }

        if (amount > 20000000) {
            Toast.makeText(this, "Số tiền rút tối đa là 20.000.000 đ/lần", Toast.LENGTH_SHORT).show();
            edtAmount.requestFocus();
            return;
        }

        if (amount % 50000 != 0) {
            Toast.makeText(this, "Số tiền phải là bội số của 50.000 đ", Toast.LENGTH_SHORT).show();
            edtAmount.requestFocus();
            return;
        }

        if (amount > currentBalance) {
            Toast.makeText(this, "Số dư không đủ", Toast.LENGTH_SHORT).show();
            edtAmount.requestFocus();
            return;
        }

        // Validate PIN
        if (pin.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập mã PIN", Toast.LENGTH_SHORT).show();
            edtPin.requestFocus();
            return;
        }

        if (pin.length() != 6) {
            Toast.makeText(this, "Mã PIN phải có 6 chữ số", Toast.LENGTH_SHORT).show();
            edtPin.requestFocus();
            return;
        }

        if (!pin.equals(DEFAULT_PIN)) {
            Toast.makeText(this, "Mã PIN không đúng", Toast.LENGTH_SHORT).show();
            edtPin.requestFocus();
            return;
        }

        // Generate code and navigate to result
        generateCode(amount);
    }

    private void generateCode(long amount) {
        // Generate random 6-digit code
        String code = String.format("%06d", (int)(Math.random() * 1000000));

        // Navigate to result screen
        Intent intent = new Intent(this, WithdrawCodeResultActivity.class);
        intent.putExtra("WITHDRAW_CODE", code);
        intent.putExtra("AMOUNT", amount);
        intent.putExtra("ACCOUNT_NUMBER", txtAccountNumber.getText().toString());
        startActivity(intent);
        finish();
    }

    private String formatCurrency(long amount) {
        return NumberFormat.getNumberInstance(new Locale("vi", "VN")).format(amount);
    }
}
