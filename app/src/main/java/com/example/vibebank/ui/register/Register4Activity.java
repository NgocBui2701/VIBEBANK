package com.example.vibebank.ui.register;

import android.content.Intent;
import android.os.Bundle;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.vibebank.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class Register4Activity extends AppCompatActivity {

    private ImageView btnBack;
    private MaterialButton btnNext;
    private TextInputEditText edtFullName, edtBirthDate, edtCCCD, edtAddress, edtIssueDate, edtPhone;
    private AutoCompleteTextView edtGender;

    private RegisterViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register4);

        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        initViews();
        loadDataFromViewModel();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnNext = findViewById(R.id.btnNext);

        edtFullName = findViewById(R.id.edtFullName);
        edtBirthDate = findViewById(R.id.edtBirthDate);
        edtGender = findViewById(R.id.edtGender);
        edtCCCD = findViewById(R.id.edtCCCD);
        edtAddress = findViewById(R.id.edtAddress);
        edtIssueDate = findViewById(R.id.edtIssueDate);
        edtPhone = findViewById(R.id.edtPhone);
    }

    private void loadDataFromViewModel() {
        // Lấy dữ liệu hiển thị lên (Chỉ để xem)
        edtFullName.setText(viewModel.getFullName());
        edtBirthDate.setText(viewModel.getBirthDate());
        edtGender.setText(viewModel.getGender());
        edtCCCD.setText(viewModel.getCccd());
        edtAddress.setText(viewModel.getAddress());
        edtIssueDate.setText(viewModel.getIssueDate());
        edtPhone.setText(viewModel.getPhone());
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnNext.setOnClickListener(v -> {
            Intent intent = new Intent(Register4Activity.this, Register5Activity.class);
            startActivity(intent);
        });
    }
}