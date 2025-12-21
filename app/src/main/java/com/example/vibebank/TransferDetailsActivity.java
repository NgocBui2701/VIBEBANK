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
import android.widget.ProgressBar;
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
    private static final double AMOUNT_TRANSACTION_LIMIT = 500000000;
    private TextView tvSuggest1, tvSuggest2, tvSuggest3;

    // Các biến cho OTP và Lockout
    private PhoneAuthManager phoneAuthManager;
    private OtpBottomSheetDialog otpDialog;
    private SessionManager sessionManager;
    private static final String PREFS_SECURITY = "SecurityPrefs";
    private static final String KEY_FAILED_ATTEMPTS = "failed_attempts";
    private static final String KEY_LOCKOUT_TIME = "lockout_timestamp";
    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final long LOCKOUT_DURATION = 1 * 60 * 1000; // 30 phút
    private ProgressBar progressBar;
    private double currentUserBalance = 0;
    private boolean isSenderInfoLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_details);

        db = FirebaseFirestore.getInstance();
        phoneAuthManager = new PhoneAuthManager(this, this);
        sessionManager = new SessionManager(this);

        // Ánh xạ
        tvReceiverBank = findViewById(R.id.txtRecipientBank);
        tvReceiverName = findViewById(R.id.txtRecipientName);
        tvReceiverAccount = findViewById(R.id.txtRecipientAccount);
        edtAmount = findViewById(R.id.edtAmount);
        edtMessage = findViewById(R.id.edtMessage);
        cbSaveContact = findViewById(R.id.cbSaveRecipient);
        btnTransfer = findViewById(R.id.btnTransfer);
        View btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);

        llSuggestions = findViewById(R.id.llSuggestions);
        tvSuggest1 = findViewById(R.id.tvSuggest1);
        tvSuggest2 = findViewById(R.id.tvSuggest2);
        tvSuggest3 = findViewById(R.id.tvSuggest3);

        // Vô hiệu hóa chờ hoàn tất lấy dữ liệu
        btnTransfer.setEnabled(false);
        edtAmount.setEnabled(false);
        edtMessage.setEnabled(false);

        // Lấy ID người dùng hiện tại
        // Lấy ID người dùng từ SessionManager
        currentUserId = sessionManager.getCurrentUserId();

        // Lấy từ Firebase Auth
        if (currentUserId == null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        //  Lấy ID người dùng từ SharedPreferences
        if (currentUserId == null) {
            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            currentUserId = prefs.getString("current_user_id", null);
        }

        android.util.Log.d("TransferDetails", "Current User ID: " + currentUserId);

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

        // Validate - only accountNumber is required
        if (receiverAccountNumber == null || receiverAccountNumber.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không tìm thấy số tài khoản người nhận", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // If receiverName is null, use account number as fallback
        if (receiverName == null || receiverName.isEmpty()) {
            receiverName = "Tài khoản " + receiverAccountNumber;
        }

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
        if (!isSenderInfoLoaded) {
            Toast.makeText(this, "Đang tải thông tin tài khoản. Vui lòng đợi trong giây lát...", Toast.LENGTH_SHORT).show();
            // Tùy chọn: Gọi lại hàm tải dữ liệu nếu bị kẹt
            fetchSenderInfo();
            return; // Dừng ngay, không chạy tiếp code bên dưới
        }
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
        if (amount < 10000) {
            edtAmount.setError("Giao dịch tối thiểu 10.000 VND");
            return;
        }
        if (amount > AMOUNT_TRANSACTION_LIMIT) {
            edtAmount.setError("Giao dịch tối đa " + formatMoney(AMOUNT_TRANSACTION_LIMIT) + " VND");
            return;
        }
        if (amount > currentUserBalance) {
            showErrorDialog("Số dư tài khoản không đủ để thực hiện giao dịch");
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
            setLoading(true);
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
            setLoading(false);
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
        setLoading(false);
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
        final boolean isExternalBank = "EXTERNAL_BANK".equals(receiverUid);
        final DocumentReference receiverRef = isExternalBank ? null : db.collection("accounts").document(receiverUid);

        // 2. Thực hiện Transaction trong Firestore
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            // 1. READ ALL DATA FIRST
            // Đọc số dư người gửi
            Double senderBalance = transaction.get(senderRef).getDouble("balance");
            if (senderBalance == null) senderBalance = 0.0;

            // Đọc số dư người nhận (nếu là internal transfer)
            Double receiverBalance = 0.0;
            if (!isExternalBank && receiverRef != null) {
                receiverBalance = transaction.get(receiverRef).getDouble("balance");
                if (receiverBalance == null) receiverBalance = 0.0;
            }

            // 2. VALIDATE
            // Kiểm tra số dư
            if (senderBalance < amount) {
                throw new FirebaseFirestoreException("Insufficient funds", FirebaseFirestoreException.Code.ABORTED);
            }

            // 3. WRITE ALL DATA
            // Tính toán số dư mới
            double newSenderBalance = senderBalance - amount;
            double newReceiverBalance = receiverBalance + amount;

            // Update số dư người gửi
            transaction.update(senderRef, "balance", newSenderBalance);

            // Update số dư người nhận (nếu là internal transfer)
            if (!isExternalBank && receiverRef != null) {
                transaction.update(receiverRef, "balance", newReceiverBalance);
            }

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

            DocumentReference senderLogRef = senderRef.collection("transactions").document(transId);
            transaction.set(senderLogRef, senderLog);

            // Chỉ ghi lịch sử cho người nhận nếu là internal transfer
            if (!isExternalBank && receiverRef != null) {
                Map<String, Object> receiverLog = new HashMap<>();
                receiverLog.put("userId", receiverUid);
                receiverLog.put("type", "RECEIVED");
                receiverLog.put("amount", amount);
                receiverLog.put("content", message);
                receiverLog.put("timestamp", Timestamp.now());
                receiverLog.put("relatedAccountName", senderName);
                receiverLog.put("transactionId", transId);

                DocumentReference receiverLogRef = receiverRef.collection("transactions").document(transId);
                transaction.set(receiverLogRef, receiverLog);
            }

            return null;
        }).addOnSuccessListener(aVoid -> {
            // Thành công
            // Thông báo người gửi qua notification bar
            sendNotification("Biến động số dư", "Tài khoản -" + formattedAmount + " VND. Nội dung: " + message);

            // Lưu thông báo người gửi vào Firestore
            Map<String, Object> senderNotification = new HashMap<>();
            senderNotification.put("userId", currentUserId);
            senderNotification.put("title", "Biến động số dư");
            senderNotification.put("message", "Chuyển tiền -" + formattedAmount + " VND đến " + receiverName + " (" + receiverAccountNumber + "). Nội dung: " + message);
            senderNotification.put("timestamp", new Date());
            senderNotification.put("isRead", false);

            db.collection("notifications")
                    .add(senderNotification)
                    .addOnFailureListener(e -> {
                        // Log lỗi nhưng không chặn luồng
                    });

            // Chỉ thông báo người nhận nếu là internal transfer
            if (!isExternalBank) {
                Map<String, Object> receiverNotification = new HashMap<>();
                receiverNotification.put("userId", receiverUid);
                receiverNotification.put("title", "Biến động số dư");
                receiverNotification.put("message", "Nhận tiền +" + formattedAmount + " VND từ " + senderName + ". Nội dung: " + message);
                receiverNotification.put("timestamp", new Date());
                receiverNotification.put("isRead", false);

                db.collection("notifications")
                        .add(receiverNotification)
                        .addOnFailureListener(e -> {
                            // Log lỗi nếu cần, nhưng không chặn luồng chính vì tiền đã chuyển xong rồi
                        });
            }

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
        isSenderInfoLoaded = false;
        setLoading(true);

        // Dùng Tasks.whenAll để đợi cả 2 việc: Lấy Profile (SĐT) và Lấy Số Dư
        DocumentReference userRef = db.collection("users").document(currentUserId);
        DocumentReference accRef = db.collection("accounts").document(currentUserId);

        // Chạy song song
        userRef.get().addOnCompleteListener(taskUser -> {
            accRef.get().addOnCompleteListener(taskAcc -> {
                setLoading(false); // Tắt loading

                if (taskUser.isSuccessful() && taskAcc.isSuccessful()) {
                    // 1. Xử lý User Info
                    DocumentSnapshot userDoc = taskUser.getResult();
                    if (userDoc != null && userDoc.exists()) {
                        senderName = userDoc.getString("full_name");
                        senderPhone = userDoc.getString("phone_number");

                        // Auto-fill message nếu cần
                        if (senderName != null && edtMessage.getText().toString().isEmpty()) {
                            edtMessage.setText(senderName + " chuyển tiền");
                        }
                    }

                    // 2. Xử lý Account Info
                    DocumentSnapshot accDoc = taskAcc.getResult();
                    if (accDoc != null && accDoc.exists()) {
                        Double balance = accDoc.getDouble("balance");
                        if (balance != null) currentUserBalance = balance;
                    }

                    // 3. Kiểm tra điều kiện để mở nút Transfer
                    if (senderPhone != null && !senderPhone.isEmpty()) {
                        isSenderInfoLoaded = true;
                        btnTransfer.setEnabled(true);
                        edtAmount.setEnabled(true);
                        edtMessage.setEnabled(true);
                    } else {
                        Toast.makeText(this, "Lỗi: Tài khoản chưa cập nhật số điện thoại (bắt buộc để nhận OTP).", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(this, "Không thể lấy thông tin tài khoản. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                }
            });
        });
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

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            btnTransfer.setEnabled(false);
            // Làm mờ màn hình
            findViewById(R.id.transferDetails).setAlpha(0.5f);
        } else {
            progressBar.setVisibility(View.GONE);
            btnTransfer.setEnabled(true);
            findViewById(R.id.transferDetails).setAlpha(1.0f);
        }
    }
}