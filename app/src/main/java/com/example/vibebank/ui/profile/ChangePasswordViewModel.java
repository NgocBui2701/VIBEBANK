package com.example.vibebank.ui.profile;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.vibebank.utils.PasswordUtils;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ChangePasswordViewModel extends ViewModel {

    private final FirebaseFirestore db;

    // LiveData
    public MutableLiveData<String> currentHash = new MutableLiveData<>(); // Hash lấy từ DB về
    public MutableLiveData<String> updateResult = new MutableLiveData<>(); // null = thành công, String = lỗi
    public MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public ChangePasswordViewModel() {
        db = FirebaseFirestore.getInstance();
    }

    // 1. Lấy Hash mật khẩu hiện tại
    public void fetchCurrentPasswordHash(String userId) {
        if (userId == null) return;

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String hash = documentSnapshot.getString("password_hash");
                        currentHash.setValue(hash);
                    }
                })
                .addOnFailureListener(e -> {
                    // Xử lý lỗi ngầm hoặc log
                });
    }

    // 2. Cập nhật mật khẩu mới (Tự động Hash tại đây)
    public void updatePassword(String userId, String newPasswordRaw) {
        isLoading.setValue(true);

        // Hash mật khẩu mới trước khi đẩy lên DB
        String newHash = PasswordUtils.hashPassword(newPasswordRaw);

        Map<String, Object> updates = new HashMap<>();
        updates.put("password_hash", newHash);

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    isLoading.setValue(false);
                    updateResult.setValue(null); // Thành công
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    updateResult.setValue("Lỗi cập nhật: " + e.getMessage());
                });
    }
}