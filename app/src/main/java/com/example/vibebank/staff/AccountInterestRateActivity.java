package com.example.vibebank.staff;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vibebank.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AccountInterestRateActivity extends AppCompatActivity {

    private TextInputEditText edtPaymentRate, edtSavingRate, edtCreditRate;
    private MaterialButton btnSave;
    private ImageView btnBack;
    private FirebaseFirestore db;

    // Default rates
    private static final double DEFAULT_PAYMENT_RATE = 0.01; // 1% năm
    private static final double DEFAULT_SAVING_RATE = 0.06;  // 6% năm
    private static final double DEFAULT_CREDIT_RATE = 0.18;  // 18% năm

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_interest_rate);

        db = FirebaseFirestore.getInstance();

        initViews();
        loadCurrentRates();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        edtPaymentRate = findViewById(R.id.edtPaymentRate);
        edtSavingRate = findViewById(R.id.edtSavingRate);
        edtCreditRate = findViewById(R.id.edtCreditRate);
        btnSave = findViewById(R.id.btnSave);
    }

    private void loadCurrentRates() {
        db.collection("settings").document("interest_rates")
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Double paymentRate = doc.getDouble("payment");
                        Double savingRate = doc.getDouble("saving");
                        Double creditRate = doc.getDouble("credit");

                        edtPaymentRate.setText(String.format("%.2f", 
                                (paymentRate != null ? paymentRate : DEFAULT_PAYMENT_RATE) * 100));
                        edtSavingRate.setText(String.format("%.2f", 
                                (savingRate != null ? savingRate : DEFAULT_SAVING_RATE) * 100));
                        edtCreditRate.setText(String.format("%.2f", 
                                (creditRate != null ? creditRate : DEFAULT_CREDIT_RATE) * 100));
                    } else {
                        // Set default values
                        edtPaymentRate.setText(String.format("%.2f", DEFAULT_PAYMENT_RATE * 100));
                        edtSavingRate.setText(String.format("%.2f", DEFAULT_SAVING_RATE * 100));
                        edtCreditRate.setText(String.format("%.2f", DEFAULT_CREDIT_RATE * 100));
                    }
                });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveRates());
    }

    private void saveRates() {
        try {
            double paymentRate = Double.parseDouble(edtPaymentRate.getText().toString()) / 100;
            double savingRate = Double.parseDouble(edtSavingRate.getText().toString()) / 100;
            double creditRate = Double.parseDouble(edtCreditRate.getText().toString()) / 100;

            Map<String, Object> rates = new HashMap<>();
            rates.put("payment", paymentRate);
            rates.put("saving", savingRate);
            rates.put("credit", creditRate);
            rates.put("updated_at", com.google.firebase.Timestamp.now());

            db.collection("settings").document("interest_rates")
                    .set(rates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Cập nhật lãi suất thành công", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Lãi suất không hợp lệ", Toast.LENGTH_SHORT).show();
        }
    }

    // Inner class
    static class PendingTransaction {
        String transactionId;
        String senderName;
        String receiverName;
        double amount;
        String content;
        Timestamp timestamp;

        PendingTransaction(String transactionId, String senderName, String receiverName,
                          double amount, String content, Timestamp timestamp) {
            this.transactionId = transactionId;
            this.senderName = senderName;
            this.receiverName = receiverName;
            this.amount = amount;
            this.content = content;
            this.timestamp = timestamp;
        }
    }

    static class TransactionApprovalAdapter extends RecyclerView.Adapter<TransactionApprovalAdapter.ViewHolder> {
        private List<PendingTransaction> transactions;
        private OnTransactionClickListener listener;

        interface OnTransactionClickListener {
            void onClick(PendingTransaction transaction);
        }

        TransactionApprovalAdapter(List<PendingTransaction> transactions, OnTransactionClickListener listener) {
            this.transactions = transactions;
            this.listener = listener;
        }

        @androidx.annotation.NonNull
        @Override
        public ViewHolder onCreateViewHolder(@androidx.annotation.NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_customer, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@androidx.annotation.NonNull ViewHolder holder, int position) {
            PendingTransaction transaction = transactions.get(position);
            holder.bind(transaction, listener);
        }

        @Override
        public int getItemCount() {
            return transactions.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            android.widget.TextView tvCustomerName, tvAccountNumber;

            ViewHolder(@androidx.annotation.NonNull android.view.View itemView) {
                super(itemView);
                tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
                tvAccountNumber = itemView.findViewById(R.id.tvAccountNumber);
            }

            void bind(PendingTransaction transaction, OnTransactionClickListener listener) {
                tvCustomerName.setText(transaction.senderName + " → " + transaction.receiverName);
                tvAccountNumber.setText(String.format("%,.0f VNĐ", transaction.amount));
                itemView.setOnClickListener(v -> listener.onClick(transaction));
            }
        }
    }
}

