package com.example.vibebank.ui.register;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import androidx.lifecycle.ViewModelProvider;

import com.example.vibebank.R;
import com.example.vibebank.ui.base.BaseActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

public class Register2Activity extends BaseActivity {
    private MaterialButton btnNext;
    private TextInputEditText edtFullName, edtBirthDate, edtIssueDate, edtAddress;
    private AutoCompleteTextView edtGender;
    private RegisterViewModel viewModel;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L} .'-]+$");
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("^[\\p{L}0-9\\s,./-]+$");

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

        edtFullName.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        edtFullName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateName(true);
            }
        });

        String[] genders = {"Nam", "Nữ"};
        edtGender.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genders));
        edtGender.setKeyListener(null);
        edtGender.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateGender(true);
            }
        });

        edtBirthDate.setKeyListener(null);
        edtIssueDate.setKeyListener(null);

        View.OnClickListener dateClick = v -> showDatePicker((TextInputEditText) v);
        edtBirthDate.setOnClickListener(dateClick);
        edtIssueDate.setOnClickListener(dateClick);

        View.OnFocusChangeListener focusListener = (v, hasFocus) -> { if(hasFocus) showDatePicker((TextInputEditText)v); };
        edtBirthDate.setOnFocusChangeListener(focusListener);
        edtIssueDate.setOnFocusChangeListener(focusListener);

        edtBirthDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateBirthDate(true);
            }
        });

        edtIssueDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateIssueDate(true);
            }
        });

        edtAddress.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateAddress(true);
            }
        });
    }

    private boolean validateName(boolean showError) {
        String name = edtFullName.getText().toString().trim();

        if (name.isEmpty()) {
            if (showError) edtFullName.setError("Vui lòng nhập họ và tên");
            return false;
        }

        if (name.length() < 4) {
            edtFullName.setError("Tên quá ngắn");
            return false;
        }

        if (!name.contains(" ")) {
            edtFullName.setError("Vui lòng nhập đầy đủ Họ và Tên");
            return false;
        }

        if (!NAME_PATTERN.matcher(name).matches()) {
            edtFullName.setError("Tên không được chứa số hoặc ký tự đặc biệt");
            return false;
        }

        edtFullName.setError(null);
        return true;
    }

    private boolean validateGender(boolean showError) {
        String gender = edtGender.getText().toString();
        if (gender.isEmpty()) {
            if (showError) edtGender.setError("Chọn giới tính");
            return false;
        }
        edtGender.setError(null);
        return true;
    }

    private boolean validateAddress(boolean showError) {
        String address = edtAddress.getText().toString().trim();

        if (address.isEmpty()) {
            if (showError) edtAddress.setError("Nhập địa chỉ");
            return false;
        }

        if (address.length() < 10) {
            edtAddress.setError("Địa chỉ quá ngắn. Vui lòng ghi rõ Số nhà, Đường, Phường/Xã...");
            return false;
        }

        if (!ADDRESS_PATTERN.matcher(address).matches()) {
            edtAddress.setError("Địa chỉ chứa ký tự không hợp lệ");
            return false;
        }

        edtAddress.setError(null);
        return true;
    }

    private boolean validateBirthDate(boolean showError) {
        String dob = edtBirthDate.getText().toString();

        if (dob.isEmpty()) {
            if (showError) edtBirthDate.setError("Chọn ngày sinh");
            return false;
        }

        if (!isAdult(dob)) {
            edtBirthDate.setError("Phải đủ 18 tuổi");
            return false;
        }

        edtBirthDate.setError(null);
        return true;
    }

    private boolean validateIssueDate(boolean showError) {
        String issue = edtIssueDate.getText().toString();

        if (issue.isEmpty()) {
            if (showError) edtIssueDate.setError("Chọn ngày cấp");
            return false;
        }

        if (!isValidIssueDate(issue, edtBirthDate.getText().toString())) {
            edtIssueDate.setError("Ngày cấp không hợp lệ");
            return false;
        }

        edtIssueDate.setError(null);
        return true;
    }

    private boolean validateInputs() {
        boolean n = validateName(false);
        boolean g = validateGender(false);
        boolean a = validateAddress(false);
        boolean b = validateBirthDate(false);
        boolean i = validateIssueDate(false);

        return n && g && a && b && i;
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
}