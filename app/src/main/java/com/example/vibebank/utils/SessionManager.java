package com.example.vibebank.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.example.vibebank.ui.login.LoginActivity;

public class SessionManager {
    private static final String PREF_NAME = "VibeBankSession";
    private static final String KEY_SAVED_PHONE = "saved_phone";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_AVATAR_URL = "avatar_url";
    private static final String KEY_HAS_LOGIN_BEFORE = "has_login_before";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_BIO_TOKEN = "BIO_ENCRYPTED_TOKEN";
    private static final String KEY_BIO_IV = "BIO_IV";

    private static boolean isCurrentSessionActive = false;
    private static String currentUserId = null;

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;
    Context context;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    // Lưu session khi đăng nhập thành công
    public void startSession(String userId, String phone, String authToken) {
        // Lưu vào RAM (Phiên hiện tại)
        isCurrentSessionActive = true;
        currentUserId = userId;

        // Lưu vào ổ cứng (Để lần sau mở app lên còn nhớ SĐT)
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_SAVED_PHONE, phone);
        editor.putString(KEY_AUTH_TOKEN, authToken);
        editor.putBoolean(KEY_HAS_LOGIN_BEFORE, true); // Đánh dấu đã từng đăng nhập
        editor.apply();
    }

    // Kiểm tra xem user đã đăng nhập chưa
    public boolean isLoggedIn() {
        return isCurrentSessionActive;
    }

    // Hàm lưu tên (Gọi khi Login Password thành công hoặc Load Profile thành công)
    public void saveUserFullName(String name) {
        editor.putString(KEY_FULL_NAME, name);
        editor.apply();
    }

    // Hàm lấy tên (Gọi khi mở màn hình Home)
    public String getUserFullName() {
        // Trả về tên đã lưu, nếu chưa có thì trả về "Khách hàng"
        return pref.getString(KEY_FULL_NAME, "Quý khách");
    }
    // Hàm lấy Avatar URL
    public void saveAvatarUrl(String url) {
        editor.putString(KEY_AVATAR_URL, url);
        editor.commit();
    }
    public String getAvatarUrl() {
        return pref.getString(KEY_AVATAR_URL, "");
    }

    // Lấy thông tin User
    public String getSavedPhone() {
        return pref.getString(KEY_SAVED_PHONE, "");
    }
    public String getCurrentUserId() {
        // Nếu RAM đang có thì trả về luôn (nhanh)
        if (currentUserId != null && !currentUserId.isEmpty()) {
            return currentUserId;
        }

        // Nếu RAM bị null (do mới mở lại app), thì lôi từ Ổ CỨNG ra
        currentUserId = pref.getString(KEY_USER_ID, "");
        return currentUserId;
    }
    public String getToken() {
        return pref.getString(KEY_AUTH_TOKEN, null);
    }
    public String getEncryptedBiometricToken() {
        return pref.getString(KEY_BIO_TOKEN, null);
    }
    public String getBiometricIV() {
        return pref.getString(KEY_BIO_IV, null);
    }

    // Lưu thông tin sinh trắc học
    public void saveBiometricCredentials(String encryptedToken, String iv) {
        editor.putString(KEY_BIO_TOKEN, encryptedToken);
        editor.putString(KEY_BIO_IV, iv);
        editor.putBoolean("IS_BIO_ENABLED", true);
        editor.apply();
    }

    public void clearBiometricCredentials() {
        editor.remove("BIO_ENCRYPTED_TOKEN");
        editor.remove("BIO_IV");
        editor.putBoolean("IS_BIO_ENABLED", false);
        editor.apply();
    }

    public boolean isBiometricEnabled() {
        return pref.getBoolean("IS_BIO_ENABLED", false);
    }

    // Hàm lấy số điện thoại đã lưu (gọi khi mở màn hình Login)
    public String getLastLoginPhone() {
        return pref.getString(KEY_SAVED_PHONE, "");
    }

    // Đăng xuất
    public void logout() {
        isCurrentSessionActive = false;
        currentUserId = null;

        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}