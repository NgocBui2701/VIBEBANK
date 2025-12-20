package com.example.vibebank.ui.forgotpassword;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.vibebank.R;
import com.example.vibebank.ui.OtpBottomSheetDialog;
import com.example.vibebank.ui.base.BaseActivity;
import com.example.vibebank.utils.PhoneAuthManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ForgotPasswordActivity extends BaseActivity {
    private static final String TAG = "ForgotPasswordActivity";

    private ImageView btnBack;
    private MaterialButton btnNext;
    private TextInputEditText edtFullName, edtCCCD, edtPhone;

    private ForgotPasswordViewModel viewModel;
    private PhoneAuthManager phoneAuthManager;
    private String matchedUid;
    private String currentPhoneNumber;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.forgotPassword), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(ForgotPasswordViewModel.class);

        phoneAuthManager = new PhoneAuthManager(this, new PhoneAuthManager.PhoneAuthCallback() {
            @Override
            public void onCodeSent() {
                // Đã gửi SMS -> Mở Dialog nhập OTP
                btnNext.setEnabled(true);
                btnNext.setText("Tiếp tục");
                showOtpDialog();
            }

            @Override
            public void onVerificationSuccess() {
                goToResetPassword();
            }

            @Override
            public void onVerificationFailed(String error) {
                // Lỗi xác thực
                btnNext.setEnabled(true);
                btnNext.setText("Tiếp tục");
                showErrorDialog(error);
            }
        });

        initViews();
        setupObservers();

        btnBack.setOnClickListener(v -> finish());

        btnNext.setOnClickListener(v -> {
            String fullName = edtFullName.getText().toString().trim();
            String cccd = edtCCCD.getText().toString().trim();
            String phone = edtPhone.getText().toString().trim();

            if (fullName.isEmpty() || cccd.isEmpty() || phone.isEmpty()) {
                showErrorDialog("Vui lòng điền đầy đủ thông tin");
                return;
            }

            // 1. Gọi ViewModel kiểm tra thông tin trong Database trước
            viewModel.verifyUserInfo(fullName, cccd, phone);
        });
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnNext = findViewById(R.id.btnNext);
        edtFullName = findViewById(R.id.edtFullName);
        edtCCCD = findViewById(R.id.edtCCCD);
        edtPhone = findViewById(R.id.edtPhone);
        edtFullName.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);
    }

    private void setupObservers() {
        // Lắng nghe lỗi
        viewModel.errorMessage.observe(this, error -> {
            if (error != null) {
                showErrorDialog(error);
                // Reset error để tránh hiện lại khi xoay màn hình
                viewModel.errorMessage.setValue(null);
            }
        });

        // Lắng nghe loading
        viewModel.isLoading.observe(this, isLoading -> {
            btnNext.setEnabled(!isLoading);
            btnNext.setText(isLoading ? "Đang kiểm tra..." : "Tiếp tục");
        });

        // Lắng nghe kết quả thành công (Tìm thấy UID khớp)
        viewModel.verifiedUid.observe(this, uid -> {
            if (uid != null) {
                this.matchedUid = uid;
                this.currentPhoneNumber = edtPhone.getText().toString().trim();

                // Gửi OTP thông qua Manager
                btnNext.setEnabled(false);
                btnNext.setText("Đang gửi OTP...");
                phoneAuthManager.sendOtp(currentPhoneNumber);
            }
        });
    }

    private void showOtpDialog() {
        // Manager đã quản lý VerificationID nên truyền null
        OtpBottomSheetDialog otpDialog = OtpBottomSheetDialog.newInstance(null);

        otpDialog.setOtpVerificationListener(new OtpBottomSheetDialog.OtpVerificationListener() {
            @Override
            public void onOtpVerified(String otp) {
                // Gọi Manager check code
                phoneAuthManager.verifyCode(otp);
            }

            @Override
            public void onResendOtp() {
                // Gọi Manager gửi lại code
                phoneAuthManager.resendOtp(currentPhoneNumber);
            }
        });
        otpDialog.show(getSupportFragmentManager(), "OtpBottomSheet");
    }

    private void goToResetPassword() {
        Intent intent = new Intent(ForgotPasswordActivity.this, ResetPasswordActivity.class);
        intent.putExtra("uid", matchedUid);
        intent.putExtra("phone", currentPhoneNumber);
        startActivity(intent);
        finish();
    }
}