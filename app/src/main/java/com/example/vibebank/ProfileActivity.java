package com.example.vibebank;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.vibebank.ui.login.LoginActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.button.MaterialButton;

public class ProfileActivity extends AppCompatActivity {

    // Views
    private ImageView btnBack, btnEditProfile, btnChangeAvatar, imgAvatar;
    private TextView txtUserName, txtPhone;
    private TextView txtAccountNumber, txtAccountType, txtOpenDate;
    private TextView btnEditPersonalInfo, btnEditContactInfo;
    private LinearLayout btnChangePassword, btnLanguage;
    private SwitchMaterial switchBiometric, switchNightMode, switchNotification;
    private MaterialButton btnLogout;
    private TextView txtCurrentLanguage;

    // Profile Info Items
    private View itemBirthday, itemGender, itemCCCD, itemAddress, itemIssueDate;
    private View itemContactPhone, itemEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initViews();
        loadUserData();
        setupListeners();
    }

    private void initViews() {
        // Header
        btnBack = findViewById(R.id.btnBack);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        imgAvatar = findViewById(R.id.imgAvatar);

        // User Info
        txtUserName = findViewById(R.id.txtUserName);
        txtPhone = findViewById(R.id.txtPhone);

        // Account Info
        txtAccountNumber = findViewById(R.id.txtAccountNumber);
        txtAccountType = findViewById(R.id.txtAccountType);
        txtOpenDate = findViewById(R.id.txtOpenDate);

        // Personal Info
        btnEditPersonalInfo = findViewById(R.id.btnEditPersonalInfo);
        itemBirthday = findViewById(R.id.itemBirthday);
        itemGender = findViewById(R.id.itemGender);
        itemCCCD = findViewById(R.id.itemCCCD);
        itemAddress = findViewById(R.id.itemAddress);
        itemIssueDate = findViewById(R.id.itemIssueDate);

        // Contact Info
        btnEditContactInfo = findViewById(R.id.btnEditContactInfo);
        itemContactPhone = findViewById(R.id.itemContactPhone);
        itemEmail = findViewById(R.id.itemEmail);

        // Security
        btnChangePassword = findViewById(R.id.btnChangePassword);
        switchBiometric = findViewById(R.id.switchBiometric);

        // Settings
        switchNightMode = findViewById(R.id.switchNightMode);
        btnLanguage = findViewById(R.id.btnLanguage);
        txtCurrentLanguage = findViewById(R.id.txtCurrentLanguage);
        switchNotification = findViewById(R.id.switchNotification);

        // Logout
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void loadUserData() {
        // TODO: Load from database
        // For now, using sample data

        // User basic info
        txtUserName.setText("TRẦN NHỰT ĐÔNG KHÔI");
        txtPhone.setText("0364953666");

        // Account info
        txtAccountNumber.setText("1234 5678 9012");
        txtAccountType.setText("Cá nhân");
        txtOpenDate.setText("15/08/2023");

        // Personal info
        setProfileInfoItem(itemBirthday, "Ngày sinh", "25/09/2005");
        setProfileInfoItem(itemGender, "Giới tính", "Nam");
        setProfileInfoItem(itemCCCD, "CCCD", "089205001234");
        setProfileInfoItem(itemAddress, "Nơi thường trú", "Xã Thoại Sơn, Tỉnh An Giang");
        setProfileInfoItem(itemIssueDate, "Ngày cấp", "22/11/2021");

        // Contact info
        setProfileInfoItem(itemContactPhone, "Số điện thoại", "0375092732");
        setProfileInfoItem(itemEmail, "Email", "dongkhoidev@gmail.com");

        // Settings
        switchBiometric.setChecked(true);
        switchNightMode.setChecked(false);
        switchNotification.setChecked(true);
        txtCurrentLanguage.setText("Tiếng Việt");
    }

    private void setProfileInfoItem(View itemView, String label, String value) {
        TextView txtLabel = itemView.findViewById(R.id.txtLabel);
        TextView txtValue = itemView.findViewById(R.id.txtValue);
        txtLabel.setText(label);
        txtValue.setText(value);
    }

    private void setupListeners() {
        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Edit profile button (top right)
        btnEditProfile.setOnClickListener(v -> {
            // TODO: Open edit profile screen
            showToast("Chỉnh sửa hồ sơ");
        });

        // Change avatar
        btnChangeAvatar.setOnClickListener(v -> {
            // TODO: Open image picker
            showToast("Chọn ảnh đại diện");
        });

        // Edit personal info
        btnEditPersonalInfo.setOnClickListener(v -> {
            // TODO: Open edit personal info dialog
            showToast("Chỉnh sửa thông tin cá nhân");
        });

        // Edit contact info
        btnEditContactInfo.setOnClickListener(v -> {
            // TODO: Open edit contact info dialog
            showToast("Chỉnh sửa thông tin liên hệ");
        });

        // Change password
        btnChangePassword.setOnClickListener(v -> {
            // TODO: Open change password screen
            showToast("Đổi mật khẩu");
        });

        // Biometric toggle
        switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // TODO: Enable/disable biometric authentication
            if (isChecked) {
                showToast("Đã bật sinh trắc học");
            } else {
                showToast("Đã tắt sinh trắc học");
            }
        });

        // Night mode toggle
        switchNightMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // TODO: Switch app theme
            if (isChecked) {
                showToast("Chế độ tối đang được phát triển");
            } else {
                showToast("Chế độ sáng");
            }
        });

        // Language selection
        btnLanguage.setOnClickListener(v -> {
            // TODO: Show language selection dialog
            showToast("Chọn ngôn ngữ");
        });

        // Notification toggle
        switchNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // TODO: Enable/disable notifications
            if (isChecked) {
                showToast("Đã bật thông báo");
            } else {
                showToast("Đã tắt thông báo");
            }
        });

        // Logout
        btnLogout.setOnClickListener(v -> {
            // TODO: Show logout confirmation dialog
            showLogoutConfirmation();
        });
    }

    private void showLogoutConfirmation() {
//        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
//                .setTitle("Đăng xuất")
//                .setMessage("Bạn có chắc chắn muốn đăng xuất?")
//                .setPositiveButton("Đăng xuất", (d, w) -> sessionManager.logoutUser())
//                .setNegativeButton("Hủy", null)
//                .show();
//        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
//                .setTextColor(android.graphics.Color.parseColor("#4CAF50"));
//        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
//                .setTextColor(android.graphics.Color.parseColor("#FF0000"));
    }

    private void showToast(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }
}
