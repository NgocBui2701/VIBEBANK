package com.example.vibebank;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;

public class Register2Activity extends AppCompatActivity {
    private static final String TAG = "Register2Activity";

    private ImageView btnBack;
    private MaterialButton btnNext;
    private TextInputEditText edtFullName;
    private TextInputEditText edtBirthDate;
    private TextInputEditText edtIssueDate;
    private TextInputEditText edtAddress;
    private AutoCompleteTextView edtGender;
    
    private String cccd, phone, email;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register2);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.register2), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnBack = findViewById(R.id.btnBack);
        btnNext = findViewById(R.id.btnNext);
        edtFullName = findViewById(R.id.edtFullName);
        edtBirthDate = findViewById(R.id.edtBirthDate);
        edtIssueDate = findViewById(R.id.edtIssueDate);
        edtAddress = findViewById(R.id.edtAddress);
        edtGender = findViewById(R.id.edtGender);

        // Get data from Register1
        cccd = getIntent().getStringExtra("cccd");
        phone = getIntent().getStringExtra("phone");
        email = getIntent().getStringExtra("email");

        // Setup gender dropdown
        String[] genders = new String[]{"Nam", "Nữ", "Khác"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genders);
        edtGender.setAdapter(adapter);

        // Setup date pickers
        edtBirthDate.setOnClickListener(v -> showDatePicker(edtBirthDate));
        edtIssueDate.setOnClickListener(v -> showDatePicker(edtIssueDate));

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
                    Intent intent = new Intent(Register2Activity.this, Register3Activity.class);
                    // Pass all data forward with null safety
                    intent.putExtra("cccd", cccd != null ? cccd : "");
                    intent.putExtra("phone", phone != null ? phone : "");
                    intent.putExtra("email", email != null ? email : "");
                    intent.putExtra("fullName", edtFullName.getText() != null ? edtFullName.getText().toString() : "");
                    intent.putExtra("birthDate", edtBirthDate.getText() != null ? edtBirthDate.getText().toString() : "");
                    intent.putExtra("gender", edtGender.getText() != null ? edtGender.getText().toString() : "");
                    intent.putExtra("address", edtAddress.getText() != null ? edtAddress.getText().toString() : "");
                    intent.putExtra("issueDate", edtIssueDate.getText() != null ? edtIssueDate.getText().toString() : "");
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(Register2Activity.this, "Có lỗi xảy ra: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });
    }

    private void showDatePicker(TextInputEditText editText) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = String.format("%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear);
                    editText.setText(date);
                },
                year, month, day
        );
        datePickerDialog.show();
    }
}
