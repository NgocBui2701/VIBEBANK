package com.example.vibebank.staff;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vibebank.R;
import com.example.vibebank.TransactionHistoryActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.content.Intent;

public class CustomerDetailActivity extends AppCompatActivity {

    private TextInputEditText edtFullName, edtPhone, edtEmail;
    private TextView tvAccountNumber, tvBalance, tvKycStatus;
    private ImageView btnBack, btnEdit;
    private MaterialButton btnViewTransactions, btnSaveChanges;

    private FirebaseFirestore db;
    private String userId;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_detail);

        db = FirebaseFirestore.getInstance();

        initViews();
        loadCustomerData();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnEdit = findViewById(R.id.btnEdit);
        edtFullName = findViewById(R.id.edtFullName);
        edtPhone = findViewById(R.id.edtPhone);
        edtEmail = findViewById(R.id.edtEmail);
        tvAccountNumber = findViewById(R.id.tvAccountNumber);
        tvBalance = findViewById(R.id.tvBalance);
        tvKycStatus = findViewById(R.id.tvKycStatus);
        btnViewTransactions = findViewById(R.id.btnViewTransactions);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
    }

    private void loadCustomerData() {
        userId = getIntent().getStringExtra("userId");
        String fullName = getIntent().getStringExtra("fullName");
        String phone = getIntent().getStringExtra("phoneNumber");
        String email = getIntent().getStringExtra("email");
        String accountNumber = getIntent().getStringExtra("accountNumber");
        String kycStatus = getIntent().getStringExtra("kycStatus");
        double balance = getIntent().getDoubleExtra("balance", 0);

        edtFullName.setText(fullName);
        edtPhone.setText(phone);
        edtEmail.setText(email);
        tvAccountNumber.setText(accountNumber);
        
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        tvBalance.setText(formatter.format(balance) + " VNĐ");

        tvKycStatus.setText(getKycStatusDisplay(kycStatus));
        
        // Set status color
        if ("verified".equals(kycStatus)) {
            tvKycStatus.setBackgroundColor(Color.parseColor("#4CAF50")); // Green
        } else if ("pending".equals(kycStatus)) {
            tvKycStatus.setBackgroundColor(Color.parseColor("#FF9800")); // Orange
        } else {
            tvKycStatus.setBackgroundColor(Color.parseColor("#F44336")); // Red
        }
    }

    private String getKycStatusDisplay(String status) {
        if ("pending".equals(status)) return "Chờ duyệt";
        if ("verified".equals(status)) return "Đã xác minh";
        if ("rejected".equals(status)) return "Từ chối";
        return "Chưa rõ";
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnEdit.setOnClickListener(v -> toggleEditMode());

        btnSaveChanges.setOnClickListener(v -> saveChanges());

        btnViewTransactions.setOnClickListener(v -> {
            // Open TransactionHistoryActivity for this customer
            // Note: You may need to modify TransactionHistoryActivity to accept userId parameter
            Toast.makeText(this, "Xem giao dịch của khách hàng", Toast.LENGTH_SHORT).show();
        });
    }

    private void toggleEditMode() {
        isEditMode = !isEditMode;
        
        edtFullName.setEnabled(isEditMode);
        edtPhone.setEnabled(isEditMode);
        edtEmail.setEnabled(isEditMode);

        if (isEditMode) {
            btnSaveChanges.setVisibility(android.view.View.VISIBLE);
            Toast.makeText(this, "Chế độ chỉnh sửa", Toast.LENGTH_SHORT).show();
        } else {
            btnSaveChanges.setVisibility(android.view.View.GONE);
        }
    }

    private void saveChanges() {
        String newName = edtFullName.getText().toString().trim();
        String newPhone = edtPhone.getText().toString().trim();
        String newEmail = edtEmail.getText().toString().trim();

        if (newName.isEmpty() || newPhone.isEmpty() || newEmail.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("full_name", newName);
        updates.put("phone_number", newPhone);
        updates.put("email", newEmail);

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    toggleEditMode();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}

