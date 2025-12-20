package com.example.vibebank.ui.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.vibebank.R;
import com.example.vibebank.utils.SessionManager;
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

    private ProfileViewModel viewModel;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sessionManager = new SessionManager(this);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        initViews();
        setupListeners();
        setupObservers();

        String uid = sessionManager.getUserId();
        viewModel.loadUserProfile(uid);
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
        switchNotification.setChecked(true);
        txtCurrentLanguage.setText("Tiếng Việt");
    }

    private void setProfileInfoItem(View itemView, String label, String value) {
        TextView txtLabel = itemView.findViewById(R.id.txtLabel);
        TextView txtValue = itemView.findViewById(R.id.txtValue);
        txtLabel.setText(label);

        if (value == null || value.isEmpty()) {
            txtValue.setText("Chưa cập nhật");
        } else {
            txtValue.setText(value);
        }
    }

    private void setupObservers() {
        // Quan sát dữ liệu từ ViewModel và cập nhật UI

        // 1. Header Info
        viewModel.fullName.observe(this, name -> {
            if (name != null) txtUserName.setText(name.toUpperCase());
        });
        viewModel.phone.observe(this, phone -> {
            if (phone != null) {
                txtPhone.setText(phone);
                setProfileInfoItem(itemContactPhone, "Số điện thoại", phone);
            }
        });

        // 2. Account Info
        viewModel.accountNumber.observe(this, accNum -> txtAccountNumber.setText(accNum));
        viewModel.accountType.observe(this, type -> txtAccountType.setText(type));
        viewModel.openDate.observe(this, date -> txtOpenDate.setText(date));

        // 3. Personal Info
        viewModel.birthday.observe(this, val -> setProfileInfoItem(itemBirthday, "Ngày sinh", val));
        viewModel.gender.observe(this, val -> setProfileInfoItem(itemGender, "Giới tính", val));
        viewModel.cccd.observe(this, val -> setProfileInfoItem(itemCCCD, "CCCD/CMND", val));
        viewModel.address.observe(this, val -> setProfileInfoItem(itemAddress, "Địa chỉ", val));
        viewModel.issueDate.observe(this, val -> setProfileInfoItem(itemIssueDate, "Ngày cấp", val));

        // 4. Contact Info
        viewModel.email.observe(this, val -> setProfileInfoItem(itemEmail, "Email", val));
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
            showLogoutConfirmation();
        });
    }

    private void showLogoutConfirmation() {
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất khỏi ứng dụng?")
                .setPositiveButton("Đăng xuất", (d, w) -> {
                    sessionManager.logoutUser();
                    finishAffinity();
                })
                .setNegativeButton("Hủy", (d, w) -> d.dismiss())
                .show();
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setTextColor(android.graphics.Color.parseColor("#4CAF50"));

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(android.graphics.Color.parseColor("#F44336"));
    }

    private void showToast(String message) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
    }
}
