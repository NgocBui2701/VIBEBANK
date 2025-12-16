package com.example.vibebank.ui.register;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.vibebank.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class Register4Activity extends AppCompatActivity {
    private static final String TAG = "Register4Activity";

    private ImageView btnBack;
    private MaterialButton btnNext;
    private TextInputEditText edtFullName;
    private TextInputEditText edtBirthDate;
    private AutoCompleteTextView edtGender;
    private TextInputEditText edtCCCD;
    private TextInputEditText edtAddress;
    private TextInputEditText edtIssueDate;
    private TextInputEditText edtPhone;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register4);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.register4), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnBack = findViewById(R.id.btnBack);
        btnNext = findViewById(R.id.btnNext);
        edtFullName = findViewById(R.id.edtFullName);
        edtBirthDate = findViewById(R.id.edtBirthDate);
        edtGender = findViewById(R.id.edtGender);
        edtCCCD = findViewById(R.id.edtCCCD);
        edtAddress = findViewById(R.id.edtAddress);
        edtIssueDate = findViewById(R.id.edtIssueDate);
        edtPhone = findViewById(R.id.edtPhone);

        // Load data from previous screens
        loadDataFromIntent();

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(Register4Activity.this, Register5Activity.class);
                    // Pass phone number to Register5
                    String phone = getIntent().getStringExtra("phone");
                    intent.putExtra("phone", phone != null ? phone : "");
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(Register4Activity.this, "Có lỗi xảy ra: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });
    }

    private void loadDataFromIntent() {
        try {
            Log.d(TAG, "Starting to load data from intent");
            
            // Get data from previous screens with default empty string
            String cccd = getIntent().getStringExtra("cccd");
            String phone = getIntent().getStringExtra("phone");
            String email = getIntent().getStringExtra("email");
            String fullName = getIntent().getStringExtra("fullName");
            String birthDate = getIntent().getStringExtra("birthDate");
            String gender = getIntent().getStringExtra("gender");
            String address = getIntent().getStringExtra("address");
            String issueDate = getIntent().getStringExtra("issueDate");
            
            Log.d(TAG, "Data received - Name: " + fullName + ", CCCD: " + cccd + ", Phone: " + phone);

            // Display data safely
            Log.d(TAG, "Setting text to fields");
            if (edtFullName != null) edtFullName.setText(fullName != null && !fullName.isEmpty() ? fullName : "");
            if (edtBirthDate != null) edtBirthDate.setText(birthDate != null && !birthDate.isEmpty() ? birthDate : "");
            if (edtGender != null) edtGender.setText(gender != null && !gender.isEmpty() ? gender : "");
            if (edtCCCD != null) edtCCCD.setText(cccd != null && !cccd.isEmpty() ? cccd : "");
            if (edtAddress != null) edtAddress.setText(address != null && !address.isEmpty() ? address : "");
            if (edtIssueDate != null) edtIssueDate.setText(issueDate != null && !issueDate.isEmpty() ? issueDate : "");
            if (edtPhone != null) edtPhone.setText(phone != null && !phone.isEmpty() ? phone : "");
            Log.d(TAG, "Data loaded successfully");
        } catch (Exception e) {
            Toast.makeText(this, "Có lỗi khi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}
