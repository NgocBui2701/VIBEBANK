package com.example.vibebank;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<NotificationItem> notificationList;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        db = FirebaseFirestore.getInstance();

        // Get current user ID
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        } else {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            currentUserId = prefs.getString("current_user_id", null);
        }

        if (currentUserId == null) {
            Toast.makeText(this, "Lỗi phiên đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        recyclerView = findViewById(R.id.recyclerNotifications);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmptyNotifications);
        View btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        // Setup RecyclerView
        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Load notifications
        loadNotifications();
    }

    private void loadNotifications() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        // Simple query without orderBy to avoid index requirement
        db.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    notificationList.clear();

                    if (querySnapshot.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            String title = doc.getString("title");
                            String message = doc.getString("message");
                            Date timestamp = doc.getDate("timestamp");
                            Boolean isRead = doc.getBoolean("isRead");

                            if (title != null && message != null && timestamp != null) {
                                notificationList.add(new NotificationItem(
                                        title,
                                        message,
                                        timestamp,
                                        isRead != null ? isRead : false
                                ));
                            }
                        }
                        
                        // Sort by timestamp descending (newest first)
                        notificationList.sort((a, b) -> b.timestamp.compareTo(a.timestamp));
                        
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("Lỗi tải thông báo");
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Notification item class
    public static class NotificationItem {
        public String title;
        public String message;
        public Date timestamp;
        public boolean isRead;

        public NotificationItem(String title, String message, Date timestamp, boolean isRead) {
            this.title = title;
            this.message = message;
            this.timestamp = timestamp;
            this.isRead = isRead;
        }

        public String getFormattedTime() {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            return sdf.format(timestamp);
        }
    }
}
