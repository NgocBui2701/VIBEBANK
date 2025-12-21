package com.example.vibebank.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.vibebank.R;
import com.example.vibebank.utils.BiometricHelper;
import com.example.vibebank.utils.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.button.MaterialButton;

import javax.crypto.Cipher;

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
    private BiometricHelper biometricHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profile), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sessionManager = new SessionManager(this);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        biometricHelper = new BiometricHelper();

        initViews();
        setupListeners();
        setupObservers();

        String uid = sessionManager.getCurrentUserId();
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
        switchBiometric.setChecked(sessionManager.isBiometricEnabled());

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
            Intent intent = new Intent(this, ChangePasswordActivity.class);
            startActivity(intent);

        });

        // Biometric toggle
        switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                if (isChecked) {
                    enableBiometric();
                } else {
                    disableBiometric();
                }
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

    private void enableBiometric() {
        androidx.biometric.BiometricManager biometricManager = androidx.biometric.BiometricManager.from(this);
        int canAuthenticate = biometricManager.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG);

        if (canAuthenticate != androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS) {
            showToast("Thiết bị không hỗ trợ sinh trắc học bảo mật cao");
            switchBiometric.setChecked(false);
            return;
        }
        try {
            // 1. Tạo Key (nếu chưa có)
            biometricHelper.generateSecretKey();

            // 2. Lấy Cipher để chuẩn bị mã hóa
            Cipher cipher;
            try {
                cipher = biometricHelper.getCipherForEncryption();
            } catch (android.security.keystore.KeyPermanentlyInvalidatedException e) {
                // Lỗi này xảy ra khi user thêm/xóa vân tay mới trong cài đặt máy
                // -> Key cũ bị vô hiệu hóa -> Cần xóa đi tạo lại
                biometricHelper.deleteKey();
                showToast("Dữ liệu vân tay đã thay đổi. Vui lòng tắt và bật lại lần nữa để cập nhật.");
                switchBiometric.setChecked(false);
                return;
            } catch (Exception e) {
                // Các lỗi khác (như bị khóa do sai quá nhiều lần)
                // Ta cũng xóa Key đi cho chắc ăn, để lần sau nó refresh lại trạng thái
                biometricHelper.deleteKey();

                showToast("Lỗi khởi tạo bảo mật. Vui lòng thử lại.");
                e.printStackTrace();
                switchBiometric.setChecked(false);
                return;
            }

            // 3. Hiển thị Biometric Prompt
            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Xác thực sinh trắc học")
                    .setSubtitle("Xác nhận để kích hoạt")
                    .setNegativeButtonText("Hủy")
                    .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .build();

            BiometricPrompt biometricPrompt = new BiometricPrompt(this,
                    ContextCompat.getMainExecutor(this),
                    new BiometricPrompt.AuthenticationCallback() {

                        @Override
                        public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);
                            try {
                                // 4. Lấy Cipher đã được mở khóa bởi vân tay
                                Cipher successCipher = result.getCryptoObject().getCipher();

                                // 5. Mã hóa Token hiện tại (Lấy từ Session đang đăng nhập)
                                String currentToken = sessionManager.getToken();
                                if (currentToken == null) {
                                    showToast("Phiên đăng nhập hết hạn, vui lòng đăng nhập lại");
                                    switchBiometric.setChecked(false);
                                    return;
                                }
                                byte[] encryptedBytes = successCipher.doFinal(currentToken.getBytes("UTF-8"));

                                // 6. Lưu dữ liệu quan trọng vào SharedPreferences
                                String encryptedTokenBase64 = Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
                                String ivBase64 = Base64.encodeToString(successCipher.getIV(), Base64.DEFAULT);

                                sessionManager.saveBiometricCredentials(encryptedTokenBase64, ivBase64);

                                showToast("Đã bật sinh trắc học thành công!");
                            } catch (Exception e) {
                                e.printStackTrace();
                                showToast("Lỗi mã hóa dữ liệu.");
                                // Rollback switch nếu lỗi
                                switchBiometric.setChecked(false);
                            }
                        }

                        @Override
                        public void onAuthenticationError(int errorCode, CharSequence errString) {
                            super.onAuthenticationError(errorCode, errString);
                            showToast("Lỗi xác thực");
                            switchBiometric.setChecked(false); // Tắt switch nếu user hủy hoặc lỗi
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            super.onAuthenticationFailed();
                            // Vân tay sai, cho thử lại, không cần tắt switch ngay
                        }
                    });

            // GỌI XÁC THỰC KÈM CRYPTO OBJECT (Quan trọng nhất)
            biometricPrompt.authenticate(promptInfo, new BiometricPrompt.CryptoObject(cipher));

        } catch (Exception e) {
            e.printStackTrace();
            showToast("Thiết bị không hỗ trợ hoặc lỗi khởi tạo.");
            switchBiometric.setChecked(false);
        }
    }

    private void disableBiometric() {
        // Xóa dữ liệu mã hóa và IV
        sessionManager.clearBiometricCredentials();
        showToast("Đã tắt sinh trắc học");
    }

    private void showLogoutConfirmation() {
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất khỏi ứng dụng?")
                .setPositiveButton("Đăng xuất", (d, w) -> {
                    sessionManager.logout();
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
