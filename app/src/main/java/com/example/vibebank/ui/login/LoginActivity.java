package com.example.vibebank.ui.login;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.lifecycle.ViewModelProvider;

import com.example.vibebank.ui.base.BaseActivity;
import com.example.vibebank.ui.forgotpassword.ForgotPasswordActivity;
import com.example.vibebank.ui.home.HomeActivity;
import com.example.vibebank.R;
import com.example.vibebank.ui.register.RegisterActivity;
import com.example.vibebank.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

public class LoginActivity extends BaseActivity {
    private static final String TAG = "LoginActivity";

    private EditText etUsername;
    private EditText etPassword;
    private TextView txtCreateAccount;
    private TextView txtForgotPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;

    private LoginViewModel viewModel;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Check Session
        sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            navigateToHome();
            return;
        }

        setContentView(R.layout.activity_login);
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        initViews();
        setupListeners();
        setupObservers();
    }

    private void initViews() {
        etUsername = findViewById(R.id.edit_text_username);
        etPassword = findViewById(R.id.edit_text_password);
        txtCreateAccount = findViewById(R.id.text_create_account);
        txtForgotPassword = findViewById(R.id.text_forgot_password);
        btnLogin = findViewById(R.id.button_login);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        txtCreateAccount.setOnClickListener(v -> {
            if (!isNetworkAvailable()) {
                showErrorDialog("Vui lòng kiểm tra kết nối Internet!");
                return;
            }
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        txtForgotPassword.setOnClickListener(v -> {
            if (!isNetworkAvailable()) {
                showErrorDialog("Vui lòng kiểm tra kết nối Internet!");
                return;
            }
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        btnLogin.setOnClickListener(v -> {
            // Ẩn bàn phím trước
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            View currentFocus = getCurrentFocus();
            if (imm != null && currentFocus != null) {
                imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
            }

            // Kiểm tra mạng
            if (!isNetworkAvailable()) {
                showErrorDialog("Không có kết nối Internet. Vui lòng kiểm tra lại đường truyền.");
                return;
            }

            String phone = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            viewModel.login(phone, password);
        });
    }

    private void setupObservers() {
        // Lắng nghe kết quả
        viewModel.loginResult.observe(this, errorMsg -> {
            if (errorMsg == null) {
                sessionManager.createLoginSession(
                        viewModel.successUserId,
                        etUsername.getText().toString().trim(),
                        viewModel.successFullName
                );

                // Lưu riêng ID vào SharedPreferences
                android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                android.content.SharedPreferences.Editor editor = prefs.edit();
                editor.putString("current_user_id", viewModel.successUserId);
                editor.apply();

                navigateToHome();
            } else {
                showErrorDialog(errorMsg);
            }
        });

        // Lắng nghe loading
        viewModel.isLoading.observe(this, isLoading -> {
            if (isLoading) {
                if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                btnLogin.setEnabled(false);
                btnLogin.setText("Đang xử lý...");
                btnLogin.setAlpha(0.7f);
            } else {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                btnLogin.setText("Đăng nhập");
                btnLogin.setAlpha(1.0f);
            }
        });
    }

    private void navigateToHome() {
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }
}