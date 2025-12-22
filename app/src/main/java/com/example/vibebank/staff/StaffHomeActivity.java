package com.example.vibebank.staff;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.example.vibebank.R;
import com.example.vibebank.ui.login.LoginActivity;
import com.example.vibebank.ui.profile.ProfileActivity;
import com.example.vibebank.utils.SessionManager;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class StaffHomeActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ImageView btnMenu, btnNotification;
    private TextView txtStaffName, txtTotalCustomers, txtPendingApprovals;
    private CardView btnCustomerList, btnCreateCustomer, btnEkycVerification, 
                     btnTransactionApproval, btnInterestRate, btnReports;

    private FirebaseFirestore db;
    private SessionManager sessionManager;
    private String currentStaffId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_home);

        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);
        currentStaffId = sessionManager.getCurrentUserId();

        if (currentStaffId == null) {
            Toast.makeText(this, "Lỗi phiên đăng nhập", Toast.LENGTH_SHORT).show();
            sessionManager.logout();
            finish();
            return;
        }

        initViews();
        setupListeners();
        loadStaffInfo();
        loadStatistics();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.staff_drawer_layout);
        btnMenu = findViewById(R.id.btnMenu);
        btnNotification = findViewById(R.id.btnNotification);
        txtStaffName = findViewById(R.id.txtStaffName);
        txtTotalCustomers = findViewById(R.id.txtTotalCustomers);
        txtPendingApprovals = findViewById(R.id.txtPendingApprovals);

        btnCustomerList = findViewById(R.id.btnCustomerList);
        btnCreateCustomer = findViewById(R.id.btnCreateCustomer);
        btnEkycVerification = findViewById(R.id.btnEkycVerification);
        btnTransactionApproval = findViewById(R.id.btnTransactionApproval);
        btnInterestRate = findViewById(R.id.btnInterestRate);
        btnReports = findViewById(R.id.btnReports);
    }

    private void setupListeners() {
        btnMenu.setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.END);
            }
        });

        btnNotification.setOnClickListener(v -> {
            Toast.makeText(this, "Thông báo nhân viên", Toast.LENGTH_SHORT).show();
        });

        // Main Functions
        btnCustomerList.setOnClickListener(v -> {
            Intent intent = new Intent(this, CustomerManagementActivity.class);
            startActivity(intent);
        });

        btnCreateCustomer.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateCustomerActivity.class);
            startActivity(intent);
        });

        btnEkycVerification.setOnClickListener(v -> {
            Intent intent = new Intent(this, EkycVerificationActivity.class);
            startActivity(intent);
        });

        btnTransactionApproval.setOnClickListener(v -> {
            Intent intent = new Intent(this, TransactionApprovalActivity.class);
            startActivity(intent);
        });

        btnInterestRate.setOnClickListener(v -> {
            Intent intent = new Intent(this, AccountInterestRateActivity.class);
            startActivity(intent);
        });

        btnReports.setOnClickListener(v -> {
            Toast.makeText(this, "Báo cáo (Coming soon)", Toast.LENGTH_SHORT).show();
        });

        // Navigation Drawer
        NavigationView navigationView = findViewById(R.id.staff_nav_view);
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(item -> {
                int itemId = item.getItemId();

                if (itemId == R.id.staff_nav_profile) {
                    Intent intent = new Intent(StaffHomeActivity.this, ProfileActivity.class);
                    startActivity(intent);
                    drawerLayout.closeDrawer(GravityCompat.END);
                    return true;
                } else if (itemId == R.id.staff_nav_settings) {
                    Toast.makeText(this, "Cài đặt", Toast.LENGTH_SHORT).show();
                    drawerLayout.closeDrawer(GravityCompat.END);
                    return true;
                } else if (itemId == R.id.staff_nav_logout) {
                    logout();
                    return true;
                }

                return false;
            });
        }
    }

    private void loadStaffInfo() {
        db.collection("users").document(currentStaffId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("full_name");
                        String avatarUrl = doc.getString("avatar_url");

                        if (name != null) {
                            txtStaffName.setText(name);
                        }

                        // Load avatar in nav header
                        NavigationView navView = findViewById(R.id.staff_nav_view);
                        if (navView != null) {
                            android.view.View headerView = navView.getHeaderView(0);
                            ImageView imgNavAvatar = headerView.findViewById(R.id.imgStaffNavAvatar);
                            TextView txtNavName = headerView.findViewById(R.id.txtStaffNavName);
                            android.widget.ImageButton btnClose = headerView.findViewById(R.id.btnCloseDrawer);

                            if (txtNavName != null && name != null) {
                                txtNavName.setText(name);
                            }

                            if (btnClose != null) {
                                btnClose.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.END));
                            }

                            if (imgNavAvatar != null && avatarUrl != null && !avatarUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(avatarUrl)
                                        .placeholder(R.drawable.ic_avatar_placeholder)
                                        .error(R.drawable.ic_avatar_placeholder)
                                        .circleCrop()
                                        .into(imgNavAvatar);
                            }
                        }
                    }
                });
    }

    private void loadStatistics() {
        // Load total customers
        db.collection("users")
                .whereEqualTo("role", "customer")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int total = querySnapshot.size();
                    txtTotalCustomers.setText(String.valueOf(total));
                });

        // Load pending eKYC approvals
        db.collection("kyc_documents")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int pending = querySnapshot.size();
                    txtPendingApprovals.setText(String.valueOf(pending));
                });
    }

    private void logout() {
        sessionManager.logout();
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }
}

