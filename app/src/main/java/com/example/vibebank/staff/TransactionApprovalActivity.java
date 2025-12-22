package com.example.vibebank.staff;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibebank.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TransactionApprovalActivity extends AppCompatActivity {

    private RecyclerView recyclerTransactions;
    private LinearLayout layoutEmpty;
    private ImageView btnBack;
    
    private FirebaseFirestore db;
    private List<PendingTransaction> pendingTransactions = new ArrayList<>();
    private TransactionApprovalAdapter adapter;

    // Ngưỡng giao dịch cần phê duyệt (500 triệu)
    private static final double APPROVAL_THRESHOLD = 500000000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_approval);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        setupListeners();
        loadPendingTransactions();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        recyclerTransactions = findViewById(R.id.recyclerTransactions);
        layoutEmpty = findViewById(R.id.layoutEmpty);
    }

    private void setupRecyclerView() {
        adapter = new TransactionApprovalAdapter(pendingTransactions, this::showTransactionDetail);
        recyclerTransactions.setLayoutManager(new LinearLayoutManager(this));
        recyclerTransactions.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadPendingTransactions() {
        pendingTransactions.clear();

        // Query tất cả giao dịch lớn chưa được duyệt
        // Note: Trong thực tế bạn cần có collection "pending_transactions"
        // Ở đây mình tạo mock data
        
        Toast.makeText(this, "Chức năng đang phát triển - Mock data", Toast.LENGTH_SHORT).show();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (pendingTransactions.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerTransactions.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerTransactions.setVisibility(View.VISIBLE);
        }
    }

    private void showTransactionDetail(PendingTransaction transaction) {
        // Show dialog to approve/reject
        new AlertDialog.Builder(this)
                .setTitle("Duyệt giao dịch")
                .setMessage("Số tiền: " + transaction.amount + "\nNội dung: " + transaction.content)
                .setPositiveButton("Phê duyệt", (dialog, which) -> {
                    approveTransaction(transaction.transactionId);
                })
                .setNegativeButton("Từ chối", (dialog, which) -> {
                    rejectTransaction(transaction.transactionId);
                })
                .setNeutralButton("Hủy", null)
                .show();
    }

    private void approveTransaction(String transactionId) {
        Toast.makeText(this, "Đã phê duyệt giao dịch", Toast.LENGTH_SHORT).show();
        loadPendingTransactions();
    }

    private void rejectTransaction(String transactionId) {
        Toast.makeText(this, "Đã từ chối giao dịch", Toast.LENGTH_SHORT).show();
        loadPendingTransactions();
    }

    // Inner classes
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
            View view = android.view.LayoutInflater.from(parent.getContext())
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
            TextView tvCustomerName, tvAccountNumber, tvKycStatus;

            ViewHolder(@androidx.annotation.NonNull View itemView) {
                super(itemView);
                tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
                tvAccountNumber = itemView.findViewById(R.id.tvAccountNumber);
                tvKycStatus = itemView.findViewById(R.id.tvKycStatus);
            }

            void bind(PendingTransaction transaction, OnTransactionClickListener listener) {
                tvCustomerName.setText(transaction.senderName + " → " + transaction.receiverName);
                NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
                tvAccountNumber.setText(formatter.format(transaction.amount) + " VNĐ");
                tvKycStatus.setText("Chờ duyệt");

                itemView.setOnClickListener(v -> listener.onClick(transaction));
            }
        }
    }
}

