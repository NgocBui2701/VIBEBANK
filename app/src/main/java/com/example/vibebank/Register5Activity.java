package com.example.vibebank;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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

import java.util.regex.Pattern;

public class Register5Activity extends AppCompatActivity {
    private static final String TAG = "Register5Activity";

    private ImageView btnBack;
    private MaterialButton btnCreateAccount;
    private TextInputEditText edtUsername;
    private TextInputEditText edtPassword;
    private TextInputEditText edtConfirmPassword;
    
    private String phone;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register5);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.register5), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnBack = findViewById(R.id.btnBack);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        edtUsername = findViewById(R.id.edtUsername);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);

        // Get phone number from previous screen
        try {
            phone = getIntent().getStringExtra("phone");
            if (phone != null && !phone.isEmpty()) {
                edtUsername.setText(phone);
            }
            Log.d(TAG, "Phone received: " + phone);
        } catch (Exception e) {
            Log.e(TAG, "Error getting phone data", e);
            Toast.makeText(this, "Lỗi nhận dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnCreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateInput()) {
                    // TODO: Call API to create account
                    Toast.makeText(Register5Activity.this, "Tạo tài khoản thành công!", Toast.LENGTH_LONG).show();
                    // Navigate to login or main screen
                    finish();
                }
            }
        });
    }

    private boolean validateInput() {
        String password = edtPassword.getText() != null ? edtPassword.getText().toString() : "";
        String confirmPassword = edtConfirmPassword.getText() != null ? edtConfirmPassword.getText().toString() : "";

        // Check if password is empty
        if (password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập mật khẩu", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check password length
        if (password.length() < 8) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 8 ký tự", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check for uppercase
        if (!Pattern.compile("[A-Z]").matcher(password).find()) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 1 chữ hoa", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check for lowercase
        if (!Pattern.compile("[a-z]").matcher(password).find()) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 1 chữ thường", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check for digit
        if (!Pattern.compile("[0-9]").matcher(password).find()) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 1 chữ số", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check for special character
        if (!Pattern.compile("[!@#$]").matcher(password).find()) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 1 ký tự đặc biệt (!@#$)", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check if passwords match
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }
}
