package com.example.vibebank.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.vibebank.ForgotPasswordActivity;
import com.example.vibebank.HomeActivity;
import com.example.vibebank.R;
import com.example.vibebank.ui.register.RegisterActivity;
import com.google.android.material.button.MaterialButton;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    private EditText etUsername;
    private EditText etPassword;
    private TextView txtCreateAccount;
    private TextView txtForgotPassword;
    private MaterialButton btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.edit_text_username);
        etPassword = findViewById(R.id.edit_text_password);
        txtCreateAccount = findViewById(R.id.text_create_account);
        txtForgotPassword = findViewById(R.id.text_forgot_password);
        btnLogin = findViewById(R.id.button_login);

        txtCreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        txtForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsername.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                    return;
                }

                // TODO: Validate with backend API
                // For now, just navigate to HomeActivity
                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                intent.putExtra("userName", username);
                startActivity(intent);
                finish();
            }
        });

        }
    }
