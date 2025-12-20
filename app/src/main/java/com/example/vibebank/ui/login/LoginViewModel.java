package com.example.vibebank.ui.login;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.vibebank.utils.PasswordUtils;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginViewModel extends ViewModel {

    public MutableLiveData<String> loginResult = new MutableLiveData<>();
    public MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    // Biến tạm để lưu thông tin user khi login thành công
    public String successUserId = "";
    public String successFullName = "";

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_TIME_MILLIS = 15 * 60 * 1000; // 15 phút

    public void login(String phone, String password) {
        if (phone.isEmpty() || password.isEmpty()) {
            loginResult.setValue("Vui lòng điền đầy đủ thông tin");
            return;
        }

        isLoading.setValue(true);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Tìm user trong Firestore theo số điện thoại
        // Lưu ý: Ở bước đăng ký ta lưu field là "phone_number"
        db.collection("users")
                .whereEqualTo("phone_number", phone)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        isLoading.setValue(false);
                        loginResult.setValue("Tên đăng nhập hoặc mật khẩu không đúng");
                    } else {
                        // 2. Lấy document user đầu tiên tìm thấy
                        DocumentSnapshot userDoc = querySnapshot.getDocuments().get(0);
                        String userId = userDoc.getId();

                        Long lockUntil = userDoc.getLong("lock_until"); // Timestamp
                        long currentTime = System.currentTimeMillis();

                        if (lockUntil != null && lockUntil > currentTime) {
                            // Tài khoản đang bị khóa
                            long minutesLeft = (lockUntil - currentTime) / 60000;
                            isLoading.setValue(false);
                            loginResult.setValue("Tài khoản đã bị khóa do nhập sai quá 5 lần. Vui lòng thử lại sau " + (minutesLeft + 1) + " phút.");
                            return;
                        }

                        // 3. Kiểm tra mật khẩu bằng BCrypt
                        String storedHash = userDoc.getString("password_hash");
                        Long failedAttemptsLong = userDoc.getLong("failed_attempts");
                        int failedAttempts = (failedAttemptsLong != null) ? failedAttemptsLong.intValue() : 0;

                        if (storedHash != null && PasswordUtils.checkPassword(password, storedHash)) {
                            // Mật khẩu đúng
                            resetFailedAttempts(db, userId);

                            successUserId = userId;
                            successFullName = userDoc.getString("full_name");
                            isLoading.setValue(false);
                            loginResult.setValue(null); // null báo hiệu thành công
                        } else {
                            // Mật khẩu sai
                            handleFailedLogin(db, userId, failedAttempts);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    loginResult.setValue("Lỗi kết nối: " + e.getMessage());
                    Log.e("Login", "Error: ", e);
                });
    }

    // Reset số lần sai khi login thành công
    private void resetFailedAttempts(FirebaseFirestore db, String userId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("failed_attempts", 0);
        updates.put("lock_until", 0);
        db.collection("users").document(userId).update(updates);
    }

    // Xử lý khi login thất bại
    private void handleFailedLogin(FirebaseFirestore db, String userId, int currentAttempts) {
        int newAttempts = currentAttempts + 1;
        Map<String, Object> updates = new HashMap<>();

        String errorMsg;

        if (newAttempts >= MAX_ATTEMPTS) {
            // Khóa tài khoản
            long lockUntil = System.currentTimeMillis() + LOCK_TIME_MILLIS;
            updates.put("failed_attempts", newAttempts);
            updates.put("lock_until", lockUntil);
            errorMsg = "Bạn đã nhập sai 5 lần. Tài khoản bị khóa 15 phút.";
        } else {
            // Chỉ tăng số lần sai
            updates.put("failed_attempts", newAttempts);
            int attemptsLeft = MAX_ATTEMPTS - newAttempts;
            errorMsg = "Mật khẩu không đúng. Bạn còn " + attemptsLeft + " lần thử.";
        }

        // Cập nhật DB
        db.collection("users").document(userId).update(updates)
                .addOnCompleteListener(task -> {
                    isLoading.setValue(false);
                    loginResult.setValue(errorMsg);
                });
    }
}