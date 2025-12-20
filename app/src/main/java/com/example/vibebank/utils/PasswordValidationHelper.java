package com.example.vibebank.utils;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

import com.example.vibebank.R;
import com.google.android.material.textfield.TextInputEditText;

import java.util.regex.Pattern;

public class PasswordValidationHelper {
    private final TextInputEditText edtPassword;
    private final TextInputEditText edtConfirmPassword;

    // Các TextView hiển thị quy tắc
    private final TextView tvRuleLength;
    private final TextView tvRuleUpperLower;
    private final TextView tvRuleDigit;
    private final TextView tvRuleSpecial;

    // Regex & Màu sắc
    private static final String SPECIAL_CHARACTERS_REGEX = "[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?~]";
    private final int COLOR_DEFAULT = Color.parseColor("#666666"); // Màu xám
    private final int COLOR_SUCCESS = Color.parseColor("#4CAF50"); // Màu xanh lá

    public PasswordValidationHelper(Context context,
                                    TextInputEditText edtPassword,
                                    TextInputEditText edtConfirmPassword,
                                    TextView tvRuleLength,
                                    TextView tvRuleUpperLower,
                                    TextView tvRuleDigit,
                                    TextView tvRuleSpecial) {
        this.edtPassword = edtPassword;
        this.edtConfirmPassword = edtConfirmPassword;
        this.tvRuleLength = tvRuleLength;
        this.tvRuleUpperLower = tvRuleUpperLower;
        this.tvRuleDigit = tvRuleDigit;
        this.tvRuleSpecial = tvRuleSpecial;

        setupListeners();
    }

    private void setupListeners() {
        // Lắng nghe thay đổi text để check Real-time (Thời gian thực)
        edtPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateRealtime(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Tự động xóa lỗi ở ô Confirm khi người dùng bắt đầu nhập lại
        edtConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (edtConfirmPassword.getError() != null) {
                    edtConfirmPassword.setError(null);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void validateRealtime(String password) {
        // 1. Độ dài >= 8
        updateRuleStatus(tvRuleLength, password.length() >= 8);

        // 2. Hoa + Thường
        boolean hasUpper = Pattern.compile("[A-Z]").matcher(password).find();
        boolean hasLower = Pattern.compile("[a-z]").matcher(password).find();
        updateRuleStatus(tvRuleUpperLower, hasUpper && hasLower);

        // 3. Số
        boolean hasDigit = Pattern.compile("[0-9]").matcher(password).find();
        updateRuleStatus(tvRuleDigit, hasDigit);

        // 4. Ký tự đặc biệt
        boolean hasSpecial = Pattern.compile(SPECIAL_CHARACTERS_REGEX).matcher(password).find();
        updateRuleStatus(tvRuleSpecial, hasSpecial);
    }

    private void updateRuleStatus(TextView tv, boolean isValid) {
        if (isValid) {
            tv.setTextColor(COLOR_SUCCESS);
            // Icon tích xanh bên trái (Yêu cầu phải có file ic_check_green trong drawable)
            tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check_green, 0, 0, 0);
        } else {
            tv.setTextColor(COLOR_DEFAULT);
            // Xóa icon
            tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
    }

    // --- HÀM PUBLIC ĐỂ ACTIVITY GỌI KHI BẤM NÚT ---

    public boolean isValid() {
        String password = edtPassword.getText().toString();
        String confirm = edtConfirmPassword.getText().toString();

        boolean hasUpper = Pattern.compile("[A-Z]").matcher(password).find();
        boolean hasLower = Pattern.compile("[a-z]").matcher(password).find();
        boolean hasDigit = Pattern.compile("[0-9]").matcher(password).find();
        boolean hasSpecial = Pattern.compile(SPECIAL_CHARACTERS_REGEX).matcher(password).find();

        // 1. Check lại Password
        if (password.length() < 8 || !hasUpper || !hasLower || !hasDigit || !hasSpecial) {
            edtPassword.setError("Mật khẩu chưa đủ điều kiện");
            edtPassword.requestFocus();
            return false;
        }

        // 2. Check khớp Confirm Password
        if (!password.equals(confirm)) {
            edtConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            edtConfirmPassword.requestFocus();
            return false;
        }

        return true;
    }

    public String getPassword() {
        return edtPassword.getText().toString().trim();
    }
}