package com.example.vibebank.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.example.vibebank.ui.login.LoginActivity;

public class SessionManager {
    private static final String PREF_NAME = "VibeBankSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_FULL_NAME = "fullName";
    private static final String KEY_USER_ID = "userId";

    // Key lưu thời gian tương tác cuối cùng
    private static final String KEY_LAST_ACTIVE_TIME = "lastActiveTime";

    // Cấu hình: Thời gian tự động đăng xuất (ví dụ 5 phút = 300000 ms)
    private static final long SESSION_TIMEOUT_IN_MILLIS = 5 * 60 * 1000;

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;
    private final Context context;

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    // Lưu thông tin khi đăng nhập thành công
    public void createLoginSession(String userId, String phone, String fullName) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_PHONE, phone);
        editor.putString(KEY_FULL_NAME, fullName);
        editor.putLong(KEY_LAST_ACTIVE_TIME, System.currentTimeMillis());
        editor.apply();
    }

    // Kiểm tra xem user đã đăng nhập chưa
    public boolean isLoggedIn() {
        boolean isLogin = pref.getBoolean(KEY_IS_LOGGED_IN, false);

        if (isLogin) {
            long lastTime = pref.getLong(KEY_LAST_ACTIVE_TIME, 0);
            long currentTime = System.currentTimeMillis();

            // Nếu thời gian hiện tại - thời gian cuối > Timeout -> Hết hạn
            if ((currentTime - lastTime) > SESSION_TIMEOUT_IN_MILLIS) {
                // Hết hạn thì đăng xuất luôn
                logoutUser();
                return false;
            } else {
                // Vẫn còn hạn -> Cập nhật lại thời gian tương tác mới nhất (Gia hạn session)
                editor.putLong(KEY_LAST_ACTIVE_TIME, currentTime);
                editor.apply();
                return true;
            }
        }
        return false;
    }

    // Lấy thông tin User
    public String getFullName() {
        return pref.getString(KEY_FULL_NAME, "User");
    }

    public String getPhone() {
        return pref.getString(KEY_PHONE, "");
    }

    public String getUserId() {
        return pref.getString(KEY_USER_ID, "");
    }

    // Đăng xuất
    public void logoutUser() {
        editor.clear();
        editor.commit();

        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}