package com.example.vibebank.ui.forgotpassword;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.vibebank.R;
import com.example.vibebank.ui.base.BaseActivity;
import com.example.vibebank.ui.login.LoginActivity;
import com.example.vibebank.utils.PasswordValidationHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

public class ResetPasswordActivity extends BaseActivity {
    private static final String TAG = "ResetPasswordActivity";

    private ImageView btnBack;
    private MaterialButton btnConfirm;
    private TextInputEditText edtUsername, edtPassword, edtConfirmPassword;
    private TextView tvRuleLength, tvRuleUpperLower, tvRuleDigit, tvRuleSpecial;

    private ResetPasswordViewModel viewModel;
    private PasswordValidationHelper passwordHelper;
    private String uid, phone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reset_password);

        // Setup Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.resetPassword), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Nhận dữ liệu từ màn hình trước
        Intent intent = getIntent();
        uid = intent.getStringExtra("uid");
        phone = intent.getStringExtra("phone");

        viewModel = new ViewModelProvider(this).get(ResetPasswordViewModel.class);

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
        btnConfirm = findViewById(R.id.btnConfirm);
        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);

        tvRuleLength = findViewById(R.id.tvRuleLength);
        tvRuleUpperLower = findViewById(R.id.tvRuleUpperLower);
        tvRuleDigit = findViewById(R.id.tvRuleDigit);
        tvRuleSpecial = findViewById(R.id.tvRuleSpecial);
    }

    private void setupData() {
        if (phone != null) {
            edtUsername.setText(phone);
            edtUsername.setEnabled(false); // Không cho sửa username
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnConfirm.setOnClickListener(v -> {
            if (passwordHelper.isValid()) {
                String newPass = passwordHelper.getPassword();
                if (uid != null) {
                    viewModel.updatePassword(uid, newPass);
                } else {
                    Toast.makeText(this, "Đã xảy ra lỗi", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupObservers() {
        viewModel.updateResult.observe(this, error -> {
            if (error == null) {
                showSuccessDialog();
            } else {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSuccessDialog() {
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Thành công")
                .setMessage("Đổi mật khẩu thành công. Vui lòng đăng nhập lại.")
                .setCancelable(false)
                .setIcon(R.drawable.ic_check_green)
                .setPositiveButton("Đăng nhập ngay", (d, w) -> {
                    Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finishAffinity();
                })
                .show();
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setTextColor(android.graphics.Color.parseColor("#4CAF50"));
    }
}