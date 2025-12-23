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

import com.example.vibebank.utils.SessionManager;
import com.example.vibebank.utils.WaterBillMockService;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.Locale;

public class WaterBillActivity extends AppCompatActivity {

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

    private WaterBillMockService.WaterBill currentBill;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_water_bill);

        // Initialize WaterBillMockService
        WaterBillMockService.initialize(this);

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
            currentBill = WaterBillMockService.getBill(customerId);

            progressBar.setVisibility(View.GONE);
            btnCheckBill.setEnabled(true);

            if (currentBill == null) {
                Toast.makeText(this, "Không tìm thấy hóa đơn với mã " + customerId, Toast.LENGTH_SHORT).show();
                return;
            }

            android.util.Log.d("WaterBillActivity", "Got bill for " + customerId + ", status: " + currentBill.getStatus());
            
            // Display bill info regardless of payment status
            displayBillInfo();
        }, 800);
    }

    private void displayBillInfo() {
        android.util.Log.d("WaterBillActivity", "displayBillInfo: status = " + currentBill.getStatus());
        
        tvCustomerName.setText(currentBill.getCustomerName());
        tvAddress.setText(currentBill.getAddress());
        tvPeriod.setText(currentBill.getPeriod());
        tvOldIndex.setText(String.valueOf(currentBill.getOldIndex()));
        tvNewIndex.setText(String.valueOf(currentBill.getNewIndex()));
        tvConsumption.setText(currentBill.getConsumption() + " m³");
        tvUnitPrice.setText(formatMoney((long)currentBill.getUnitPrice()) + " VND/m³");
        tvAmount.setText(formatMoney((long)currentBill.getAmount()) + " VND");
        tvDueDate.setText("Hạn thanh toán: " + currentBill.getDueDate());

        cardBillInfo.setVisibility(View.VISIBLE);
        tvCurrentBalance.setVisibility(View.VISIBLE);

        // Check payment status
        if ("PAID".equals(currentBill.getStatus())) {
            android.util.Log.d("WaterBillActivity", "Showing PAID status");
            // Show paid status, hide payment button
            tvPaymentStatus.setVisibility(View.VISIBLE);
            btnPay.setVisibility(View.GONE);
            tvCurrentBalance.setVisibility(View.GONE);
        } else {
            android.util.Log.d("WaterBillActivity", "Showing UNPAID status");
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
        intent.putExtra("receiverName", "Công ty cấp nước Sài Gòn");
        intent.putExtra("receiverUserId", "EXTERNAL_BANK");
        intent.putExtra("bankName", "Vibebank");
        intent.putExtra("amount", String.valueOf((long)currentBill.getAmount()));
        intent.putExtra("isWaterBillPayment", true);
        intent.putExtra("billCustomerId", currentBill.getCustomerId());
        
        startActivityForResult(intent, 1002);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1002) {
            if (resultCode == RESULT_OK) {
                // Transfer successful - show success screen
                // Bill already marked as paid in TransferDetailsActivity
                Intent resultIntent = new Intent(this, WaterBillResultActivity.class);
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

    private String formatMoney(long amount) {
        return NumberFormat.getInstance(new Locale("vi", "VN")).format(amount);
    }
}
