package com.example.vibebank.ui.register;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import androidx.lifecycle.ViewModelProvider;

import com.example.vibebank.R;
import com.example.vibebank.ui.OtpBottomSheetDialog;
import com.example.vibebank.ui.base.BaseActivity;
import com.example.vibebank.utils.PhoneAuthManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterActivity extends BaseActivity {
    private MaterialButton btnNext;
    private TextInputEditText edtCCCD, edtPhone, edtEmail;
    private RegisterViewModel viewModel;
    private PhoneAuthManager phoneAuthManager;
    private String currentPhoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        phoneAuthManager = new PhoneAuthManager(this, new PhoneAuthManager.PhoneAuthCallback() {
            @Override
            public void onCodeSent() {
                // Đã gửi tin nhắn xong -> Hiện Dialog nhập OTP
                btnNext.setEnabled(true);
                btnNext.setText("Tiếp tục");
                showOtpDialog();
            }

            @Override
            public void onVerificationSuccess() {
                // Lưu dữ liệu và chuyển màn hình
                viewModel.saveStep1(
                        edtEmail.getText().toString(),
                        edtPhone.getText().toString(),
                        edtCCCD.getText().toString()
                );

                Intent intent = new Intent(RegisterActivity.this, Register2Activity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onVerificationFailed(String error) {
                // Có lỗi xảy ra (Sai OTP, Lỗi mạng...)
                btnNext.setEnabled(true);
                btnNext.setText("Tiếp tục");
                showErrorDialog(error);
            }
        });

        initViews();
        setupRealtimeCheck();
        setupObservers();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnNext.setOnClickListener(v -> {
            if (validateInputs()) {
                String email = edtEmail.getText().toString().trim();
                String phone = edtPhone.getText().toString().trim();
                String cccd = edtCCCD.getText().toString().trim();

                viewModel.performFinalCheck(email, phone, cccd);
            }
        });
    }

    private void initViews() {
        btnNext = findViewById(R.id.btnNext);
        edtCCCD = findViewById(R.id.edtCCCD);
        edtPhone = findViewById(R.id.edtPhone);
        edtEmail = findViewById(R.id.edtEmail);
    }

    private void setupRealtimeCheck() {
        edtEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String email = edtEmail.getText().toString().trim();
                if (validateEmailFormat(email)) {
                    viewModel.checkEmailExistence(email);
                }
            }
        });

        edtCCCD.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String cccd = edtCCCD.getText().toString().trim();
                if (validateCccdFormat(cccd)) {
                    viewModel.checkCccdExistence(cccd);
                }
            }
        });

        edtPhone.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String phone = edtPhone.getText().toString().trim();
                if (validatePhoneFormat(phone)) {
                    viewModel.checkPhoneExistence(phone);
                }
            }
        });
    }

    private void setupObservers() {
        viewModel.emailError.observe(this, errorMsg -> {
            if (errorMsg != null) edtEmail.setError(errorMsg);
            else edtEmail.setError(null);
        });

        viewModel.cccdError.observe(this, errorMsg -> {
            if (errorMsg != null) edtCCCD.setError(errorMsg);
            else edtCCCD.setError(null);
        });

        viewModel.phoneError.observe(this, errorMsg -> {
            if (errorMsg != null) edtPhone.setError(errorMsg);
            else edtPhone.setError(null);
        });

        viewModel.finalCheckAction.observe(this, result -> {
            if (result == null) return;

            if (result.isSuccess) {
                currentPhoneNumber = edtPhone.getText().toString().trim();

                btnNext.setEnabled(false);
                btnNext.setText("Đang gửi OTP...");

                phoneAuthManager.sendOtp(currentPhoneNumber);

                btnNext.setEnabled(true);
                btnNext.setText("Tiếp tục");
            } else {
                if ("EMAIL".equals(result.errorField)) {
                    edtEmail.setError(result.errorMsg);
                    edtEmail.requestFocus();
                } else if ("PHONE".equals(result.errorField)) {
                    edtPhone.setError(result.errorMsg);
                    edtPhone.requestFocus();
                } else if ("CCCD".equals(result.errorField)) {
                    edtCCCD.setError(result.errorMsg);
                    edtCCCD.requestFocus();
                }
            }
        });
    }

    private boolean validateEmailFormat(String email) {
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Email sai định dạng");
            return false;
        }
        return true;
    }

    private boolean validateCccdFormat(String cccd) {
        if (cccd.isEmpty() || !cccd.matches("^\\d{12}$")) {
            edtCCCD.setError("CCCD phải đủ 12 số");
            return false;
        }
        return true;
    }

    private boolean validatePhoneFormat(String phone) {
        if (phone.isEmpty() || !phone.matches("^0\\d{9}$")) {
            edtPhone.setError("Số điện thoại không hợp lệ (10 số)");
            return false;
        }
        return true;
    }

    private boolean validateInputs() {
        boolean e = validateEmailFormat(edtEmail.getText().toString().trim());
        boolean c = validateCccdFormat(edtCCCD.getText().toString().trim());
        boolean p = validatePhoneFormat(edtPhone.getText().toString().trim());

        return e && c && p;
    }

    private void showOtpDialog() {
        OtpBottomSheetDialog otpDialog = OtpBottomSheetDialog.newInstance(null);

        otpDialog.setOtpVerificationListener(new OtpBottomSheetDialog.OtpVerificationListener() {
            @Override
            public void onOtpVerified(String otp) {
                phoneAuthManager.verifyCode(otp);
            }

            @Override
            public void onResendOtp() {
                phoneAuthManager.resendOtp(currentPhoneNumber);
            }
        });
        otpDialog.show(getSupportFragmentManager(), "OtpBottomSheet");
    }
}