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

import com.bumptech.glide.Glide;
import com.example.vibebank.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EkycVerificationActivity extends AppCompatActivity {

    private RecyclerView recyclerPending;
    private LinearLayout layoutEmpty;
    private ImageView btnBack;
    
    private FirebaseFirestore db;
    private List<KycDocument> pendingKyc = new ArrayList<>();
    private KycAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ekyc_verification);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        setupListeners();
        loadPendingKyc();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        recyclerPending = findViewById(R.id.recyclerPending);
        layoutEmpty = findViewById(R.id.layoutEmpty);
    }

    private void setupRecyclerView() {
        adapter = new KycAdapter(pendingKyc, this::showKycDetail);
        recyclerPending.setLayoutManager(new LinearLayoutManager(this));
        recyclerPending.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadPendingKyc() {
        pendingKyc.clear();

        db.collection("kyc_documents")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot kycDoc : querySnapshot.getDocuments()) {
                        String userId = kycDoc.getId();
                        String idNumber = kycDoc.getString("id_number");
                        String frontUrl = kycDoc.getString("front_image_url");
                        String backUrl = kycDoc.getString("back_image_url");

                        // Load user info
                        db.collection("users").document(userId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        String fullName = userDoc.getString("full_name");
                                        String phone = userDoc.getString("phone_number");

                                        KycDocument kyc = new KycDocument(
                                                userId, fullName, phone, idNumber, frontUrl, backUrl
                                        );
                                        pendingKyc.add(kyc);
                                        adapter.notifyDataSetChanged();

                                        updateEmptyState();
                                    }
                                });
                    }
                });
    }

    private void updateEmptyState() {
        if (pendingKyc.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerPending.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerPending.setVisibility(View.VISIBLE);
        }
    }

    private void showKycDetail(KycDocument kyc) {
        // Show dialog with images and approve/reject buttons
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_kyc_verification, null);
        builder.setView(dialogView);

        TextView tvName = dialogView.findViewById(R.id.tvKycName);
        TextView tvIdNumber = dialogView.findViewById(R.id.tvKycIdNumber);
        ImageView imgFront = dialogView.findViewById(R.id.imgFrontId);
        ImageView imgBack = dialogView.findViewById(R.id.imgBackId);
        MaterialButton btnApprove = dialogView.findViewById(R.id.btnApprove);
        MaterialButton btnReject = dialogView.findViewById(R.id.btnReject);

        tvName.setText(kyc.fullName);
        tvIdNumber.setText("CCCD: " + kyc.idNumber);

        // Load images
        if (kyc.frontUrl != null && !kyc.frontUrl.isEmpty()) {
            Glide.with(this).load(kyc.frontUrl).into(imgFront);
        }
        if (kyc.backUrl != null && !kyc.backUrl.isEmpty()) {
            Glide.with(this).load(kyc.backUrl).into(imgBack);
        }

        AlertDialog dialog = builder.create();

        btnApprove.setOnClickListener(v -> {
            updateKycStatus(kyc.userId, "verified");
            dialog.dismiss();
        });

        btnReject.setOnClickListener(v -> {
            updateKycStatus(kyc.userId, "rejected");
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateKycStatus(String userId, String status) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);

        db.collection("kyc_documents").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    String message = "verified".equals(status) ? "Đã duyệt" : "Đã từ chối";
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    loadPendingKyc(); // Reload list
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Inner class for KYC document
    static class KycDocument {
        String userId;
        String fullName;
        String phone;
        String idNumber;
        String frontUrl;
        String backUrl;

        KycDocument(String userId, String fullName, String phone, String idNumber, 
                   String frontUrl, String backUrl) {
            this.userId = userId;
            this.fullName = fullName;
            this.phone = phone;
            this.idNumber = idNumber;
            this.frontUrl = frontUrl;
            this.backUrl = backUrl;
        }
    }

    // Simple adapter for KYC list
    static class KycAdapter extends RecyclerView.Adapter<KycAdapter.KycViewHolder> {
        private List<KycDocument> documents;
        private OnKycClickListener listener;

        interface OnKycClickListener {
            void onClick(KycDocument kyc);
        }

        KycAdapter(List<KycDocument> documents, OnKycClickListener listener) {
            this.documents = documents;
            this.listener = listener;
        }

        @androidx.annotation.NonNull
        @Override
        public KycViewHolder onCreateViewHolder(@androidx.annotation.NonNull android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_customer, parent, false);
            return new KycViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@androidx.annotation.NonNull KycViewHolder holder, int position) {
            KycDocument kyc = documents.get(position);
            holder.bind(kyc, listener);
        }

        @Override
        public int getItemCount() {
            return documents.size();
        }

        static class KycViewHolder extends RecyclerView.ViewHolder {
            TextView tvCustomerName, tvAccountNumber, tvKycStatus;

            KycViewHolder(@androidx.annotation.NonNull View itemView) {
                super(itemView);
                tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
                tvAccountNumber = itemView.findViewById(R.id.tvAccountNumber);
                tvKycStatus = itemView.findViewById(R.id.tvKycStatus);
            }

            void bind(KycDocument kyc, OnKycClickListener listener) {
                tvCustomerName.setText(kyc.fullName);
                tvAccountNumber.setText("SĐT: " + kyc.phone);
                tvKycStatus.setText("Chờ duyệt");
                tvKycStatus.setBackgroundColor(android.graphics.Color.parseColor("#FF9800"));

                itemView.setOnClickListener(v -> listener.onClick(kyc));
            }
        }
    }
}


