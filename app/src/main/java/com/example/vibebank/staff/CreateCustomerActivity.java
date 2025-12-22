package com.example.vibebank.staff;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vibebank.R;
import com.example.vibebank.utils.PasswordUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CreateCustomerActivity extends AppCompatActivity {

    private TextInputEditText edtFullName, edtPhone, edtEmail, edtAddress, edtIdNumber;
    private MaterialButton btnCreate;
    private ImageView btnBack;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_customer);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        edtFullName = findViewById(R.id.edtFullName);
        edtPhone = findViewById(R.id.edtPhone);
        edtEmail = findViewById(R.id.edtEmail);
        edtAddress = findViewById(R.id.edtAddress);
        edtIdNumber = findViewById(R.id.edtIdNumber);
        btnCreate = findViewById(R.id.btnCreate);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnCreate.setOnClickListener(v -> createCustomer());
    }

    private void createCustomer() {
        String fullName = edtFullName.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String address = edtAddress.getText().toString().trim();
        String idNumber = edtIdNumber.getText().toString().trim();

        if (fullName.isEmpty() || phone.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate default password
        String defaultPassword = "Vibebank@" + phone.substring(phone.length() - 4);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        
        // Create Firebase Auth account
        auth.createUserWithEmailAndPassword(email, defaultPassword)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    saveCustomerData(uid, fullName, phone, email, address, idNumber, defaultPassword);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tạo tài khoản: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void saveCustomerData(String uid, String fullName, String phone, String email, 
                                  String address, String idNumber, String password) {
        WriteBatch batch = db.batch();

        // User document
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("full_name", fullName);
        userMap.put("phone_number", phone);
        userMap.put("email", email);
        userMap.put("address", address);
        userMap.put("created_at", new Date());
        userMap.put("avatar_url", "");
        userMap.put("role", "customer");
        userMap.put("password_hash", PasswordUtils.hashPassword(password));

        // Account document
        Map<String, Object> accountMap = new HashMap<>();
        accountMap.put("account_number", phone);
        accountMap.put("account_type", "checking");
        accountMap.put("balance", 0);
        accountMap.put("created_at", new Date());

        // KYC document
        Map<String, Object> kycMap = new HashMap<>();
        kycMap.put("id_number", idNumber);
        kycMap.put("status", "pending");
        kycMap.put("created_at", new Date());

        batch.set(db.collection("users").document(uid), userMap);
        batch.set(db.collection("accounts").document(uid), accountMap);
        batch.set(db.collection("kyc_documents").document(uid), kycMap);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Tạo tài khoản thành công!\nMật khẩu: " + password, 
                            Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}

