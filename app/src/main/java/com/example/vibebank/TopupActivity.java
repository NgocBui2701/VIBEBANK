package com.example.vibebank;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.vibebank.utils.SessionManager;
import com.example.vibebank.utils.TopupMockService;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class TopupActivity extends AppCompatActivity {

    private ImageView btnBack;
    private EditText edtPhoneNumber;
    private TextView tvCarrierName, tvCurrentBalance;
    private LinearLayout llPackages;
    private CardView cardSelectedPackage;
    private TextView tvSelectedPackage, tvSelectedAmount;
    private MaterialButton btnPay;

    private FirebaseFirestore db;
    private String currentUserId;
    private double currentBalance = 0;
    private SessionManager sessionManager;
    private String selectedPhoneNumber;
    private long selectedAmount = 0;
    private String selectedPackageName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topup);

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
        loadTopupPackages();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        edtPhoneNumber = findViewById(R.id.edtPhoneNumber);
        tvCarrierName = findViewById(R.id.tvCarrierName);
        tvCurrentBalance = findViewById(R.id.tvCurrentBalance);
        llPackages = findViewById(R.id.llPackages);
        cardSelectedPackage = findViewById(R.id.cardSelectedPackage);
        tvSelectedPackage = findViewById(R.id.tvSelectedPackage);
        tvSelectedAmount = findViewById(R.id.tvSelectedAmount);
        btnPay = findViewById(R.id.btnPay);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        edtPhoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String phoneNumber = s.toString().trim();
                if (TopupMockService.isValidPhoneNumber(phoneNumber)) {
                    String carrier = TopupMockService.getCarrierName(phoneNumber);
                    tvCarrierName.setText("Nhà mạng: " + carrier);
                    tvCarrierName.setVisibility(View.VISIBLE);
                    selectedPhoneNumber = phoneNumber;
                } else {
                    tvCarrierName.setVisibility(View.GONE);
                    selectedPhoneNumber = null;
                }
                updatePayButton();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

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

    private void loadTopupPackages() {
        List<TopupMockService.TopupPackage> packages = TopupMockService.getTopupPackages();
        
        for (TopupMockService.TopupPackage pkg : packages) {
            View packageView = getLayoutInflater().inflate(R.layout.item_topup_package, llPackages, false);
            
            TextView tvPackageName = packageView.findViewById(R.id.tvPackageName);
            TextView tvPackageAmount = packageView.findViewById(R.id.tvPackageAmount);
            TextView tvPackageDesc = packageView.findViewById(R.id.tvPackageDesc);
            CardView cardPackage = packageView.findViewById(R.id.cardPackage);
            
            tvPackageName.setText(pkg.getName());
            tvPackageAmount.setText(formatMoney(pkg.getAmount()) + " VND");
            tvPackageDesc.setText(pkg.getDescription());
            
            cardPackage.setOnClickListener(v -> selectPackage(pkg, cardPackage));
            
            llPackages.addView(packageView);
        }
    }

    private void selectPackage(TopupMockService.TopupPackage pkg, CardView selectedCard) {
        // Reset all package cards
        for (int i = 0; i < llPackages.getChildCount(); i++) {
            View child = llPackages.getChildAt(i);
            CardView card = child.findViewById(R.id.cardPackage);
            card.setCardBackgroundColor(getResources().getColor(android.R.color.white));
            card.setCardElevation(4.0f);
        }
        
        // Highlight selected package
        selectedCard.setCardBackgroundColor(getResources().getColor(R.color.background));
        selectedCard.setCardElevation(8.0f);
        
        selectedAmount = pkg.getAmount();
        selectedPackageName = pkg.getName();
        
        tvSelectedPackage.setText(pkg.getName());
        tvSelectedAmount.setText(formatMoney(pkg.getAmount()) + " VND");
        cardSelectedPackage.setVisibility(View.VISIBLE);
        
        updatePayButton();
    }

    private void updatePayButton() {
        boolean canPay = selectedPhoneNumber != null && 
                        !selectedPhoneNumber.isEmpty() && 
                        selectedAmount > 0;
        btnPay.setEnabled(canPay);
        btnPay.setAlpha(canPay ? 1.0f : 0.5f);
    }

    private void proceedToTransfer() {
        if (selectedPhoneNumber == null || selectedAmount == 0) {
            Toast.makeText(this, "Vui lòng chọn gói cước", Toast.LENGTH_SHORT).show();
            return;
        }

        String formattedPhone = TopupMockService.formatPhoneNumber(selectedPhoneNumber);
        String carrier = TopupMockService.getCarrierName(selectedPhoneNumber);

        // Navigate to TransferDetailsActivity
        Intent intent = new Intent(this, TransferDetailsActivity.class);
        intent.putExtra("receiverAccountNumber", selectedPhoneNumber);
        intent.putExtra("receiverName", carrier);
        intent.putExtra("receiverUserId", "EXTERNAL_BANK");
        intent.putExtra("bankName", "Vibebank");
        intent.putExtra("amount", String.valueOf(selectedAmount));
        intent.putExtra("isTopupPayment", true);
        intent.putExtra("topupPhoneNumber", selectedPhoneNumber);
        intent.putExtra("topupPackageName", selectedPackageName);
        
        startActivityForResult(intent, 1003);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1003) {
            if (resultCode == RESULT_OK) {
                // Transfer successful - show success screen
                Intent resultIntent = new Intent(this, TopupResultActivity.class);
                resultIntent.putExtra("phoneNumber", selectedPhoneNumber);
                resultIntent.putExtra("packageName", selectedPackageName);
                resultIntent.putExtra("amount", selectedAmount);
                startActivity(resultIntent);
                finish();
            } else {
                // Transfer failed or cancelled
                Toast.makeText(this, "Nạp cước thất bại hoặc đã hủy", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String formatMoney(long amount) {
        return NumberFormat.getInstance(new Locale("vi", "VN")).format(amount);
    }
}
