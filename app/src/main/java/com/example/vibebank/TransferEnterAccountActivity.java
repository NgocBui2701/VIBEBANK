package com.example.vibebank;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class TransferEnterAccountActivity extends AppCompatActivity {

    private EditText edtAccountNumber;
    private Button btnContinue;
    private TextView tvError;
    private FirebaseFirestore db;
    private String selectedBank;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_enter_account);

        db = FirebaseFirestore.getInstance();
        selectedBank = getIntent().getStringExtra("bankName");

        // Ánh xạ view (cần đảm bảo ID trùng khớp XML)
        edtAccountNumber = findViewById(R.id.edtAccountNumber); // Cần thêm ID này vào EditText trong XML
        btnContinue = findViewById(R.id.btnContinue);
        tvError = findViewById(R.id.tvError); // Cần thêm TextView báo lỗi (mặc định visibility=GONE)
        View btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        // Enable button khi có text
        edtAccountNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnContinue.setEnabled(s.length() > 0);
                tvError.setVisibility(View.GONE); // Ẩn lỗi khi gõ lại
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnContinue.setOnClickListener(v -> checkAccountExistence());
    }

    private void checkAccountExistence() {
        String accountNumber = edtAccountNumber.getText().toString().trim();

        // Tìm user có accountNumber tương ứng trong collection "users"
        db.collection("accounts")
                .whereEqualTo("account_number", accountNumber)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot documents = task.getResult();
                        if (!documents.isEmpty()) {
                            // Tài khoản tồn tại -> Lấy thông tin và chuyển trang
                            DocumentSnapshot accountDoc = documents.getDocuments().get(0);
                            String userId = accountDoc.getId();

                            db.collection("users")
                                    .document(userId)
                                    .get()
                                    .addOnCompleteListener(userTask -> {
                                        if (userTask.isSuccessful()) {
                                            DocumentSnapshot userDoc = userTask.getResult();
                                            if (userDoc.exists()) {
                                                String name = userDoc.getString("full_name");

                                                // Chuyển trang và gửi dữ liệu
                                                Intent intent = new Intent(TransferEnterAccountActivity.this, TransferDetailsActivity.class);
                                                intent.putExtra("receiverAccountNumber", accountNumber);
                                                intent.putExtra("receiverName", name);
                                                intent.putExtra("receiverUserId", userId);
                                                intent.putExtra("bankName", selectedBank);
                                                startActivity(intent);
                                            } else {
                                                // Có ID trong accounts nhưng không tìm thấy document bên users
                                                tvError.setText("Dữ liệu người dùng bị lỗi");
                                                tvError.setVisibility(View.VISIBLE);
                                            }
                                        } else {
                                            tvError.setText("Lỗi khi lấy thông tin người dùng");
                                            tvError.setVisibility(View.VISIBLE);
                                        }
                                    });
                        } else {
                            // Không tìm thấy số tài khoản trong collection accounts
                            tvError.setText("Tài khoản người nhận không khả dụng");
                            tvError.setVisibility(View.VISIBLE);
                        }
                    } else {
                        tvError.setText("Lỗi kết nối server");
                        tvError.setVisibility(View.VISIBLE);
                    }
                });
    }
}