package com.example.vibebank.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.vibebank.ui.OtpBottomSheetDialog;
import com.example.vibebank.ui.base.BaseActivity;
import com.example.vibebank.ui.forgotpassword.ForgotPasswordActivity;
import com.example.vibebank.ui.home.HomeActivity;
import com.example.vibebank.R;
import com.example.vibebank.ui.register.RegisterActivity;
import com.example.vibebank.utils.BiometricHelper;
import com.example.vibebank.utils.PhoneAuthManager;
import com.example.vibebank.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;

import javax.crypto.Cipher;

public class LoginActivity extends BaseActivity implements
        PhoneAuthManager.PhoneAuthCallback,
        OtpBottomSheetDialog.OtpVerificationListener {
    private static final String TAG = "LoginActivity";

    private EditText etUsername;
    private EditText etPassword;
    private ImageView imgFingerprint;
    private TextView txtCreateAccount;
    private TextView txtForgotPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;

    private LoginViewModel viewModel;
    private SessionManager sessionManager;
    private BiometricHelper biometricHelper;
    private PhoneAuthManager phoneAuthManager;
    private OtpBottomSheetDialog otpDialog;
    private String currentDeviceId;

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
        biometricHelper = new BiometricHelper();
        currentDeviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        initViews();
        setupData();
        setupBiometric();
        setupListeners();
        setupObservers();
    }

    private void initViews() {
        etUsername = findViewById(R.id.edit_text_username);
        etPassword = findViewById(R.id.edit_text_password);
        txtCreateAccount = findViewById(R.id.text_create_account);
        txtForgotPassword = findViewById(R.id.text_forgot_password);
        btnLogin = findViewById(R.id.button_login);
        imgFingerprint = findViewById(R.id.imgFingerprint);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupData() {
        String savedPhone = sessionManager.getSavedPhone();
        if (!savedPhone.isEmpty()) {
            etUsername.setText(savedPhone);
            etUsername.setEnabled(true);
            etPassword.requestFocus();
        }
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

            // Kiểm tra mạng
            if (!isNetworkAvailable()) {
                showErrorDialog("Không có kết nối Internet. Vui lòng kiểm tra lại đường truyền.");
                return;
            }

            String phone = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            viewModel.login(phone, password, currentDeviceId);
        });

        imgFingerprint.setOnClickListener(v -> showBiometricPrompt());
    }

    private void setupBiometric() {
        String iv = sessionManager.getBiometricIV();
        String token = sessionManager.getEncryptedBiometricToken();

        if (iv != null && !iv.isEmpty() && token != null && !token.isEmpty()) {
            imgFingerprint.setVisibility(View.VISIBLE);
            imgFingerprint.setOnClickListener(v -> showBiometricPrompt());
        } else {
            imgFingerprint.setVisibility(View.GONE);
        }
    }

    private void showBiometricPrompt() {
        try {
            // 1. Lấy IV và Token mã hóa từ SessionManager
            String ivBase64 = sessionManager.getBiometricIV();
            String encryptedTokenBase64 = sessionManager.getEncryptedBiometricToken();

            if (ivBase64 == null || encryptedTokenBase64 == null) {
                Toast.makeText(this, "Dữ liệu sinh trắc học lỗi, vui lòng đăng nhập bằng mật khẩu", Toast.LENGTH_SHORT).show();
                return;
            }

            byte[] iv = Base64.decode(ivBase64, Base64.DEFAULT);

            // 2. Lấy Cipher ở chế độ GIẢI MÃ (Decrypt)
            Cipher cipher = biometricHelper.getCipherForDecryption(iv);

            // 3. Cấu hình Prompt
            Executor executor = ContextCompat.getMainExecutor(this);
            BiometricPrompt biometricPrompt = new BiometricPrompt(LoginActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    viewModel.isLoading.setValue(true);
                    try {
                        // 4. Khi vân tay đúng -> Lấy Cipher đã mở khóa
                        Cipher successCipher = result.getCryptoObject().getCipher();

                        // 5. Giải mã Token
                        byte[] encryptedData = Base64.decode(encryptedTokenBase64, Base64.DEFAULT);
                        byte[] decodedData = successCipher.doFinal(encryptedData);
                        String originalToken = new String(decodedData, StandardCharsets.UTF_8);

                        // 6. ĐĂNG NHẬP THÀNH CÔNG VỚI TOKEN THẬT
                        // Giả sử lấy được userId từ token hoặc từ session cũ
                        String savedUserId = sessionManager.getCurrentUserId();
                        String savedPhone = sessionManager.getSavedPhone();
                        String savedAvatarUrl = sessionManager.getAvatarUrl();

                        if (savedUserId == null || savedUserId.isEmpty()) {
                            Toast.makeText(LoginActivity.this, "Dữ liệu người dùng hỏng, vui lòng đăng nhập bằng mật khẩu", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Lưu lại token vào session hiện tại để gọi API
                        sessionManager.startSession(savedUserId, savedPhone, originalToken);

                        Toast.makeText(LoginActivity.this, "Đăng nhập vân tay thành công!", Toast.LENGTH_SHORT).show();
                        viewModel.isLoading.setValue(false);
                        navigateToHome();

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(LoginActivity.this, "Giải mã thất bại", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    Toast.makeText(LoginActivity.this, "Vân tay không đúng", Toast.LENGTH_SHORT).show();
                }
            });

            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Đăng nhập bằng sinh trắc học")
                    .setNegativeButtonText("Hủy")
                    .build();

            // 7. Quan trọng: Truyền CryptoObject vào authenticate
            biometricPrompt.authenticate(promptInfo, new BiometricPrompt.CryptoObject(cipher));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khởi tạo bảo mật", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupObservers() {
        // Lắng nghe kết quả
        viewModel.loginResult.observe(this, error -> {
            if (error == null) {
                sessionManager.saveUserFullName(viewModel.tempFullName);
                sessionManager.saveAvatarUrl(viewModel.avatarUrl);
                String authToken = viewModel.getAuthToken();
                sessionManager.startSession(viewModel.tempUserId, viewModel.tempPhone, authToken);
                navigateToHome();
            } else {
                showErrorDialog(error);
            }
        });

        viewModel.needOtpVerify.observe(this, needed -> {
            phoneAuthManager = new PhoneAuthManager(this, this);

            if (needed) {
                phoneAuthManager.sendOtp(viewModel.tempPhone);
            }
        });

        // Lắng nghe loading
        viewModel.isLoading.observe(this, isLoading -> {
            if (isLoading) {
                if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                btnLogin.setEnabled(false);
                btnLogin.setAlpha(0.7f);
            } else {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                btnLogin.setAlpha(1.0f);
            }
        });
    }

    @Override
    public void onCodeSent() {
        otpDialog = OtpBottomSheetDialog.newInstance("");
        otpDialog.setOtpVerificationListener(this); // Gắn listener để nhận code nhập vào
        otpDialog.show(getSupportFragmentManager(), "OtpDialog");
    }

    @Override
    public void onVerificationSuccess() {
        // OTP đúng -> Firebase xác thực xong
        Toast.makeText(this, "Xác thực OTP thành công!", Toast.LENGTH_SHORT).show();

        // Gọi ViewModel để cập nhật thiết bị này thành tin cậy
        viewModel.updateTrustedDevice(currentDeviceId);
    }

    @Override
    public void onVerificationFailed(String error) {
        // OTP sai hoặc lỗi mạng
        showErrorDialog(error);
    }

    @Override
    public void onOtpVerified(String otpCode) {
        // Khi người dùng bấm nút "Tiếp tục" trong Dialog
        // Truyền code này cho PhoneAuthManager kiểm tra
        phoneAuthManager.verifyCode(otpCode);
    }

    @Override
    public void onResendOtp() {
        // Khi người dùng bấm "Gửi lại" trong Dialog
        phoneAuthManager.resendOtp(viewModel.tempPhone);
    }

    private void navigateToHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}