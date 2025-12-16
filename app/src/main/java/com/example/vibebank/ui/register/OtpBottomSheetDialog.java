package com.example.vibebank.ui.register;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.vibebank.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

public class OtpBottomSheetDialog extends BottomSheetDialogFragment {

    private EditText otpDigit1, otpDigit2, otpDigit3, otpDigit4, otpDigit5, otpDigit6;
    private MaterialButton btnContinue;
    private TextView tvResendOtp, tvOtpMessage;

    private String mVerificationId;
    private boolean isOtpExpired = false;
    private CountDownTimer countDownTimer;
    private static final long TIMEOUT_MS = 60000;
    private OtpVerificationListener listener;

    public interface OtpVerificationListener {
        void onOtpVerified(String otpCode);
        void onResendOtp();
    }

    public static OtpBottomSheetDialog newInstance(String verificationId) {
        OtpBottomSheetDialog fragment = new OtpBottomSheetDialog();
        Bundle args = new Bundle();
        args.putString("verificationId", verificationId);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), getTheme()) {
            @Override
            public boolean dispatchTouchEvent(@NonNull MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    View v = getCurrentFocus();
                    if (v instanceof EditText) {
                        Rect outRect = new Rect();
                        v.getGlobalVisibleRect(outRect);
                        if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                            v.clearFocus();
                            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                        }
                    }
                }
                return super.dispatchTouchEvent(event);
            }
        };
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_otp, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) mVerificationId = getArguments().getString("verificationId");

        initViews(view);
        setupOtpInputs();
        startCountDownTimer();

        btnContinue.setOnClickListener(v -> {
            if (isOtpExpired) {
                Toast.makeText(getContext(), "Mã OTP đã hết hạn. Vui lòng lấy mã mới!", Toast.LENGTH_SHORT).show();
                return;
            }
            String otp = getOtpCode();
            if (otp.length() == 6) {
                if (listener != null) listener.onOtpVerified(otp);
            } else {
                Toast.makeText(getContext(), "Vui lòng nhập đủ 6 số", Toast.LENGTH_SHORT).show();
            }
        });

        tvResendOtp.setOnClickListener(v -> {
            if (listener != null) {
                listener.onResendOtp();
                resetTimerState();
            }
        });

        otpDigit1.requestFocus();
        showKeyboard(otpDigit1);
    }

    private void initViews(View view) {
        otpDigit1 = view.findViewById(R.id.otpDigit1);
        otpDigit2 = view.findViewById(R.id.otpDigit2);
        otpDigit3 = view.findViewById(R.id.otpDigit3);
        otpDigit4 = view.findViewById(R.id.otpDigit4);
        otpDigit5 = view.findViewById(R.id.otpDigit5);
        otpDigit6 = view.findViewById(R.id.otpDigit6);
        btnContinue = view.findViewById(R.id.btnContinueOtp);
        tvResendOtp = view.findViewById(R.id.tvResendOtp);
        tvOtpMessage = view.findViewById(R.id.tvOtpMessage);
    }

    private void startCountDownTimer() {
        isOtpExpired = false;
        setConfirmButtonEnabled(true);
        tvResendOtp.setEnabled(false);
        tvResendOtp.setTextColor(Color.GRAY);

        if (countDownTimer != null) countDownTimer.cancel();

        countDownTimer = new CountDownTimer(TIMEOUT_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvResendOtp.setText("Gửi lại mã (" + millisUntilFinished / 1000 + "s)");
            }
            @Override
            public void onFinish() {
                isOtpExpired = true;
                tvResendOtp.setText("Gửi lại mã ngay");
                tvResendOtp.setEnabled(true);
                tvResendOtp.setTextColor(Color.BLACK);
                tvOtpMessage.setText("Mã xác thực đã hết hiệu lực!");
                tvOtpMessage.setTextColor(Color.RED);
                setConfirmButtonEnabled(false);
            }
        }.start();
    }

    private void resetTimerState() {
        tvOtpMessage.setText("Mã OTP đã được gửi về số điện thoại");
        tvOtpMessage.setTextColor(Color.parseColor("#666666"));
        clearOtpInputs();
        startCountDownTimer();
    }

    private void setupOtpInputs() {
        EditText[] otpDigits = {otpDigit1, otpDigit2, otpDigit3, otpDigit4, otpDigit5, otpDigit6};
        for (int i = 0; i < otpDigits.length; i++) {
            final int index = i;
            otpDigits[i].addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < otpDigits.length - 1) otpDigits[index + 1].requestFocus();
                }
                public void afterTextChanged(Editable s) {}
            });
            otpDigits[i].setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                    if (otpDigits[index].getText().length() == 0 && index > 0) {
                        otpDigits[index - 1].requestFocus();
                        otpDigits[index - 1].setText("");
                        return true;
                    }
                }
                return false;
            });
        }
    }

    private void clearOtpInputs() {
        otpDigit1.setText(""); otpDigit2.setText(""); otpDigit3.setText("");
        otpDigit4.setText(""); otpDigit5.setText(""); otpDigit6.setText("");
        otpDigit1.requestFocus();
    }

    private void setConfirmButtonEnabled(boolean isEnabled) {
        btnContinue.setEnabled(isEnabled);
        btnContinue.setBackgroundTintList(ColorStateList.valueOf(isEnabled ? Color.BLACK : Color.LTGRAY));
    }

    private String getOtpCode() {
        return otpDigit1.getText().toString().trim() + otpDigit2.getText().toString().trim() +
                otpDigit3.getText().toString().trim() + otpDigit4.getText().toString().trim() +
                otpDigit5.getText().toString().trim() + otpDigit6.getText().toString().trim();
    }

    public void setOtpVerificationListener(OtpVerificationListener listener) { this.listener = listener; }

    private void showKeyboard(View view) {
        if (view.requestFocus()) {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}