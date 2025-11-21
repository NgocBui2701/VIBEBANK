package com.example.vibebank;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
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

public class Register3Activity extends AppCompatActivity {
    private static final String TAG = "Register3Activity";

    private ImageView btnBack;
    private MaterialButton btnCaptureFront;
    private MaterialButton btnCaptureBack;
    private MaterialButton btnNext;
    
    private String cccd, phone, email, fullName, birthDate, gender, address, issueDate;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register3);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.register3), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnBack = findViewById(R.id.btnBack);
        btnCaptureFront = findViewById(R.id.btnCaptureFront);
        btnCaptureBack = findViewById(R.id.btnCaptureBack);
        btnNext = findViewById(R.id.btnNext);

        // Get data from Register2
        try {
            cccd = getIntent().getStringExtra("cccd");
            phone = getIntent().getStringExtra("phone");
            email = getIntent().getStringExtra("email");
            fullName = getIntent().getStringExtra("fullName");
            birthDate = getIntent().getStringExtra("birthDate");
            gender = getIntent().getStringExtra("gender");
            address = getIntent().getStringExtra("address");
            issueDate = getIntent().getStringExtra("issueDate");
            
            Log.d(TAG, "Data received - CCCD: " + cccd + ", Phone: " + phone + ", Name: " + fullName);
        } catch (Exception e) {
            Log.e(TAG, "Error getting intent data", e);
            Toast.makeText(this, "Lỗi nhận dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnCaptureFront.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Implement camera capture for front side
                Toast.makeText(Register3Activity.this, "Chụp mặt trước", Toast.LENGTH_SHORT).show();
            }
        });

        btnCaptureBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Implement camera capture for back side
                Toast.makeText(Register3Activity.this, "Chụp mặt sau", Toast.LENGTH_SHORT).show();
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(Register3Activity.this, Register4Activity.class);
                    // Pass all data to Register4 with null safety
                    intent.putExtra("cccd", cccd != null ? cccd : "");
                    intent.putExtra("phone", phone != null ? phone : "");
                    intent.putExtra("email", email != null ? email : "");
                    intent.putExtra("fullName", fullName != null ? fullName : "");
                    intent.putExtra("birthDate", birthDate != null ? birthDate : "");
                    intent.putExtra("gender", gender != null ? gender : "");
                    intent.putExtra("address", address != null ? address : "");
                    intent.putExtra("issueDate", issueDate != null ? issueDate : "");
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(Register3Activity.this, "Có lỗi xảy ra: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });
    }
}
