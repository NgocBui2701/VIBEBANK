package com.example.vibebank;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ForgotPasswordActivity extends AppCompatActivity {
    private static final String TAG = "ForgotPasswordActivity";

    private ImageView btnBack;
    private MaterialButton btnNext;
    private TextInputEditText edtFullName;
    private TextInputEditText edtCCCD;
    private TextInputEditText edtPhone;

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

        btnBack = findViewById(R.id.btnBack);
        btnNext = findViewById(R.id.btnNext);
        edtFullName = findViewById(R.id.edtFullName);
        edtCCCD = findViewById(R.id.edtCCCD);
        edtPhone = findViewById(R.id.edtPhone);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fullName = edtFullName.getText().toString().trim();
                String cccd = edtCCCD.getText().toString().trim();
                String phone = edtPhone.getText().toString().trim();

                // Validate inputs
                if (fullName.isEmpty() || cccd.isEmpty() || phone.isEmpty()) {
                    Toast.makeText(ForgotPasswordActivity.this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Show OTP dialog
                showOtpDialog(phone, fullName, cccd);
            }
        });
    }

    private void showOtpDialog(String phone, String fullName, String cccd) {
        OtpBottomSheetDialog otpDialog = OtpBottomSheetDialog.newInstance("Email/Phone của bạn");
        otpDialog.setOtpVerificationListener(new OtpBottomSheetDialog.OtpVerificationListener() {
            @Override
            public void onOtpVerified(String otp) {
                // TODO: Verify OTP with backend
                Toast.makeText(ForgotPasswordActivity.this, "Xác thực OTP thành công!", Toast.LENGTH_SHORT).show();
                
                // Navigate to reset password screen
                Intent intent = new Intent(ForgotPasswordActivity.this, ResetPasswordActivity.class);
                intent.putExtra("phone", phone);
                intent.putExtra("fullName", fullName);
                intent.putExtra("cccd", cccd);
                startActivity(intent);
            }

            @Override
            public void onResendOtp() {
                // TODO: Resend OTP
                Toast.makeText(ForgotPasswordActivity.this, "Đã gửi lại mã OTP", Toast.LENGTH_SHORT).show();
            }
        });
        otpDialog.show(getSupportFragmentManager(), "OtpBottomSheet");
    }
}
