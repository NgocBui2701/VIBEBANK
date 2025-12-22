package com.example.vibebank.ui.profile;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.vibebank.R;
import com.example.vibebank.utils.BiometricHelper;
import com.example.vibebank.utils.CloudinaryHelper;
import com.example.vibebank.utils.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

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

    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;

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
        CloudinaryHelper.initCloudinary(this);

        initViews();
        setupImagePicker();
        setupListeners();
        setupObservers();

        String uid = sessionManager.getCurrentUserId();
        viewModel.loadUserProfile(uid);
    }

    private void setupImagePicker() {
        pickMedia = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                // A. Hiển thị ảnh xem trước ngay lập tức cho mượt (UI Feedback)
                Glide.with(this)
                        .load(uri)
                        .circleCrop()
                        .into(imgAvatar);

                // B. Gọi ViewModel để xử lý Upload ngầm
                String userId = sessionManager.getCurrentUserId();
                viewModel.uploadAvatar(uri, userId);
            } else {
                showToast("Đã hủy chọn ảnh");
            }
        });
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

        viewModel.avatarUrl.observe(this, url -> {
            if (url != null && !url.isEmpty()) {
                Glide.with(this)
                        .load(url)
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .error(R.drawable.ic_avatar_placeholder)
                        .circleCrop()
                        .into(imgAvatar);
                sessionManager.saveAvatarUrl(url);
            }
        });
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
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        // Edit personal info
        btnEditPersonalInfo.setOnClickListener(v -> showEditPersonalDialog());

        // Edit contact info
        btnEditContactInfo.setOnClickListener(v -> showEditContactDialog());

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

    private void showEditPersonalDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_personal, null);

        TextInputEditText edtAddress = view.findViewById(R.id.edtEditAddress);
        TextInputEditText edtBirthday = view.findViewById(R.id.edtEditBirthday);
        AutoCompleteTextView edtGender = view.findViewById(R.id.edtEditGender);

        edtAddress.setText(viewModel.address.getValue());
        edtBirthday.setText(viewModel.birthday.getValue());
        edtGender.setText(viewModel.gender.getValue());

        String[] genders = new String[]{"Nam", "Nữ"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line, // Layout item
                genders
        );
        edtGender.setAdapter(adapter);

        String currentGender = viewModel.gender.getValue();
        if (currentGender != null && !currentGender.isEmpty()) {
            edtGender.setText(currentGender, false);
        } else {
            edtGender.setText("Nam", false);
        }

        edtGender.setOnClickListener(v -> edtGender.showDropDown());

        edtBirthday.setOnClickListener(v -> {
            // Lấy ngày hiện tại để mặc định hiển thị
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            java.util.Calendar eighteenYearsAgo = java.util.Calendar.getInstance();
            eighteenYearsAgo.add(java.util.Calendar.YEAR, -18);

            // Nếu đã có ngày sinh cũ, parse ra để hiển thị đúng ngày đó trên lịch
            try {
                String currentDob = edtBirthday.getText().toString();
                if (!currentDob.isEmpty()) {
                    java.util.Date date = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).parse(currentDob);
                    if (date != null) calendar.setTime(date);
                }
            } catch (Exception e) { e.printStackTrace(); }

            int year = calendar.get(java.util.Calendar.YEAR);
            int month = calendar.get(java.util.Calendar.MONTH);
            int day = calendar.get(java.util.Calendar.DAY_OF_MONTH);

            // Hiển thị lịch
            DatePickerDialog dialog = new DatePickerDialog(this, (view1, selectedYear, selectedMonth, selectedDay) -> {
                // Format thành dd/MM/yyyy
                String selectedDate = String.format(java.util.Locale.getDefault(), "%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear);
                edtBirthday.setText(selectedDate);
                edtBirthday.setError(null); // Xóa lỗi nếu có
            }, year, month, day);
            dialog.getDatePicker().setMaxDate(eighteenYearsAgo.getTimeInMillis());

            dialog.show();
        });

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setCancelable(false)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
                .create();

        dialog.show();

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.RED);
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.parseColor("#4CAF50"));

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newAddress = edtAddress.getText().toString().trim();
            String newBirthday = edtBirthday.getText().toString().trim();
            String newGender = edtGender.getText().toString().trim();

            String oldAddress = viewModel.address.getValue();
            String oldBirthday = viewModel.birthday.getValue();
            String oldGender = viewModel.gender.getValue();

            java.util.regex.Pattern ADDRESS_PATTERN = java.util.regex.Pattern.compile("^[\\p{L}0-9\\s,./-]+$");

            if (newAddress.isEmpty() || newAddress.length() < 10 || !ADDRESS_PATTERN.matcher(newAddress).matches()) {
                edtAddress.setError("Địa chỉ không hợp lệ");
                return;
            }

            boolean isAddressSame = newAddress.equals(oldAddress);
            boolean isBirthdaySame = newBirthday.equals(oldBirthday);
            boolean isGenderSame = newGender.equals(oldGender);

            if (isAddressSame && isBirthdaySame && isGenderSame) {
                dialog.dismiss();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            // Chỉ put cái nào thay đổi (Tối ưu)
            if (!isAddressSame) updates.put("address", newAddress);
            if (!isBirthdaySame) updates.put("birth_date", newBirthday);
            if (!isGenderSame) updates.put("gender", newGender);

            dialog.dismiss();

            showConfirmSecurityDialog(() -> {
                String uid = sessionManager.getCurrentUserId();
                viewModel.updateUserProfile(uid, updates);
            });
        });
    }

    private void showEditContactDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_contact, null);
        TextInputEditText edtEmail = view.findViewById(R.id.edtEditEmail);

        // Điền email hiện tại
        edtEmail.setText(viewModel.email.getValue());

        new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setCancelable(false)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", (dialog, which) -> {
                })
                .create();

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setCancelable(false)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
                .show();

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newEmail = edtEmail.getText().toString().trim();
            String currentEmail = viewModel.email.getValue();

            // 1. Validate cơ bản
            if (newEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                edtEmail.setError("Email không hợp lệ");
            }
            if (newEmail.equals(currentEmail)) {
                dialog.dismiss();
                return;
            }

            // 2. Check trùng Email
            viewModel.checkEmailExists(newEmail, isExists -> {
                if (isExists) {
                    edtEmail.setError("Email này đã được sử dụng bởi tài khoản khác!");
                } else {
                    // Email hợp lệ -> Đóng dialog nhập liệu -> Mở dialog xác thực
                    dialog.dismiss();
                    showConfirmSecurityDialog(() -> {
                        // Hành động cần làm khi pass đúng: Update Email
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("email", newEmail);
                        String uid = sessionManager.getCurrentUserId();
                        viewModel.updateUserProfile(uid, updates);
                    });
                }
            });
        });

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(android.graphics.Color.RED);

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setTextColor(android.graphics.Color.parseColor("#4CAF50"));
    }

    private void showConfirmSecurityDialog(Runnable onSuccessAction) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_password, null);
        TextInputEditText edtPass = view.findViewById(R.id.edtConfirmPass);

        androidx.appcompat.app.AlertDialog authDialog = new MaterialAlertDialogBuilder(this)
                .setView(view)
                .setCancelable(false)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xác nhận", null) // Override sau
                .show();

        authDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(android.graphics.Color.RED); // Hủy -> Đỏ

        authDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setTextColor(android.graphics.Color.parseColor("#4CAF50")); // Xác nhận -> Xanh lá

        authDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String password = edtPass.getText().toString().trim();

            if (password.isEmpty()) {
                edtPass.setError("Vui lòng nhập mật khẩu");
                return;
            }

            // Gọi ViewModel check password
            if (viewModel.validatePassword(password)) {
                // MẬT KHẨU ĐÚNG
                authDialog.dismiss();

                // ---> CHẠY HÀNH ĐỘNG ĐƯỢC TRUYỀN VÀO <---
                if (onSuccessAction != null) {
                    onSuccessAction.run();
                }
            } else {
                edtPass.setError("Mật khẩu không đúng!");
            }
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
