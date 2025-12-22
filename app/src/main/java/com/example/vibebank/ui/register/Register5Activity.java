package com.example.vibebank.ui.register;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.example.vibebank.R;
import com.example.vibebank.ui.base.BaseActivity;
import com.example.vibebank.ui.forgotpassword.ResetPasswordActivity;
import com.example.vibebank.ui.login.LoginActivity;
import com.example.vibebank.utils.CloudinaryHelper;
import com.example.vibebank.utils.PasswordValidationHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

public class Register5Activity extends BaseActivity {

    private ImageView btnBack;
    private MaterialButton btnCreateAccount;
    private TextInputEditText edtUsername, edtPassword, edtConfirmPassword;
    private TextView tvRuleLength, tvRuleUpperLower, tvRuleDigit, tvRuleSpecial;
    private ProgressBar progressBar;

    private RegisterViewModel viewModel;
    private PasswordValidationHelper passwordHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register5);

        CloudinaryHelper.initCloudinary(this);

        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        initViews();

        passwordHelper = new PasswordValidationHelper(
                this,
                edtPassword,
                edtConfirmPassword,
                tvRuleLength,
                tvRuleUpperLower,
                tvRuleDigit,
                tvRuleSpecial
        );

        setupData();
        setupListeners();
        setupObservers();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        progressBar = findViewById(R.id.progressBar);

        tvRuleLength = findViewById(R.id.tvRuleLength);
        tvRuleUpperLower = findViewById(R.id.tvRuleUpperLower);
        tvRuleDigit = findViewById(R.id.tvRuleDigit);
        tvRuleSpecial = findViewById(R.id.tvRuleSpecial);
    }

    private void setupData() {
        // Tự động điền Username là SĐT (Read-only)
        String phone = viewModel.getPhone();
        if (phone != null) {
            edtUsername.setText(phone);
            edtUsername.setEnabled(false); // Không cho sửa username
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnCreateAccount.setOnClickListener(v -> {
            if (passwordHelper.isValid()) {
                // Lấy pass từ Helper và lưu vào ViewModel
                viewModel.saveAccountInfo(passwordHelper.getPassword());
                // Gọi API đăng ký
                viewModel.registerUser();
            }
        });
    }

    private void setupObservers() {
        viewModel.registrationResult.observe(this, isSuccess -> {
            if (isSuccess) {
                showSuccessDialog();
            } else {
                showErrorDialog("Đã xảy ra lỗi khi tạo tài khoản");
            }
        });
        
        viewModel.isLoading.observe(this, isLoading -> {
            if (isLoading) {
                // Đang tạo: Hiện xoay vòng, ẩn chữ (hoặc giữ chữ), disable nút
                progressBar.setVisibility(View.VISIBLE);
                btnCreateAccount.setEnabled(false);
                btnCreateAccount.setText(""); // Mẹo: Xóa chữ để chỉ hiện vòng xoay cho đẹp
            } else {
                // Xong/Lỗi: Ẩn xoay vòng, hiện lại chữ, enable nút
                progressBar.setVisibility(View.GONE);
                btnCreateAccount.setEnabled(true);
                btnCreateAccount.setText("TẠO TÀI KHOẢN");
            }
        });
        
        // QUAN TRỌNG: Observe toastMessage để hiển thị lỗi chi tiết
        viewModel.toastMessage.observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showSuccessDialog() {
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Thành công")
                .setMessage("Tài khoản đã được tạo thành công! Vui lòng đăng nhập.")
                .setCancelable(false)
                .setIcon(R.drawable.ic_check_green)
                .setPositiveButton("Đăng nhập ngay", (d, w) -> {
                    Intent intent = new Intent(Register5Activity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finishAffinity();
                })
                .show();
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setTextColor(android.graphics.Color.parseColor("#4CAF50"));
    }
}