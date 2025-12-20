package com.example.vibebank.ui.forgotpassword;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.vibebank.utils.PasswordUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ResetPasswordViewModel extends ViewModel {

    public MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    public MutableLiveData<String> updateResult = new MutableLiveData<>(); // null = Success, String = Error

    public void updatePassword(String uid, String newRawPassword) {
        isLoading.setValue(true);

        // 1. Hash mật khẩu mới (QUAN TRỌNG)
        String hashedPassword = PasswordUtils.hashPassword(newRawPassword);

        // 2. Cập nhật vào Firestore
        FirebaseFirestore.getInstance().collection("users")
                .document(uid)
                .update("password_hash", hashedPassword)
                .addOnSuccessListener(aVoid -> {
                    isLoading.setValue(false);
                    // Sau khi đổi pass trong DB, nên đăng xuất Firebase Auth để an toàn
                    FirebaseAuth.getInstance().signOut();
                    updateResult.setValue(null); // Thành công
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    updateResult.setValue("Lỗi cập nhật: " + e.getMessage());
                });
    }
}