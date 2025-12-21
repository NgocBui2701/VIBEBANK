package com.example.vibebank;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.example.vibebank.ui.OtpBottomSheetDialog;
import com.example.vibebank.utils.PhoneAuthManager;
import com.example.vibebank.utils.SessionManager;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;

import java.text.NumberFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class TransferDetailsActivity extends AppCompatActivity implements
        PhoneAuthManager.PhoneAuthCallback,
        OtpBottomSheetDialog.OtpVerificationListener {

    private TextView tvReceiverName, tvReceiverAccount, tvReceiverBank;
    private EditText edtAmount, edtMessage;
    private CheckBox cbSaveContact;
    private Button btnTransfer;
    private FirebaseFirestore db;
    private String currentUserId = null;
    private String senderName = "Người dùng ẩn danh";
    private String senderPhone = "";
    private String receiverUid, receiverAccountNumber, receiverName;
    private LinearLayout llSuggestions;
    private static final double MAX_SUGGESTION_LIMIT = 999999999;
    private TextView tvSuggest1, tvSuggest2, tvSuggest3;

    // Các biến cho OTP và Lockout
    private PhoneAuthManager phoneAuthManager;
    private OtpBottomSheetDialog otpDialog;
    private SessionManager sessionManager;
    private static final String PREFS_SECURITY = "SecurityPrefs";
    private static final String KEY_FAILED_ATTEMPTS = "failed_attempts";
    private static final String KEY_LOCKOUT_TIME = "lockout_timestamp";
    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final long LOCKOUT_DURATION = 30 * 60 * 1000; // 30 phút

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_details);

        db = FirebaseFirestore.getInstance();
        phoneAuthManager = new PhoneAuthManager(this, this);
        sessionManager = new SessionManager(this);

        // Lấy ID người dùng hiện tại
        // 1. Lấy từ Firebase Auth
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // 2. Nếu null, lấy từ SessionManager (đồng bộ với LoginActivity)
        if (currentUserId == null && sessionManager.isLoggedIn()) {
            currentUserId = sessionManager.getCurrentUserId();
        }

        // 3. Lấy từ SharedPreferences "UserPrefs"
        if (currentUserId == null) {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            currentUserId = prefs.getString("current_user_id", null);
        }

        // Kiểm tra kết quả cuối cùng
        if (currentUserId == null) {
            Toast.makeText(this, "Lỗi phiên đăng nhập. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
            // Tùy chọn: Chuyển về màn hình Login
            // Intent intent = new Intent(this, LoginActivity.class);
            // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            // startActivity(intent);
            finish();
            return;
        }

        if (currentUserId != null) {
            fetchSenderInfo();
        }

        // Nhận dữ liệu
        receiverAccountNumber = getIntent().getStringExtra("receiverAccountNumber");
        receiverName = getIntent().getStringExtra("receiverName");
        receiverUid = getIntent().getStringExtra("receiverUserId");
        String bankName = getIntent().getStringExtra("bankName");

        if (receiverUid == null || receiverAccountNumber == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin người nhận", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Ánh xạ
        tvReceiverBank = findViewById(R.id.txtRecipientBank);
        tvReceiverName = findViewById(R.id.txtRecipientName);
        tvReceiverAccount = findViewById(R.id.txtRecipientAccount);
        edtAmount = findViewById(R.id.edtAmount);
        edtMessage = findViewById(R.id.edtMessage);
        cbSaveContact = findViewById(R.id.cbSaveRecipient);
        btnTransfer = findViewById(R.id.btnTransfer);
        View btnBack = findViewById(R.id.btnBack);

        llSuggestions = findViewById(R.id.llSuggestions);
        tvSuggest1 = findViewById(R.id.tvSuggest1);
        tvSuggest2 = findViewById(R.id.tvSuggest2);
        tvSuggest3 = findViewById(R.id.tvSuggest3);

        // Setup UI
        tvReceiverBank.setText(bankName != null ? bankName : "VibeBank");
        tvReceiverAccount.setText(receiverAccountNumber);
        tvReceiverName.setText(receiverName);

        // Kiểm tra người nhận đã tồn tại trong danh sách đã lưu
        SharedPreferences prefs = getSharedPreferences("SavedRecipients", MODE_PRIVATE);
        if (prefs.contains(receiverAccountNumber)) {
            cbSaveContact.setChecked(true);
            cbSaveContact.setEnabled(false);
        }

        setupMoneyInput();

        btnBack.setOnClickListener(v -> finish());

        // Mở nút chuyển tiền khi nhập đủ
        btnTransfer.setEnabled(true);
        btnTransfer.setOnClickListener(v -> preCheckTransfer());
    }

    private void preCheckTransfer() {
        // Kiểm tra đầu vào cơ bản
        String amountStr = edtAmount.getText().toString().replace(".", "");
        if (amountStr.isEmpty()) {
            edtAmount.setError("Vui lòng nhập số tiền");
            return;
        }
        double amount = Double.parseDouble(amountStr);
        if (amount <= 0) {
            edtAmount.setError("Số tiền phải lớn hơn 0");
            return;
        }

        // KIỂM TRA KHÓA (LOCKOUT)
        if (isLockedOut()) {
            long remainingTime = getRemainingLockoutTime();
            long minutes = remainingTime / 60000;
            showErrorDialog("Tài khoản bị tạm khóa chức năng chuyển tiền do nhập sai OTP quá 3 lần. Vui lòng thử lại sau " + (minutes + 1) + " phút.");
            return;
        }

        // Nếu có SĐT thì gửi OTP
        if (senderPhone != null && !senderPhone.isEmpty()) {
            // Show loading
            Toast.makeText(this, "Đang gửi mã OTP...", Toast.LENGTH_SHORT).show();
            phoneAuthManager.sendOtp(senderPhone);
        } else {
            showErrorDialog("Không tìm thấy số điện thoại xác thực. Vui lòng cập nhật hồ sơ.");
        }
    }

    private boolean isLockedOut() {
        SharedPreferences prefs = getSharedPreferences(PREFS_SECURITY, MODE_PRIVATE);
        long lockoutTime = prefs.getLong(KEY_LOCKOUT_TIME, 0);
        return System.currentTimeMillis() < lockoutTime;
    }

    private long getRemainingLockoutTime() {
        SharedPreferences prefs = getSharedPreferences(PREFS_SECURITY, MODE_PRIVATE);
        long lockoutTime = prefs.getLong(KEY_LOCKOUT_TIME, 0);
        return Math.max(0, lockoutTime - System.currentTimeMillis());
    }

    private void handleFailedAttempt() {
        SharedPreferences prefs = getSharedPreferences(PREFS_SECURITY, MODE_PRIVATE);
        int attempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1;
        SharedPreferences.Editor editor = prefs.edit();

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            // Khóa 30 phút
            long unlockTime = System.currentTimeMillis() + LOCKOUT_DURATION;
            editor.putLong(KEY_LOCKOUT_TIME, unlockTime);
            editor.putInt(KEY_FAILED_ATTEMPTS, 0); // Reset số lần sai sau khi đã khóa
            editor.apply();

            // Đóng dialog OTP nếu đang mở
            if (otpDialog != null && otpDialog.isVisible()) {
                otpDialog.dismiss();
            }

            showErrorDialog("Bạn đã nhập sai quá 3 lần. Chức năng chuyển tiền bị khóa trong 30 phút.");
        } else {
            editor.putInt(KEY_FAILED_ATTEMPTS, attempts);
            editor.apply();

            // Thông báo số lần còn lại
            int remaining = MAX_FAILED_ATTEMPTS - attempts;
            new AlertDialog.Builder(this)
                    .setTitle("Mã OTP không đúng")
                    .setMessage("Bạn còn " + remaining + " lần thử.")
                    .setPositiveButton("Thử lại", null)
                    .show();
        }
    }

    private void resetFailedAttempts() {
        SharedPreferences prefs = getSharedPreferences(PREFS_SECURITY, MODE_PRIVATE);
        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, 0).remove(KEY_LOCKOUT_TIME).apply();
    }

    // --- IMPLEMENT PHONE AUTH CALLBACKS (Giao tiếp với PhoneAuthManager) ---

    @Override
    public void onCodeSent() {
        // OTP đã gửi thành công -> Hiện Dialog nhập
        otpDialog = OtpBottomSheetDialog.newInstance(""); // Truyền sđt nếu muốn hiển thị
        otpDialog.setOtpVerificationListener(this);
        otpDialog.show(getSupportFragmentManager(), "OtpTransferDialog");
    }

    @Override
    public void onVerificationSuccess() {
        // Firebase xác thực thành công OTP
        if (otpDialog != null && otpDialog.isVisible()) {
            otpDialog.dismiss();
        }

        Toast.makeText(this, "Xác thực thành công!", Toast.LENGTH_SHORT).show();
        resetFailedAttempts(); // Reset bộ đếm lỗi

        // TIẾN HÀNH CHUYỂN TIỀN THẬT
        performTransaction();
    }

    @Override
    public void onVerificationFailed(String error) {
        // Lỗi từ Firebase (Code sai, hết hạn...)
        handleFailedAttempt();
    }

    // --- IMPLEMENT OTP DIALOG LISTENERS (Giao tiếp với BottomSheet) ---

    @Override
    public void onOtpVerified(String otpCode) {
        // Người dùng bấm nút Xác nhận trên Dialog
        phoneAuthManager.verifyCode(otpCode);
    }

    @Override
    public void onResendOtp() {
        if (senderPhone != null) {
            phoneAuthManager.resendOtp(senderPhone);
            Toast.makeText(this, "Đã gửi lại mã OTP", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupMoneyInput() {
        edtAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                edtAmount.removeTextChangedListener(this);

                try {
                    String originalString = s.toString();
                    String cleanString = originalString.replace(".", "");

                    if (!cleanString.isEmpty()) {
                        double parsed = Double.parseDouble(cleanString);
                        String formatted = formatMoney(parsed); // Dùng hàm formatMoney đã viết ở bước trước

                        edtAmount.setText(formatted);
                        edtAmount.setSelection(formatted.length());

                        updateSuggestions(parsed);
                    } else {
                        // Nếu xóa hết thì ẩn gợi ý
                        llSuggestions.setVisibility(View.GONE);
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }

                edtAmount.addTextChangedListener(this);
            }
        });

        View.OnClickListener suggestionListener = v -> {
            TextView tv = (TextView) v;
            String value = tv.getText().toString();
            edtAmount.setText(value);
            edtAmount.setSelection(value.length());
        };

        tvSuggest1.setOnClickListener(suggestionListener);
        tvSuggest2.setOnClickListener(suggestionListener);
        tvSuggest3.setOnClickListener(suggestionListener);
    }

    private void updateSuggestions(double currentAmount) {
        if (currentAmount <= 0) {
            llSuggestions.setVisibility(View.GONE);
            return;
        }

        double s1 = currentAmount * 1000;
        double s2 = currentAmount * 10000;
        double s3 = currentAmount * 100000;

        if (s1 > MAX_SUGGESTION_LIMIT) {
            llSuggestions.setVisibility(View.GONE);
            return;
        }

        llSuggestions.setVisibility(View.VISIBLE);

        tvSuggest1.setVisibility(View.VISIBLE);
        tvSuggest2.setVisibility(View.VISIBLE);
        tvSuggest3.setVisibility(View.VISIBLE);

        tvSuggest1.setText(formatMoney(s1));

        // Kiểm tra s2
        if (s2 > MAX_SUGGESTION_LIMIT) {
            tvSuggest2.setVisibility(View.GONE);
            tvSuggest3.setVisibility(View.GONE);
        } else {
            tvSuggest2.setText(formatMoney(s2));

            if (s3 > MAX_SUGGESTION_LIMIT) {
                tvSuggest3.setVisibility(View.GONE);
            } else {
                tvSuggest3.setText(formatMoney(s3));
            }
        }
    }

    private String formatMoney(double amount) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return formatter.format(amount);
    }

    private void performTransaction() {

        if (currentUserId == null) {
            Toast.makeText(this, "Lỗi: Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
            return;
        }

        if (receiverUid == null || receiverUid.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID người nhận. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
            return;
        }

        String amountStr = edtAmount.getText().toString();
        String message = edtMessage.getText().toString();

        if (amountStr.isEmpty()) {
            edtAmount.setError("Vui lòng nhập số tiền");
            return;
        }

        String cleanAmountStr = amountStr.replace(".", "");

        double amount = Double.parseDouble(cleanAmountStr);

        String formattedAmount = formatMoney(amount);

        // Kiểm tra số tiền tối thiểu
        if (amount <= 0) {
            edtAmount.setError("Số tiền phải lớn hơn 0");
            return;
        }

        // 1. Lưu người nhận nếu chọn
        if (cbSaveContact != null && cbSaveContact.isChecked()) {
            SharedPreferences prefs = getSharedPreferences("SavedRecipients", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            String valueToSave = receiverName + ";" + receiverUid;

            editor.putString(receiverAccountNumber, valueToSave);
            editor.apply();
        }

        final DocumentReference senderRef = db.collection("accounts").document(currentUserId);
        final DocumentReference receiverRef = db.collection("accounts").document(receiverUid);

        // 2. Thực hiện Transaction trong Firestore
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            // Đọc số dư người gửi
            Double senderBalance = transaction.get(senderRef).getDouble("balance");
            Double receiverBalance = transaction.get(receiverRef).getDouble("balance");

            if (senderBalance == null) senderBalance = 0.0;
            if (receiverBalance == null) receiverBalance = 0.0;

            // Kiểm tra số dư
            if (senderBalance < amount) {
                throw new FirebaseFirestoreException("Insufficient funds", FirebaseFirestoreException.Code.ABORTED);
            }

            // Tính toán
            double newSenderBalance = senderBalance - amount;
            double newReceiverBalance = receiverBalance + amount;

            // Update số dư
            transaction.update(senderRef, "balance", newSenderBalance);
            transaction.update(receiverRef, "balance", newReceiverBalance);


            String transId = UUID.randomUUID().toString();

            // Ghi lịch sử cho Người Gửi
            Map<String, Object> senderLog = new HashMap<>();
            senderLog.put("userId", currentUserId); // Người sở hữu log này
            senderLog.put("type", "SENT");
            senderLog.put("amount", amount);
            senderLog.put("content", message);
            senderLog.put("timestamp", Timestamp.now());
            senderLog.put("relatedAccountName", receiverName);
            senderLog.put("relatedAccountNumber", receiverAccountNumber);
            senderLog.put("transactionId", transId);

            // Ghi lịch sử cho Người Nhận
            Map<String, Object> receiverLog = new HashMap<>();
            receiverLog.put("userId", receiverUid);
            receiverLog.put("type", "RECEIVED");
            receiverLog.put("amount", amount);
            receiverLog.put("content", message);
            receiverLog.put("timestamp", Timestamp.now());
            receiverLog.put("relatedAccountName", senderName);
            receiverLog.put("transactionId", transId);

            DocumentReference senderLogRef = senderRef.collection("transactions").document(transId);
            DocumentReference receiverLogRef = receiverRef.collection("transactions").document(transId);

            transaction.set(senderLogRef, senderLog);
            transaction.set(receiverLogRef, receiverLog);

            return null;
        }).addOnSuccessListener(aVoid -> {
            // Thành công
            // Thông báo người gửi
            sendNotification("Biến động số dư", "Tài khoản -" + formattedAmount + " VND. Nội dung: " + message);

            // Thông báo người nhận
            Map<String, Object> notification = new HashMap<>();
            notification.put("userId", receiverUid);
            notification.put("title", "Biến động số dư");
            notification.put("message", "Tài khoản " + receiverAccountNumber + ": +" + formattedAmount + " VND từ " + senderName + ". Nội dung: " + message);
            notification.put("timestamp", new Date());
            notification.put("isRead", false);

            db.collection("notifications")
                    .add(notification)
                    .addOnFailureListener(e -> {
                        // Log lỗi nếu cần, nhưng không chặn luồng chính vì tiền đã chuyển xong rồi
                    });

            Intent intent = new Intent(TransferDetailsActivity.this, TransferResultActivity.class);
            intent.putExtra("amount", amount);
            intent.putExtra("receiverName", receiverName);
            intent.putExtra("receiverAccount", receiverAccountNumber);
            intent.putExtra("content", message);
            intent.putExtra("refId", UUID.randomUUID().toString().substring(0, 10).toUpperCase());
            startActivity(intent);
            finish();

        }).addOnFailureListener(e -> {
            if (e.getMessage() != null && e.getMessage().contains("Insufficient funds")) {
                showErrorDialog("Số dư tài khoản không đủ để thực hiện thao tác");
            } else {
                showErrorDialog("Giao dịch thất bại: " + e.getMessage());
            }
        });
    }

    private void showErrorDialog(String msg) {
        new AlertDialog.Builder(this)
                .setTitle("Thông báo")
                .setMessage(msg)
                .setPositiveButton("Đóng", null)
                .show();
    }

    private void sendNotification(String title, String message) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "balance_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Balance Updates", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, builder.build());
    }

    // Lấy thông tin người gửi
    private void fetchSenderInfo() {
        if (currentUserId == null) return;
        db.collection("users")
                .document(currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot userDoc = task.getResult();
                        if (userDoc.exists()) {
                            // Lấy field "full_name" như trong đoạn mã mẫu
                            String name = userDoc.getString("full_name");

                            if (name != null && !name.isEmpty()) {
                                senderName = name;

                                if (edtMessage.getText().toString().isEmpty() ||
                                        edtMessage.getText().toString().equals("Người dùng ẩn danh chuyển tiền")) {
                                    edtMessage.setText(senderName + " chuyển tiền");
                                }
                            }
                            // 2. Lấy Số điện thoại
                            String phone = userDoc.getString("phone_number");
                            if (phone != null && !phone.isEmpty()) {
                                senderPhone = phone;
                            } else {
                                Toast.makeText(this, "Hồ sơ của bạn thiếu số điện thoại để xác thực OTP", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(this, "Không tìm thấy hồ sơ người dùng", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Lỗi kết nối server
                        Toast.makeText(this, "Lỗi kết nối khi lấy thông tin người gửi", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
