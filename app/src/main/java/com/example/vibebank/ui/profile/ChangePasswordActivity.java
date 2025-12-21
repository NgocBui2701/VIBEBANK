package com.example.vibebank.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.vibebank.R;
import com.example.vibebank.ui.OtpBottomSheetDialog;
import com.example.vibebank.ui.base.BaseActivity;
import com.example.vibebank.ui.login.LoginActivity;
import com.example.vibebank.utils.PasswordUtils;
import com.example.vibebank.utils.PasswordValidationHelper;
import com.example.vibebank.utils.PhoneAuthManager;
import com.example.vibebank.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ChangePasswordActivity extends BaseActivity implements
        PhoneAuthManager.PhoneAuthCallback,
        OtpBottomSheetDialog.OtpVerificationListener {

    private static final String TAG = "ChangePasswordActivity";

    // Views
    private ImageView btnBack;
    private MaterialButton btnUpdatePassword;
    private TextInputEditText edtCurrPassword, edtNewPassword, edtConfirmPassword;
    private TextView tvRuleLength, tvRuleUpperLower, tvRuleDigit, tvRuleSpecial;

    // Helpers & Logic
    private ChangePasswordViewModel viewModel;
    private SessionManager sessionManager;
    private PasswordValidationHelper passwordHelper;
    private PhoneAuthManager phoneAuthManager;

    // Data
    private String currentUserId;
    private String userPhone;
    private String storedHashFromDb = ""; // Lưu hash từ DB để so sánh local

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_password);

        setupWindowInsets();

        // Init Core
        sessionManager = new SessionManager(this);
        viewModel = new ViewModelProvider(this).get(ChangePasswordViewModel.class);

        // Lấy User ID và Phone từ Session
        currentUserId = sessionManager.getCurrentUserId();
        userPhone = sessionManager.getSavedPhone();

        initViews();
        setupHelpers();
        setupData();
        setupListeners();
        setupObservers();
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.changePassword), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnUpdatePassword = findViewById(R.id.btnConfirm); // ID theo XML layout

        edtCurrPassword = findViewById(R.id.edtCurrPassword);
        edtNewPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);

        tvRuleLength = findViewById(R.id.tvRuleLength);
        tvRuleUpperLower = findViewById(R.id.tvRuleUpperLower);
        tvRuleDigit = findViewById(R.id.tvRuleDigit);
        tvRuleSpecial = findViewById(R.id.tvRuleSpecial);
    }

    private void setupHelpers() {
        // Init PhoneAuth
        phoneAuthManager = new PhoneAuthManager(this, this);

        // Init Validation Helper (Cho mật khẩu mới)
        passwordHelper = new PasswordValidationHelper(
                this, edtNewPassword, edtConfirmPassword,
                tvRuleLength, tvRuleUpperLower, tvRuleDigit, tvRuleSpecial
        );
    }

    private void setupData() {
        // Gọi ViewModel lấy Hash mật khẩu hiện tại ngay khi vào màn hình
        viewModel.fetchCurrentPasswordHash(currentUserId);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // Bấm nút cập nhật
        btnUpdatePassword.setOnClickListener(v -> handleUpdateClick());
    }

    private void setupObservers() {
        // Lắng nghe Hash từ DB trả về
        viewModel.currentHash.observe(this, hash -> {
            this.storedHashFromDb = hash;
        });

        // Lắng nghe kết quả Update
        viewModel.updateResult.observe(this, error -> {
            if (error == null) {
                showSuccessAndLogout();
            } else {
                showErrorDialog(error);
                btnUpdatePassword.setEnabled(true);
            }
        });

        // Lắng nghe Loading
        viewModel.isLoading.observe(this, isLoading -> {
            btnUpdatePassword.setEnabled(!isLoading);
            btnUpdatePassword.setText(isLoading ? "ĐANG XỬ LÝ..." : "CẬP NHẬT MẬT KHẨU");
        });
    }

    // --- LOGIC HANDLERS ---

    private void handleUpdateClick() {
        // 1. Validate Mật khẩu cũ
        if (!validateCurrentPassword()) {
            return;
        }

        // 2. Validate Mật khẩu mới (Format + Khớp Confirm)
        if (!passwordHelper.isValid()) {
            return;
        }

        // 3. Validate Logic: Mới không được trùng Cũ
        String oldPass = edtCurrPassword.getText().toString();
        String newPass = edtNewPassword.getText().toString();

        if (oldPass.equals(newPass)) {
            showErrorDialog("Mật khẩu mới không được trùng với mật khẩu hiện tại.");
            return;
        }

        // Tất cả OK -> Bắt đầu quy trình OTP
        startOtpFlow();
    }

    private boolean validateCurrentPassword() {
        String inputOldPass = edtCurrPassword.getText().toString();

        if (inputOldPass.isEmpty()) {
            edtCurrPassword.setError("Vui lòng nhập mật khẩu hiện tại");
            return false;
        }

        if (storedHashFromDb == null || storedHashFromDb.isEmpty()) {
            // Chưa tải xong dữ liệu -> Chặn lại cho an toàn
            return false;
        }

        // Check Hash
        boolean isCorrect = PasswordUtils.checkPassword(inputOldPass, storedHashFromDb);

        if (!isCorrect) {
            edtCurrPassword.setError("Mật khẩu hiện tại không đúng");
            return false;
        }

        edtCurrPassword.setError(null);
        return true;
    }

    private void startOtpFlow() {
        btnUpdatePassword.setEnabled(false);
        btnUpdatePassword.setText("ĐANG GỬI OTP...");
        phoneAuthManager.sendOtp(userPhone);
    }

    // --- PHONE AUTH CALLBACKS ---

    @Override
    public void onCodeSent() {
        btnUpdatePassword.setEnabled(true);
        btnUpdatePassword.setText("CẬP NHẬT MẬT KHẨU");
        showOtpBottomSheet();
    }

    @Override
    public void onVerificationSuccess() {
        // OTP Đúng -> Tiến hành cập nhật DB
        String newPassRaw = passwordHelper.getPassword();
        viewModel.updatePassword(currentUserId, newPassRaw);
    }

    @Override
    public void onVerificationFailed(String error) {
        btnUpdatePassword.setEnabled(true);
        btnUpdatePassword.setText("CẬP NHẬT MẬT KHẨU");
        showErrorDialog(error);
    }

    // --- OTP DIALOG LISTENER ---

    @Override
    public void onOtpVerified(String otpCode) {
        phoneAuthManager.verifyCode(otpCode);
    }

    @Override
    public void onResendOtp() {
        phoneAuthManager.resendOtp(userPhone);
    }

    // --- UI & NAVIGATION ---

    private void showOtpBottomSheet() {
        OtpBottomSheetDialog otpDialog = OtpBottomSheetDialog.newInstance("Nhập mã xác thực gửi tới " + userPhone);
        otpDialog.setOtpVerificationListener(this);
        otpDialog.show(getSupportFragmentManager(), "OtpBottomSheet");
    }

    private void showSuccessAndLogout() {
        Toast.makeText(this, "Đổi mật khẩu thành công! Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();

        // Logout và về Login
        sessionManager.logout();
    }
}