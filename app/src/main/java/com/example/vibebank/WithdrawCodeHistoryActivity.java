package com.example.vibebank;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Activity hiển thị lịch sử mã rút tiền đã tạo
 */
public class WithdrawCodeHistoryActivity extends AppCompatActivity {
    
    private static final String TAG = "WithdrawCodeHistory";
    
    private ImageView btnBack;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    
    private FirebaseFirestore db;
    private String currentUserId;
    private WithdrawCodeHistoryAdapter adapter;
    private List<WithdrawCodeItem> codeList;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withdraw_code_history);
        
        db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
            Log.d(TAG, "Current userId: " + currentUserId);
        } else {
            Log.e(TAG, "No authenticated user found");
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initViews();
        setupRecyclerView();
        loadWithdrawCodes();
    }
    
    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        
        btnBack.setOnClickListener(v -> finish());
    }
    
    private void setupRecyclerView() {
        codeList = new ArrayList<>();
        adapter = new WithdrawCodeHistoryAdapter(this, codeList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }
    
    private void loadWithdrawCodes() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);
        
        Log.d(TAG, "Loading withdrawal codes for userId: " + currentUserId);
        
        // Query without composite index (removed orderBy to avoid index requirement)
        db.collection("withdrawal_codes")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    
                    Log.d(TAG, "Query successful, documents count: " + queryDocumentSnapshots.size());
                    
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "No withdrawal codes found");
                        tvEmpty.setVisibility(View.VISIBLE);
                        return;
                    }
                    
                    recyclerView.setVisibility(View.VISIBLE);
                    codeList.clear();
                    
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        WithdrawCodeItem item = new WithdrawCodeItem();
                        item.code = doc.getString("code");
                        item.amount = doc.getDouble("amount");
                        item.createdAt = doc.getTimestamp("createdAt");
                        item.expiresAt = doc.getTimestamp("expiryTime");
                        item.status = "Đã rút"; // Mặc định đã rút
                        
                        Log.d(TAG, "Loaded code: " + item.code + ", amount: " + item.amount);
                        codeList.add(item);
                    }
                    
                    // Sort by createdAt descending (newest first) in memory
                    codeList.sort((a, b) -> {
                        if (a.createdAt == null || b.createdAt == null) return 0;
                        return b.createdAt.compareTo(a.createdAt);
                    });
                    
                    Log.d(TAG, "Total codes loaded: " + codeList.size());
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                    Log.e(TAG, "Error loading withdrawal codes", e);
                    Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace(); // Log full error
                });
    }
    
    /**
     * Model class cho withdraw code item
     */
    public static class WithdrawCodeItem {
        public String code;
        public Double amount;
        public Timestamp createdAt;
        public Timestamp expiresAt;
        public String status;
        
        public String getFormattedAmount() {
            NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
            formatter.setMaximumFractionDigits(0);
            return formatter.format(amount != null ? amount : 0) + " VNĐ";
        }
        
        public String getFormattedDate() {
            if (createdAt == null) return "";
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            return sdf.format(createdAt.toDate());
        }
        
        public String getFormattedExpiry() {
            if (expiresAt == null) return "";
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            return sdf.format(expiresAt.toDate());
        }
    }
}
