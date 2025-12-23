package com.example.vibebank;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.vibebank.utils.ElectricBillMockService;
import com.example.vibebank.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ElectricBillActivity extends AppCompatActivity {

    private ImageView btnBack;
    private EditText edtCustomerId;
    private MaterialButton btnCheckBill, btnPay;
    private CardView cardBillInfo;
    private TextView tvCustomerName, tvAddress, tvPeriod, tvOldIndex, tvNewIndex;
    private TextView tvConsumption, tvUnitPrice, tvAmount, tvDueDate, tvCurrentBalance, tvPaymentStatus;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private String currentUserId;
    private double currentBalance = 0;
    private SessionManager sessionManager;

    private ElectricBillMockService.ElectricBill currentBill;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_electric_bill);

        // Initialize ElectricBillMockService
        ElectricBillMockService.initialize(this);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);
        
        // Lấy userId từ session
        currentUserId = sessionManager.getCurrentUserId();
        
        // Fallback: thử lấy từ FirebaseAuth nếu chưa có trong session
        if (currentUserId == null || currentUserId.isEmpty()) {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                currentUserId = auth.getCurrentUser().getUid();
            }
        }

        initViews();
        setupListeners();
        loadAccountBalance();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        edtCustomerId = findViewById(R.id.edtCustomerId);
        btnCheckBill = findViewById(R.id.btnCheckBill);
        btnPay = findViewById(R.id.btnPay);
        cardBillInfo = findViewById(R.id.cardBillInfo);
        progressBar = findViewById(R.id.progressBar);

        tvCustomerName = findViewById(R.id.tvCustomerName);
        tvAddress = findViewById(R.id.tvAddress);
        tvPeriod = findViewById(R.id.tvPeriod);
        tvOldIndex = findViewById(R.id.tvOldIndex);
        tvNewIndex = findViewById(R.id.tvNewIndex);
        tvConsumption = findViewById(R.id.tvConsumption);
        tvUnitPrice = findViewById(R.id.tvUnitPrice);
        tvAmount = findViewById(R.id.tvAmount);
        tvDueDate = findViewById(R.id.tvDueDate);
        tvCurrentBalance = findViewById(R.id.tvCurrentBalance);
        tvPaymentStatus = findViewById(R.id.tvPaymentStatus);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnCheckBill.setOnClickListener(v -> checkBill());
        btnPay.setOnClickListener(v -> proceedToTransfer());
    }

    private void loadAccountBalance() {
        if (currentUserId == null) {
            return;
        }

        db.collection("accounts").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Double balance = documentSnapshot.getDouble("balance");
                        if (balance != null) {
                            currentBalance = balance;
                            tvCurrentBalance.setText("Số dư hiện tại: " + formatMoney((long)currentBalance) + " VND");
                        }
                    }
                });
    }

    private void checkBill() {
        String customerId = edtCustomerId.getText().toString().trim().toUpperCase();

        if (customerId.isEmpty()) {
            edtCustomerId.setError("Vui lòng nhập mã khách hàng");
            return;
        }

        btnCheckBill.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        cardBillInfo.setVisibility(View.GONE);
        btnPay.setVisibility(View.GONE);
        tvCurrentBalance.setVisibility(View.GONE);

        // Simulate API call delay
        new android.os.Handler().postDelayed(() -> {
            currentBill = ElectricBillMockService.getBill(customerId);

            progressBar.setVisibility(View.GONE);
            btnCheckBill.setEnabled(true);

            if (currentBill == null) {
                Toast.makeText(this, "Không tìm thấy hóa đơn với mã " + customerId, Toast.LENGTH_SHORT).show();
                return;
            }

            android.util.Log.d("ElectricBillActivity", "Got bill for " + customerId + ", status: " + currentBill.getStatus());
            
            // Display bill info regardless of payment status
            displayBillInfo();
        }, 800);
    }

    private void displayBillInfo() {
        android.util.Log.d("ElectricBillActivity", "displayBillInfo: status = " + currentBill.getStatus());
        
        tvCustomerName.setText(currentBill.getCustomerName());
        tvAddress.setText(currentBill.getAddress());
        tvPeriod.setText(currentBill.getPeriod());
        tvOldIndex.setText(String.valueOf(currentBill.getOldIndex()));
        tvNewIndex.setText(String.valueOf(currentBill.getNewIndex()));
        tvConsumption.setText(currentBill.getConsumption() + " kWh");
        tvUnitPrice.setText(formatMoney((long)currentBill.getUnitPrice()) + " VND/kWh");
        tvAmount.setText(formatMoney((long)currentBill.getAmount()) + " VND");
        tvDueDate.setText("Hạn thanh toán: " + currentBill.getDueDate());

        cardBillInfo.setVisibility(View.VISIBLE);
        tvCurrentBalance.setVisibility(View.VISIBLE);

        // Check payment status
        if ("PAID".equals(currentBill.getStatus())) {
            android.util.Log.d("ElectricBillActivity", "Showing PAID status");
            // Show paid status, hide payment button
            tvPaymentStatus.setVisibility(View.VISIBLE);
            btnPay.setVisibility(View.GONE);
            tvCurrentBalance.setVisibility(View.GONE);
        } else {
            android.util.Log.d("ElectricBillActivity", "Showing UNPAID status");
            // Hide paid status, show payment button
            tvPaymentStatus.setVisibility(View.GONE);
            btnPay.setVisibility(View.VISIBLE);
        }
    }

    private void proceedToTransfer() {
        if (currentBill == null) {
            return;
        }

        // Navigate to TransferDetailsActivity
        Intent intent = new Intent(this, TransferDetailsActivity.class);
        intent.putExtra("receiverAccountNumber", currentBill.getCustomerId());
        intent.putExtra("receiverName", "Công ty điện lực EVN");
        intent.putExtra("receiverUserId", "EXTERNAL_BANK");
        intent.putExtra("bankName", "Vibebank");
        intent.putExtra("amount", String.valueOf((long)currentBill.getAmount()));
        intent.putExtra("isElectricBillPayment", true);
        intent.putExtra("billCustomerId", currentBill.getCustomerId());
        
        startActivityForResult(intent, 1001);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001) {
            if (resultCode == RESULT_OK) {
                // Transfer successful - show success screen
                // Bill already marked as paid in TransferDetailsActivity
                Intent resultIntent = new Intent(this, ElectricBillResultActivity.class);
                resultIntent.putExtra("customerId", currentBill.getCustomerId());
                resultIntent.putExtra("customerName", currentBill.getCustomerName());
                resultIntent.putExtra("period", currentBill.getPeriod());
                resultIntent.putExtra("amount", currentBill.getAmount());
                startActivity(resultIntent);
                finish();
            } else {
                // Transfer failed or cancelled
                Toast.makeText(this, "Thanh toán thất bại hoặc đã hủy", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void confirmPayment() {
        if (currentBill == null) {
            return;
        }

        double amount = currentBill.getAmount();

        if (currentBalance < amount) {
            new AlertDialog.Builder(this)
                    .setTitle("Số dư không đủ")
                    .setMessage("Số dư tài khoản không đủ để thanh toán hóa đơn này.\n\nSố dư: " + 
                               formatMoney((long)currentBalance) + " VND\nCần: " + 
                               formatMoney((long)amount) + " VND")
                    .setPositiveButton("Đóng", null)
                    .show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Xác nhận thanh toán")
                .setMessage("Bạn có chắc muốn thanh toán hóa đơn tiền điện?\n\n" +
                           "Mã KH: " + currentBill.getCustomerId() + "\n" +
                           "Khách hàng: " + currentBill.getCustomerName() + "\n" +
                           "Số tiền: " + formatMoney((long)amount) + " VND")
                .setPositiveButton("Thanh toán", (dialog, which) -> performPayment())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void performPayment() {
        btnPay.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        double amount = currentBill.getAmount();
        DocumentReference accountRef = db.collection("accounts").document(currentUserId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot accountDoc = transaction.get(accountRef);
            Double balance = 0.0;
            boolean exists = accountDoc.exists();

            if (exists) {
                balance = accountDoc.getDouble("balance");
                if (balance == null) balance = 0.0;
            }

            if (balance < amount) {
                throw new com.google.firebase.firestore.FirebaseFirestoreException("Insufficient balance", com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED);
            }

            double newBalance = balance - amount;

            if (exists) {
                transaction.update(accountRef, "balance", newBalance);
            } else {
                Map<String, Object> accountData = new HashMap<>();
                accountData.put("userId", currentUserId);
                accountData.put("balance", newBalance);
                transaction.set(accountRef, accountData);
            }

            // Log transaction
            String transId = UUID.randomUUID().toString();
            Map<String, Object> log = new HashMap<>();
            log.put("userId", currentUserId);
            log.put("type", "SENT");
            log.put("amount", amount);
            log.put("content", "Thanh toán tiền điện " + currentBill.getPeriod());
            log.put("relatedAccountName", "Điện lực " + currentBill.getCustomerId());
            log.put("timestamp", Timestamp.now());
            log.put("transactionId", transId);

            DocumentReference logRef = accountRef.collection("transactions").document(transId);
            transaction.set(logRef, log);

            return null;
        }).addOnSuccessListener(aVoid -> {
            // Mark bill as paid
            ElectricBillMockService.payBill(currentBill.getCustomerId());

            progressBar.setVisibility(View.GONE);

            // Navigate to result
            Intent intent = new Intent(this, ElectricBillResultActivity.class);
            intent.putExtra("CUSTOMER_ID", currentBill.getCustomerId());
            intent.putExtra("CUSTOMER_NAME", currentBill.getCustomerName());
            intent.putExtra("PERIOD", currentBill.getPeriod());
            intent.putExtra("AMOUNT", currentBill.getAmount());
            intent.putExtra("CONSUMPTION", currentBill.getConsumption());
            startActivity(intent);
            finish();

        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            btnPay.setEnabled(true);
            Toast.makeText(this, "Lỗi thanh toán: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private String formatMoney(long amount) {
        return NumberFormat.getInstance(new Locale("vi", "VN")).format(amount);
    }
}
