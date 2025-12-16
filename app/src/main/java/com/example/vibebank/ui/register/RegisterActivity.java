package com.example.vibebank.ui.register;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.vibebank.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class RegisterActivity extends AppCompatActivity {
    private MaterialButton btnNext;
    private TextInputEditText edtCCCD, edtPhone, edtEmail;
    private FirebaseAuth mAuth;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private RegisterViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        btnNext = findViewById(R.id.btnNext);
        edtCCCD = findViewById(R.id.edtCCCD);
        edtPhone = findViewById(R.id.edtPhone);
        edtEmail = findViewById(R.id.edtEmail);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnNext.setOnClickListener(v -> {
            if (validateInputs()) {
                String phoneRaw = edtPhone.getText().toString().trim();
                String phoneFormatted = phoneRaw.startsWith("0") ? "+84" + phoneRaw.substring(1) : phoneRaw;
                sendSmsOtp(phoneFormatted);
            }
        });
    }

    private boolean validateInputs() {
        String cccd = edtCCCD.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();

        if (cccd.isEmpty() || !cccd.matches("^\\d{12}$")) {
            edtCCCD.setError("CCCD phải đủ 12 số"); return false;
        }
        if (phone.isEmpty() || !phone.matches("^0\\d{9}$")) {
            edtPhone.setError("Số điện thoại không hợp lệ (10 số)"); return false;
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Email không đúng định dạng"); return false;
        }
        return true;
    }

    private void sendSmsOtp(String phoneNumber) {
        btnNext.setEnabled(false);
        btnNext.setText("Đang gửi OTP...");

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallbacks)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    // Tự động verify thành công (tùy chọn)
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    btnNext.setEnabled(true);
                    btnNext.setText("Tiếp tục");
                    showErrorDialog(e.getMessage());
                }

                @Override
                public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    btnNext.setEnabled(true);
                    btnNext.setText("Tiếp tục");
                    mVerificationId = verificationId;
                    mResendToken = token;
                    showOtpDialog();
                }
            };

    private void showOtpDialog() {
        OtpBottomSheetDialog otpDialog = OtpBottomSheetDialog.newInstance(mVerificationId);
        otpDialog.setOtpVerificationListener(new OtpBottomSheetDialog.OtpVerificationListener() {
            @Override
            public void onOtpVerified(String otp) {
                verifyCode(otp);
            }
            @Override
            public void onResendOtp() {
                String phone = edtPhone.getText().toString().trim();
                sendSmsOtp(phone.startsWith("0") ? "+84" + phone.substring(1) : phone);
            }
        });
        otpDialog.show(getSupportFragmentManager(), "OtpBottomSheet");
    }

    private void verifyCode(String code) {
        if (mVerificationId == null) return;
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                // LƯU DỮ LIỆU VÀO VIEWMODEL
                viewModel.saveStep1(
                        edtEmail.getText().toString(),
                        edtPhone.getText().toString(),
                        edtCCCD.getText().toString()
                );

                Intent intent = new Intent(RegisterActivity.this, Register2Activity.class);
                startActivity(intent);
            } else {
                Toast.makeText(RegisterActivity.this, "Mã OTP không đúng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showErrorDialog(String msg) {
        new AlertDialog.Builder(this).setTitle("Lỗi").setMessage(msg).setPositiveButton("OK", null).show();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }
}