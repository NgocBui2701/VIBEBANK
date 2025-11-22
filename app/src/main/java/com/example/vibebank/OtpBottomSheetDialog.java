package com.example.vibebank;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

public class OtpBottomSheetDialog extends BottomSheetDialogFragment {

    private EditText otpDigit1, otpDigit2, otpDigit3, otpDigit4, otpDigit5, otpDigit6;
    private MaterialButton btnContinue;
    private TextView tvResendOtp;
    private String email;
    private OtpVerificationListener listener;

    public interface OtpVerificationListener {
        void onOtpVerified(String otp);
        void onResendOtp();
    }

    public static OtpBottomSheetDialog newInstance(String email) {
        OtpBottomSheetDialog fragment = new OtpBottomSheetDialog();
        Bundle args = new Bundle();
        args.putString("email", email);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            email = getArguments().getString("email");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_otp, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        otpDigit1 = view.findViewById(R.id.otpDigit1);
        otpDigit2 = view.findViewById(R.id.otpDigit2);
        otpDigit3 = view.findViewById(R.id.otpDigit3);
        otpDigit4 = view.findViewById(R.id.otpDigit4);
        otpDigit5 = view.findViewById(R.id.otpDigit5);
        otpDigit6 = view.findViewById(R.id.otpDigit6);
        btnContinue = view.findViewById(R.id.btnContinueOtp);
        tvResendOtp = view.findViewById(R.id.tvResendOtp);

        // Setup OTP auto-focus
        setupOtpInputs();

        // Continue button click
        btnContinue.setOnClickListener(v -> {
            String otp = getOtpCode();
            if (otp.length() == 6) {
                if (listener != null) {
                    listener.onOtpVerified(otp);
                }
                dismiss();
            } else {
                Toast.makeText(getContext(), "Vui lòng nhập đầy đủ mã OTP", Toast.LENGTH_SHORT).show();
            }
        });

        // Resend OTP click
        tvResendOtp.setOnClickListener(v -> {
            if (listener != null) {
                listener.onResendOtp();
            }
            Toast.makeText(getContext(), "Đã gửi lại mã OTP", Toast.LENGTH_SHORT).show();
        });

        // Request focus on first digit
        otpDigit1.requestFocus();
    }

    private void setupOtpInputs() {
        EditText[] otpDigits = {otpDigit1, otpDigit2, otpDigit3, otpDigit4, otpDigit5, otpDigit6};

        for (int i = 0; i < otpDigits.length; i++) {
            final int index = i;
            otpDigits[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < otpDigits.length - 1) {
                        // Move to next field
                        otpDigits[index + 1].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            // Handle backspace
            otpDigits[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == android.view.KeyEvent.KEYCODE_DEL && otpDigits[index].getText().length() == 0 && index > 0) {
                    otpDigits[index - 1].requestFocus();
                    return true;
                }
                return false;
            });
        }
    }

    private String getOtpCode() {
        return otpDigit1.getText().toString() +
                otpDigit2.getText().toString() +
                otpDigit3.getText().toString() +
                otpDigit4.getText().toString() +
                otpDigit5.getText().toString() +
                otpDigit6.getText().toString();
    }

    public void setOtpVerificationListener(OtpVerificationListener listener) {
        this.listener = listener;
    }
}
