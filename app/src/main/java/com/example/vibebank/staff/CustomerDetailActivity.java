package com.example.vibebank.staff;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vibebank.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import android.content.Intent;

public class CustomerDetailActivity extends AppCompatActivity {

    private TextInputEditText edtFullName, edtPhone, edtEmail;
    private TextView tvAccountNumber, tvBalance, tvKycStatus;
    private ImageView btnBack, btnEdit;
    private MaterialButton btnViewTransactions, btnSaveChanges, btnDepositForCustomer;

    private FirebaseFirestore db;
    private String userId;
    private double currentBalance = 0;
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
        btnDepositForCustomer = findViewById(R.id.btnDepositForCustomer);
    }

    private void loadCustomerData() {
        userId = getIntent().getStringExtra("userId");
        String fullName = getIntent().getStringExtra("fullName");
        String phone = getIntent().getStringExtra("phoneNumber");
        String email = getIntent().getStringExtra("email");
        String accountNumber = getIntent().getStringExtra("accountNumber");
        String kycStatus = getIntent().getStringExtra("kycStatus");
        currentBalance = getIntent().getDoubleExtra("balance", 0);

        edtFullName.setText(fullName);
        edtPhone.setText(phone);
        edtEmail.setText(email);
        tvAccountNumber.setText(accountNumber);
        
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        tvBalance.setText(formatter.format(currentBalance) + " VNĐ");

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

        btnDepositForCustomer.setOnClickListener(v -> showDepositDialog());
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

    /**
     * Hiển thị dialog cho nhân viên nhập số tiền nạp hộ
     */
    private void showDepositDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_amount_input, null);
        builder.setView(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvBalanceInfo = dialogView.findViewById(R.id.tvCurrentBalance);
        android.widget.EditText edtAmount = dialogView.findViewById(R.id.edtAmount);

        tvTitle.setText("Nạp tiền hộ khách hàng");
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        tvBalanceInfo.setText("Số dư hiện tại: " + formatter.format(currentBalance) + " VNĐ");

        builder.setPositiveButton("Xác nhận nạp", null);
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());

        android.app.AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String amountStr = edtAmount.getText().toString().trim();
                if (amountStr.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    double amount = Double.parseDouble(amountStr.replaceAll("[,.]", ""));
                    if (amount <= 0) {
                        Toast.makeText(this, "Số tiền phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (amount < 10000) {
                        Toast.makeText(this, "Số tiền tối thiểu 10,000 VNĐ", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    depositForCustomer(amount);
                    dialog.dismiss();
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    /**
     * Nhân viên nạp tiền trực tiếp vào tài khoản khách hàng
     * - Cộng số dư vào accounts/{userId}.balance
     * - Ghi log vào accounts/{userId}/transactions
     */
    private void depositForCustomer(double amount) {
        DocumentReference accountRef = db.collection("accounts").document(userId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            Double balance = transaction.get(accountRef).getDouble("balance");
            if (balance == null) balance = 0.0;

            double newBalance = balance + amount;
            transaction.update(accountRef, "balance", newBalance);

            // Ghi lịch sử giao dịch
            String transId = UUID.randomUUID().toString();
            Map<String, Object> log = new HashMap<>();
            log.put("userId", userId);
            log.put("type", "RECEIVED");
            log.put("amount", amount);
            log.put("content", "Nạp tiền tại quầy bởi nhân viên ngân hàng");
            log.put("relatedAccountName", "Giao dịch tại quầy");
            log.put("timestamp", Timestamp.now());
            log.put("transactionId", transId);

            DocumentReference logRef = accountRef.collection("transactions").document(transId);
            transaction.set(logRef, log);

            currentBalance = newBalance;
            return null;
        }).addOnSuccessListener(aVoid -> {
            NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
            tvBalance.setText(formatter.format(currentBalance) + " VNĐ");
            Toast.makeText(this, "Nạp tiền thành công cho khách hàng", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Lỗi nạp tiền: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
}

