package com.example.vibebank.ui.register;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.vibebank.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Register2Activity extends AppCompatActivity {
    private MaterialButton btnNext;
    private TextInputEditText edtFullName, edtBirthDate, edtIssueDate, edtAddress;
    private AutoCompleteTextView edtGender;
    private RegisterViewModel viewModel;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register2);

        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);
        initViews();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnNext.setOnClickListener(v -> {
            if (validateInputs()) {
                // LƯU DỮ LIỆU BƯỚC 2 VÀO VIEWMODEL
                viewModel.saveStep2(
                        edtFullName.getText().toString(),
                        edtBirthDate.getText().toString(),
                        edtGender.getText().toString(),
                        edtAddress.getText().toString(),
                        edtIssueDate.getText().toString()
                );

                 Intent intent = new Intent(this, Register3Activity.class);
                 startActivity(intent);
            }
        });
    }

    private void initViews() {
        btnNext = findViewById(R.id.btnNext);
        edtFullName = findViewById(R.id.edtFullName);
        edtBirthDate = findViewById(R.id.edtBirthDate);
        edtIssueDate = findViewById(R.id.edtIssueDate);
        edtAddress = findViewById(R.id.edtAddress);
        edtGender = findViewById(R.id.edtGender);

        String[] genders = {"Nam", "Nữ"};
        edtGender.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genders));
        edtGender.setKeyListener(null);

        edtBirthDate.setKeyListener(null);
        edtIssueDate.setKeyListener(null);

        View.OnClickListener dateClick = v -> showDatePicker((TextInputEditText) v);
        edtBirthDate.setOnClickListener(dateClick);
        edtIssueDate.setOnClickListener(dateClick);

        View.OnFocusChangeListener focusListener = (v, hasFocus) -> { if(hasFocus) showDatePicker((TextInputEditText)v); };
        edtBirthDate.setOnFocusChangeListener(focusListener);
        edtIssueDate.setOnFocusChangeListener(focusListener);
    }

    private boolean validateInputs() {
        String name = edtFullName.getText().toString().trim();
        String dob = edtBirthDate.getText().toString();
        String gender = edtGender.getText().toString();
        String address = edtAddress.getText().toString().trim();
        String issue = edtIssueDate.getText().toString();

        if (name.length() < 4) { edtFullName.setError("Họ tên quá ngắn"); return false; }
        if (gender.isEmpty()) { edtGender.setError("Chọn giới tính"); return false; } else edtGender.setError(null);
        if (address.isEmpty()) { edtAddress.setError("Nhập địa chỉ"); return false; }

        if (dob.isEmpty()) { edtBirthDate.setError("Chọn ngày sinh"); return false; }
        if (!isAdult(dob)) { edtBirthDate.setError("Phải đủ 18 tuổi"); return false; } else edtBirthDate.setError(null);

        if (issue.isEmpty()) { edtIssueDate.setError("Chọn ngày cấp"); return false; }
        if (!isValidIssueDate(issue, dob)) { edtIssueDate.setError("Ngày cấp không hợp lệ"); return false; } else edtIssueDate.setError(null);

        return true;
    }

    private boolean isAdult(String dateStr) {
        try {
            Calendar dob = Calendar.getInstance(); dob.setTime(dateFormat.parse(dateStr));
            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
            if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) age--;
            return age >= 18;
        } catch (Exception e) { return false; }
    }

    private boolean isValidIssueDate(String issueStr, String dobStr) {
        try {
            Date issue = dateFormat.parse(issueStr);
            Date dob = dateFormat.parse(dobStr);
            Date today = new Date();
            return !issue.after(today) && issue.after(dob);
        } catch (Exception e) { return false; }
    }

    private void showDatePicker(TextInputEditText editText) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);

        Calendar cal = Calendar.getInstance();
        try { if(editText.getText().length() > 0) cal.setTime(dateFormat.parse(editText.getText().toString())); } catch (Exception e){}

        DatePickerDialog dpd = new DatePickerDialog(this, (v, y, m, d) -> {
            editText.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", d, m+1, y));
            editText.setError(null);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dpd.getDatePicker().setMaxDate(System.currentTimeMillis());
        dpd.show();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }
}