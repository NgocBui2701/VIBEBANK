package com.example.vibebank.utils;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class PhoneAuthManager {
    private final FirebaseAuth mAuth;
    private final Activity activity;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private final PhoneAuthCallback callback;

    // Interface giao tiếp về Activity
    public interface PhoneAuthCallback {
        void onCodeSent();                       // Đã gửi SMS -> Hiện Dialog nhập
        void onVerificationSuccess();            // Đăng nhập thành công (Xong tất cả)
        void onVerificationFailed(String error); // Lỗi (Sai code, lỗi mạng...)
    }

    public PhoneAuthManager(Activity activity, PhoneAuthCallback callback) {
        this.activity = activity;
        this.callback = callback;
        this.mAuth = FirebaseAuth.getInstance();
    }

    // 1. Gửi OTP (Có xử lý format +84)
    public void sendOtp(String rawPhone) {
        String formattedPhone = rawPhone.startsWith("0")
                ? "+84" + rawPhone.substring(1)
                : rawPhone;

        startPhoneNumberVerification(formattedPhone);
    }

    // 2. Gửi lại OTP (Dùng token cũ)
    public void resendOtp(String rawPhone) {
        String formattedPhone = rawPhone.startsWith("0")
                ? "+84" + rawPhone.substring(1)
                : rawPhone;

        if (mResendToken != null) {
            PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                    .setPhoneNumber(formattedPhone)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(mCallbacks)
                    .setForceResendingToken(mResendToken) // Quan trọng: Token cũ
                    .build();
            PhoneAuthProvider.verifyPhoneNumber(options);
        } else {
            // Nếu mất token thì gửi mới hoàn toàn
            startPhoneNumberVerification(formattedPhone);
        }
    }

    // 3. Xác thực mã code người dùng nhập tay
    public void verifyCode(String code) {
        if (mVerificationId == null) {
            callback.onVerificationFailed("Lỗi hệ thống: Không tìm thấy ID xác thực.");
            return;
        }
        // Tạo credential từ code nhập tay
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    // Hàm nội bộ: Bắt đầu quy trình gửi
    private void startPhoneNumberVerification(String phoneNumber) {
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(mCallbacks)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    // Hàm nội bộ: Thực hiện đăng nhập Firebase
    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        // Đăng nhập thành công -> Báo về Activity
                        callback.onVerificationSuccess();
                    } else {
                        callback.onVerificationFailed("Xác thực thất bại");
                    }
                });
    }

    // Callbacks của Firebase
    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    // Trường hợp tự động xác thực (Instant Verify hoặc Auto-retrieval)
                    // Ta đăng nhập luôn mà không cần người dùng nhập
                    Log.d("PhoneAuth", "Auto verification completed");
                    signInWithPhoneAuthCredential(credential);
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    Log.e("PhoneAuth", "Verification failed", e);
                    callback.onVerificationFailed("Xác thực thất bại");
                }

                @Override
                public void onCodeSent(@NonNull String verificationId,
                                       @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    Log.d("PhoneAuth", "Code sent");
                    mVerificationId = verificationId;
                    mResendToken = token;
                    // Báo Activity để hiện Dialog nhập
                    callback.onCodeSent();
                }
            };
}