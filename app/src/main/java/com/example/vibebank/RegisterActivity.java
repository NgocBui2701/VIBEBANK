package com.example.vibebank;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
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

public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "RegisterActivity";

    private ImageView btnBack;
    private MaterialButton btnNext;
    private TextInputEditText edtCCCD;
    private TextInputEditText edtPhone;
    private TextInputEditText edtEmail;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.register1), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnBack = findViewById(R.id.btnBack);
        btnNext = findViewById(R.id.btnNext);
        edtCCCD = findViewById(R.id.edtCCCD);
        edtPhone = findViewById(R.id.edtPhone);
        edtEmail = findViewById(R.id.edtEmail);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String cccd = edtCCCD.getText().toString().trim();
                String phone = edtPhone.getText().toString().trim();
                String email = edtEmail.getText().toString().trim();

                // Validate inputs
                if (cccd.isEmpty() || phone.isEmpty() || email.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(RegisterActivity.this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Show OTP dialog
                showOtpDialog(email, cccd, phone);
            }
        });
    }

    private void showOtpDialog(String email, String cccd, String phone) {
        OtpBottomSheetDialog otpDialog = OtpBottomSheetDialog.newInstance(email);
        otpDialog.setOtpVerificationListener(new OtpBottomSheetDialog.OtpVerificationListener() {
            @Override
            public void onOtpVerified(String otp) {
                // TODO: Verify OTP with backend
                Toast.makeText(RegisterActivity.this, "Xác thực OTP thành công!", Toast.LENGTH_SHORT).show();
                
                // Navigate to next screen
                Intent intent = new Intent(RegisterActivity.this, Register2Activity.class);
                intent.putExtra("cccd", cccd);
                intent.putExtra("phone", phone);
                intent.putExtra("email", email);
                startActivity(intent);
            }

            @Override
            public void onResendOtp() {
                // TODO: Resend OTP to email
                Toast.makeText(RegisterActivity.this, "Đã gửi lại mã OTP đến " + email, Toast.LENGTH_SHORT).show();
            }
        });
        otpDialog.show(getSupportFragmentManager(), "OtpBottomSheet");
    }
}
