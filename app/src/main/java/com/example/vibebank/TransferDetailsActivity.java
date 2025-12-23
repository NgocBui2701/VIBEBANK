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
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.vibebank.ui.OtpBottomSheetDialog;
import com.example.vibebank.utils.BiometricHelper;
import com.example.vibebank.utils.ElectricBillMockService;
import com.example.vibebank.utils.FlightTicketMockService;
import com.example.vibebank.utils.MovieTicketMockService;
import com.example.vibebank.utils.PhoneAuthManager;
import com.example.vibebank.utils.SessionManager;
import com.example.vibebank.utils.TicketDatabaseService;
import com.example.vibebank.utils.WaterBillMockService;

import java.util.ArrayList;
import com.example.vibebank.utils.WaterBillMockService;
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

import javax.crypto.Cipher;

public class TransferDetailsActivity extends AppCompatActivity implements
        PhoneAuthManager.PhoneAuthCallback,
        OtpBottomSheetDialog.OtpVerificationListener {

    private TextView tvReceiverName, tvReceiverAccount, tvReceiverBank;
    private EditText edtAmount, edtMessage;
    private CheckBox cbSaveContact;
    private Button btnTransfer;
    private Spinner spinnerSourceAccount;
    private FirebaseFirestore db;
    private String currentUserId = null;
    private String senderName = "Người dùng ẩn danh";
    private String senderPhone = "";
    private String receiverUid, receiverAccountNumber, receiverName;
    private LinearLayout llSuggestions;
    private static final double MAX_SUGGESTION_LIMIT = 999999999;
    private static final double AMOUNT_TRANSACTION_LIMIT = 500000000;
    private TextView tvSuggest1, tvSuggest2, tvSuggest3;
    
    // Source account info
    private int selectedAccountType = 0; // 0: Payment, 1: Saving, 2: Credit
    private double paymentBalance = 0;
    private double savingBalance = 0;
    private double creditAvailable = 0; // Credit limit - debt
    private java.util.List<String> accountOptions = new java.util.ArrayList<>();

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
    
    // Flags to prevent duplicate booking saves
    private boolean isFlightTicketSaved = false;
    private boolean isMovieTicketSaved = false;
    private boolean isHotelBookingSaved = false;
    
    // Flag to prevent duplicate transaction execution
    private boolean isTransactionProcessing = false;
    
    // Flag to prevent duplicate navigation to result screen
    private boolean hasNavigatedToResult = false;
    
    // Flag to prevent duplicate OTP verification callback
    private boolean isOtpVerified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_details);

        db = FirebaseFirestore.getInstance();
        phoneAuthManager = new PhoneAuthManager(this, this);
        sessionManager = new SessionManager(this);
        
        // Initialize TicketDatabaseService for flight and movie ticket bookings
        TicketDatabaseService.init();
        android.util.Log.d("TransferDetailsActivity", "TicketDatabaseService initialized");

        // Ánh xạ
        tvReceiverBank = findViewById(R.id.txtRecipientBank);
        tvReceiverName = findViewById(R.id.txtRecipientName);
        tvReceiverAccount = findViewById(R.id.txtRecipientAccount);
        edtAmount = findViewById(R.id.edtAmount);
        edtMessage = findViewById(R.id.edtMessage);
        cbSaveContact = findViewById(R.id.cbSaveRecipient);
        btnTransfer = findViewById(R.id.btnTransfer);
        spinnerSourceAccount = findViewById(R.id.spinnerSourceAccount);
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

        // Check if this is a service payment (electric, water, topup, flight, movie, hotel)
        boolean isElectricBillPayment = getIntent().getBooleanExtra("isElectricBillPayment", false);
        boolean isWaterBillPayment = getIntent().getBooleanExtra("isWaterBillPayment", false);
        boolean isTopupPayment = getIntent().getBooleanExtra("isTopupPayment", false);
        boolean isFlightTicketPayment = getIntent().getBooleanExtra("isFlightTicketPayment", false);
        boolean isMovieTicketPayment = getIntent().getBooleanExtra("isMovieTicketPayment", false);
        boolean isHotelBookingPayment = getIntent().getBooleanExtra("isHotelBookingPayment", false);
        
        boolean isServicePayment = isElectricBillPayment || isWaterBillPayment || isTopupPayment || 
                                   isFlightTicketPayment || isMovieTicketPayment || isHotelBookingPayment;

        // For service payments, set default values if not provided
        if (isServicePayment) {
            if (receiverAccountNumber == null || receiverAccountNumber.isEmpty()) {
                receiverAccountNumber = "SERVICE";
            }
            if (receiverName == null || receiverName.isEmpty()) {
                if (isElectricBillPayment) receiverName = "Thanh toán tiền điện";
                else if (isWaterBillPayment) receiverName = "Thanh toán tiền nước";
                else if (isTopupPayment) receiverName = "Nạp tiền điện thoại";
                else if (isFlightTicketPayment) receiverName = "Thanh toán vé máy bay";
                else if (isMovieTicketPayment) receiverName = "Thanh toán vé xem phim";
                else if (isHotelBookingPayment) receiverName = "Thanh toán đặt phòng khách sạn";
            }
            if (receiverUid == null || receiverUid.isEmpty()) {
                receiverUid = "SYSTEM_SERVICE";
            }
            if (bankName == null) {
                bankName = "VibeBank";
            }
        }

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

        // Auto-fill amount if provided
        String prefilledAmount = getIntent().getStringExtra("amount");
        if (prefilledAmount != null && !prefilledAmount.isEmpty()) {
            edtAmount.setText(prefilledAmount);
        }

        // Kiểm tra người nhận đã tồn tại trong danh sách đã lưu
        SharedPreferences prefs = getSharedPreferences("SavedRecipients", MODE_PRIVATE);
        if (prefs.contains(receiverAccountNumber)) {
            cbSaveContact.setChecked(true);
            cbSaveContact.setEnabled(false);
        }

        setupMoneyInput();

        btnBack.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        // Mở nút chuyển tiền khi nhập đủ
        btnTransfer.setEnabled(true);
        btnTransfer.setOnClickListener(v -> preCheckTransfer());
    }

    private void preCheckTransfer() {
        // Prevent starting new transaction if one is already processing
        if (isTransactionProcessing) {
            android.util.Log.w("TransferDetailsActivity", "⚠️ Transaction already in progress, ignoring button click");
            Toast.makeText(this, "Giao dịch đang được xử lý...", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!isSenderInfoLoaded) {
            Toast.makeText(this, "Đang tải thông tin tài khoản. Vui lòng đợi trong giây lát...", Toast.LENGTH_SHORT).show();
            // Tùy chọn: Gọi lại hàm tải dữ liệu nếu bị kẹt
            fetchSenderInfo();
            return; // Dừng ngay, không chạy tiếp code bên dưới
        }
        // Kiểm tra đầu vào cơ bản
        String amountStr = edtAmount.getText().toString().replace(".", "");

        // KIỂM TRA KHÓA (LOCKOUT)
        if (isLockedOut()) {
            long remainingTime = getRemainingLockoutTime();
            long minutes = remainingTime / 60000;
            showErrorDialog("Tài khoản bị tạm khóa chức năng chuyển tiền do nhập sai OTP quá 3 lần. Vui lòng thử lại sau " + (minutes + 1) + " phút.");
            return;
        }

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
        // Validate amount against selected account balance
        double availableBalance = 0;
        String accountName = "";
        switch (selectedAccountType) {
            case 0: // Payment Account
                availableBalance = paymentBalance;
                accountName = "Thanh toán";
                break;
            case 1: // Saving Account
                availableBalance = savingBalance;
                accountName = "Tiết kiệm";
                break;
            case 2: // Credit Card
                availableBalance = creditAvailable;
                accountName = "Credit";
                break;
        }
        
        if (amount > availableBalance) {
            android.util.Log.d("TransferDetails", "Validation failed - Amount: " + amount + ", Available: " + availableBalance + ", Account Type: " + selectedAccountType);
            if (selectedAccountType == 2) {
                showErrorDialog("Hạn mức Credit khả dụng không đủ để thực hiện giao dịch");
            } else {
                showErrorDialog("Số dư tài khoản " + accountName + " không đủ để thực hiện giao dịch");
            }
            return;
        }
        
        android.util.Log.d("TransferDetails", "Validation passed - Amount: " + amount + ", Available: " + availableBalance + ", Account Type: " + selectedAccountType);

        // KIỂM TRA KHÓA (LOCKOUT)
        if (isLockedOut()) {
            long remainingTime = getRemainingLockoutTime();
            long minutes = remainingTime / 60000;
            showErrorDialog("Tài khoản bị tạm khóa chức năng chuyển tiền do nhập sai OTP quá 3 lần. Vui lòng thử lại sau " + (minutes + 1) + " phút.");
            return;
        }

        // Nếu có SĐT thì gửi OTP
        if (senderPhone != null && !senderPhone.isEmpty()) {
            isOtpVerified = false;
            btnTransfer.setEnabled(false);
            setLoading(true);
            Toast.makeText(this, "Đang gửi mã OTP...", Toast.LENGTH_SHORT).show();
            phoneAuthManager.sendOtp(senderPhone);
        } else {
            showErrorDialog("Không tìm thấy số điện thoại xác thực.");
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
            btnTransfer.setEnabled(true);
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
            // Keep button enabled so user can try again
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
        // Re-enable button in case user wants to cancel and try again
        btnTransfer.setEnabled(true);
        // OTP đã gửi thành công -> Hiện Dialog nhập
        otpDialog = OtpBottomSheetDialog.newInstance(""); // Truyền sđt nếu muốn hiển thị
        otpDialog.setOtpVerificationListener(this);
        otpDialog.show(getSupportFragmentManager(), "OtpTransferDialog");
    }

    @Override
    public void onVerificationSuccess() {
        // CRITICAL: Prevent duplicate Firebase callback execution
        if (isOtpVerified) {
            android.util.Log.w("TransferDetailsActivity", "⚠️⚠️⚠️ Firebase callback fired again but OTP already verified - BLOCKING");
            return;
        }
        isOtpVerified = true;
        android.util.Log.d("TransferDetailsActivity", "✓ OTP verified, flag set to prevent duplicates");
        
        // Prevent duplicate callback execution
        if (isTransactionProcessing) {
            android.util.Log.w("TransferDetailsActivity", "⚠️ Verification success callback called but transaction already processing");
            return;
        }
        
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
        // Re-enable button when verification fails
        btnTransfer.setEnabled(true);
        android.util.Log.d("TransferDetailsActivity", "✓ Transfer button re-enabled after verification failure");
        
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
        
        // Prevent duplicate transaction execution
        if (isTransactionProcessing) {
            android.util.Log.w("TransferDetailsActivity", "⚠️ Transaction already in progress, skipping duplicate call");
            return;
        }
        
        isTransactionProcessing = true;
        android.util.Log.d("TransferDetailsActivity", "✓ Starting transaction processing");

        if (currentUserId == null) {
            Toast.makeText(this, "Lỗi: Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.", Toast.LENGTH_LONG).show();
            isTransactionProcessing = false; // Reset flag on error
            return;
        }

        // Validate receiverUid (skip for external bank transfers)
        boolean isExternalTransfer = "EXTERNAL_BANK".equals(receiverUid);
        if (!isExternalTransfer && (receiverUid == null || receiverUid.isEmpty())) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID người nhận. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
            isTransactionProcessing = false; // Reset flag on error
            return;
        }

        String amountStr = edtAmount.getText().toString();
        String message = edtMessage.getText().toString();

        if (amountStr.isEmpty()) {
            edtAmount.setError("Vui lòng nhập số tiền");
            isTransactionProcessing = false; // Reset flag on error
            return;
        }

        String cleanAmountStr = amountStr.replace(".", "");

        double amount = Double.parseDouble(cleanAmountStr);

        String formattedAmount = formatMoney(amount);

        // Kiểm tra số tiền tối thiểu
        if (amount <= 0) {
            edtAmount.setError("Số tiền phải lớn hơn 0");
            isTransactionProcessing = false; // Reset flag on error
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
        final DocumentReference senderSavingRef = db.collection("savings").document(currentUserId);
        final DocumentReference senderCreditRef = db.collection("credit_cards").document(currentUserId);
        final boolean isExternalBank = "EXTERNAL_BANK".equals(receiverUid);
        final boolean isSystemService = "SYSTEM_SERVICE".equals(receiverUid);
        final DocumentReference receiverRef = (isExternalBank || isSystemService) ? null : db.collection("accounts").document(receiverUid);

        // 2. Thực hiện Transaction trong Firestore
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            // 1. READ ALL DATA FIRST (MUST READ EVERYTHING BEFORE ANY WRITES)
            DocumentSnapshot paymentSnapshot = transaction.get(senderRef);
            DocumentSnapshot savingSnapshot = transaction.get(senderSavingRef);
            DocumentSnapshot creditSnapshot = transaction.get(senderCreditRef);
            
            // Read receiver if applicable
            DocumentSnapshot receiverSnapshot = null;
            Double receiverBalance = 0.0;
            boolean receiverExists = false;
            if (!isExternalBank && !isSystemService && receiverRef != null) {
                receiverSnapshot = transaction.get(receiverRef);
                if (receiverSnapshot.exists()) {
                    receiverExists = true;
                    receiverBalance = receiverSnapshot.getDouble("balance");
                    if (receiverBalance == null) receiverBalance = 0.0;
                }
            }
            
            // Process data based on selected account type
            Double senderBalance = 0.0;
            Double senderDebt = 0.0;
            Double creditLimit = 0.0;
            boolean needCreateCreditCard = false;
            boolean needCreateSaving = false;
            
            switch (selectedAccountType) {
                case 0: // Payment Account
                    if (paymentSnapshot.exists()) {
                        senderBalance = paymentSnapshot.getDouble("balance");
                        if (senderBalance == null) senderBalance = 0.0;
                    } else {
                        throw new FirebaseFirestoreException("Payment account does not exist", FirebaseFirestoreException.Code.ABORTED);
                    }
                    break;
                case 1: // Saving Account
                    if (savingSnapshot.exists()) {
                        senderBalance = savingSnapshot.getDouble("balance");
                        if (senderBalance == null) senderBalance = 0.0;
                    } else {
                        // Saving account not created yet
                        throw new FirebaseFirestoreException("Saving account does not exist. Please deposit first.", FirebaseFirestoreException.Code.ABORTED);
                    }
                    break;
                case 2: // Credit Card
                    if (creditSnapshot.exists()) {
                        senderDebt = creditSnapshot.getDouble("debt");
                        creditLimit = creditSnapshot.getDouble("credit_limit");
                        if (senderDebt == null) senderDebt = 0.0;
                        if (creditLimit == null) creditLimit = 10000000.0; // Default 10 triệu
                    } else {
                        // Will create document in write phase
                        needCreateCreditCard = true;
                        senderDebt = 0.0;
                        creditLimit = 10000000.0;
                    }
                    senderBalance = creditLimit - senderDebt; // Available credit
                    break;
            }

            // 2. VALIDATE
            // Kiểm tra số dư/hạn mức khả dụng
            // For Credit card (type 2), senderBalance is creditAvailable which is already validated
            // For Payment and Saving, check actual balance
            if (senderBalance < amount) {
                throw new FirebaseFirestoreException("Insufficient funds", FirebaseFirestoreException.Code.ABORTED);
            }

            // 3. WRITE ALL DATA
            // Tính toán số dư mới dựa trên loại tài khoản
            double newReceiverBalance = receiverBalance + amount;
            
            // Update số dư người gửi dựa trên loại tài khoản
            switch (selectedAccountType) {
                case 0: // Payment Account - deduct from balance
                    double newPaymentBalance = senderBalance - amount;
                    transaction.update(senderRef, "balance", newPaymentBalance);
                    break;
                case 1: // Saving Account - deduct from saving balance
                    double newSavingBalance = senderBalance - amount;
                    transaction.update(senderSavingRef, "balance", newSavingBalance);
                    break;
                case 2: // Credit Card - increase debt
                    double newDebt = senderDebt + amount;
                    if (needCreateCreditCard) {
                        // Create new document with initial debt
                        Map<String, Object> creditData = new HashMap<>();
                        creditData.put("user_id", currentUserId);
                        creditData.put("credit_limit", creditLimit);
                        creditData.put("debt", newDebt); // Set debt with transaction amount
                        creditData.put("interest_rate", 0.18);
                        transaction.set(senderCreditRef, creditData);
                        android.util.Log.d("TransferDetails", "Created new credit card with limit: " + creditLimit + ", initial debt: " + newDebt);
                    } else {
                        // Update existing document
                        transaction.update(senderCreditRef, "debt", newDebt);
                    }
                    break;
            }

            // Update số dư người nhận (chỉ nếu là internal transfer và không phải system service)
            if (!isExternalBank && !isSystemService && receiverRef != null && receiverExists) {
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
            senderLog.put("sourceAccountType", selectedAccountType); // 0: Payment, 1: Saving, 2: Credit

            // Chọn đúng collection để ghi log dựa trên account type
            DocumentReference senderLogRef;
            switch (selectedAccountType) {
                case 0: // Payment Account
                    senderLogRef = senderRef.collection("transactions").document(transId);
                    break;
                case 1: // Saving Account
                    senderLogRef = senderSavingRef.collection("transactions").document(transId);
                    break;
                case 2: // Credit Card
                    senderLogRef = senderCreditRef.collection("transactions").document(transId);
                    break;
                default:
                    senderLogRef = senderRef.collection("transactions").document(transId);
                    break;
            }
            transaction.set(senderLogRef, senderLog);

            // Chỉ ghi lịch sử cho người nhận nếu là internal transfer và không phải system service
            if (!isExternalBank && !isSystemService && receiverRef != null) {
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
            // Reset transaction flag on success
            isTransactionProcessing = false;
            android.util.Log.d("TransferDetailsActivity", "✓ Transaction completed successfully, flag reset");
            
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

            // If this is an electric bill payment, mark it as paid
            String billCustomerId = getIntent().getStringExtra("billCustomerId");
            if (billCustomerId != null && !billCustomerId.isEmpty()) {
                com.example.vibebank.utils.ElectricBillMockService.initialize(TransferDetailsActivity.this);
                com.example.vibebank.utils.ElectricBillMockService.payBill(billCustomerId);
                android.util.Log.d("TransferDetailsActivity", "Marked electric bill as paid: " + billCustomerId);
            }

            // If this is a water bill payment, mark it as paid
            boolean isWaterBillPayment = getIntent().getBooleanExtra("isWaterBillPayment", false);
            if (isWaterBillPayment && billCustomerId != null && !billCustomerId.isEmpty()) {
                com.example.vibebank.utils.WaterBillMockService.initialize(TransferDetailsActivity.this);
                com.example.vibebank.utils.WaterBillMockService.payBill(billCustomerId);
                android.util.Log.d("TransferDetailsActivity", "Marked water bill as paid: " + billCustomerId);
            }

            // If this is a topup payment, just log it (no need to mark anything as paid)
            boolean isTopupPayment = getIntent().getBooleanExtra("isTopupPayment", false);
            if (isTopupPayment) {
                String topupPhoneNumber = getIntent().getStringExtra("topupPhoneNumber");
                String topupPackageName = getIntent().getStringExtra("topupPackageName");
                android.util.Log.d("TransferDetailsActivity", "Topup payment completed: " + topupPhoneNumber + " - " + topupPackageName);
            }

            // If this is a flight ticket payment, save booking to Firestore
            boolean isFlightTicketPayment = getIntent().getBooleanExtra("isFlightTicketPayment", false);
            android.util.Log.d("TransferDetailsActivity", "isFlightTicketPayment = " + isFlightTicketPayment);
            
            if (isFlightTicketPayment && !isFlightTicketSaved) {
                isFlightTicketSaved = true; // Set flag immediately to prevent duplicate saves
                
                String flightCode = getIntent().getStringExtra("flightCode");
                String airline = getIntent().getStringExtra("airline");
                String departure = getIntent().getStringExtra("departure");
                String destination = getIntent().getStringExtra("destination");
                String departureDate = getIntent().getStringExtra("departureDate");
                String departureTime = getIntent().getStringExtra("departureTime");
                String arrivalTime = getIntent().getStringExtra("arrivalTime");
                String seatClass = getIntent().getStringExtra("seatClass");
                String duration = getIntent().getStringExtra("duration");
                String passengerName = getIntent().getStringExtra("passengerName");
                String passengerID = getIntent().getStringExtra("passengerID");
                String passengerPhone = getIntent().getStringExtra("passengerPhone");
                String passengerEmail = getIntent().getStringExtra("passengerEmail");
                
                android.util.Log.d("TransferDetailsActivity", "========== BOOKING FLIGHT TICKET ==========");
                android.util.Log.d("TransferDetailsActivity", "Flight: " + flightCode + ", Airline: " + airline);
                android.util.Log.d("TransferDetailsActivity", "Route: " + departure + " → " + destination);
                android.util.Log.d("TransferDetailsActivity", "Date: " + departureDate + ", Time: " + departureTime);
                android.util.Log.d("TransferDetailsActivity", "Passenger: " + passengerName + ", " + passengerPhone);
                android.util.Log.d("TransferDetailsActivity", "Amount: " + amount);
                
                // Create flight ticket data map
                java.util.Map<String, Object> flightData = new java.util.HashMap<>();
                flightData.put("bookingId", "BK" + System.currentTimeMillis());
                flightData.put("flightCode", flightCode);
                flightData.put("airline", airline);
                flightData.put("departure", departure);
                flightData.put("destination", destination);
                flightData.put("departureDate", departureDate);
                flightData.put("departureTime", departureTime);
                flightData.put("arrivalTime", arrivalTime);
                flightData.put("seatClass", seatClass);
                flightData.put("price", (long) amount);
                flightData.put("duration", duration);
                flightData.put("passengerName", passengerName);
                flightData.put("passengerID", passengerID);
                flightData.put("passengerPhone", passengerPhone);
                flightData.put("passengerEmail", passengerEmail);
                flightData.put("bookingTime", new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(new java.util.Date()));
                flightData.put("timestamp", System.currentTimeMillis());
                
                // Save to Firestore
                TicketDatabaseService.saveFlightTicket(flightData, new TicketDatabaseService.OnSaveListener() {
                    @Override
                    public void onSuccess(String bookingId) {
                        android.util.Log.d("TransferDetailsActivity", "✓✓✓ Flight ticket saved to Firestore successfully. ID: " + bookingId);
                    }
                    
                    @Override
                    public void onError(String error) {
                        android.util.Log.e("TransferDetailsActivity", "✗✗✗ Failed to save flight ticket to Firestore: " + error);
                    }
                });
            }

            // If this is a movie ticket payment, save booking
            boolean isMovieTicketPayment = getIntent().getBooleanExtra("isMovieTicketPayment", false);
            if (isMovieTicketPayment && !isMovieTicketSaved) {
                isMovieTicketSaved = true; // Set flag immediately to prevent duplicate saves
                
                String movieTitle = getIntent().getStringExtra("movieTitle");
                String cinemaName = getIntent().getStringExtra("cinemaName");
                String date = getIntent().getStringExtra("date");
                String time = getIntent().getStringExtra("time");
                String showtimeKey = getIntent().getStringExtra("showtimeKey");
                ArrayList<String> seats = getIntent().getStringArrayListExtra("seats");
                String customerName = getIntent().getStringExtra("customerName");
                String customerPhone = getIntent().getStringExtra("customerPhone");
                String customerEmail = getIntent().getStringExtra("customerEmail");
                
                android.util.Log.d("TransferDetailsActivity", "========== BOOKING MOVIE TICKET ==========");
                android.util.Log.d("TransferDetailsActivity", "Movie: " + movieTitle + ", Cinema: " + cinemaName);
                android.util.Log.d("TransferDetailsActivity", "Date: " + date + ", Time: " + time);
                android.util.Log.d("TransferDetailsActivity", "Seats: " + seats);
                
                // Create movie ticket data map
                java.util.Map<String, Object> movieData = new java.util.HashMap<>();
                movieData.put("bookingId", "MV" + System.currentTimeMillis());
                movieData.put("movieTitle", movieTitle);
                movieData.put("cinemaName", cinemaName);
                movieData.put("date", date);
                movieData.put("time", time);
                movieData.put("showtimeKey", showtimeKey);
                movieData.put("seats", new java.util.ArrayList<>(seats));
                movieData.put("totalPrice", (long) amount);
                movieData.put("customerName", customerName);
                movieData.put("customerPhone", customerPhone);
                movieData.put("customerEmail", customerEmail);
                movieData.put("bookingTime", new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(new java.util.Date()));
                movieData.put("timestamp", System.currentTimeMillis());
                
                // Save to Firestore
                TicketDatabaseService.saveMovieTicket(movieData, new TicketDatabaseService.OnSaveListener() {
                    @Override
                    public void onSuccess(String bookingId) {
                        android.util.Log.d("TransferDetailsActivity", "✓✓✓ Movie ticket saved to Firestore successfully. ID: " + bookingId);
                    }
                    
                    @Override
                    public void onError(String error) {
                        android.util.Log.e("TransferDetailsActivity", "✗✗✗ Failed to save movie ticket to Firestore: " + error);
                    }
                });
            }

            // Prevent duplicate navigation
            if (hasNavigatedToResult) {
                android.util.Log.w("TransferDetailsActivity", "⚠️ Already navigated to result screen, preventing duplicate");
                return;
            }
            hasNavigatedToResult = true;
            android.util.Log.d("TransferDetailsActivity", "✓ Navigating to result screen");
            
            // For hotel booking, return result directly without going to TransferResultActivity
            boolean isHotelBookingPayment = getIntent().getBooleanExtra("isHotelBookingPayment", false);
            if (isHotelBookingPayment) {
                android.util.Log.d("TransferDetailsActivity", "Hotel booking payment completed - returning to booking activity");
                Intent resultIntent = new Intent();
                resultIntent.putExtra("paymentSuccess", true);
                setResult(RESULT_OK, resultIntent);
                finish();
                return;
            }

            Intent intent = new Intent(TransferDetailsActivity.this, TransferResultActivity.class);
            intent.putExtra("amount", amount);
            intent.putExtra("receiverName", receiverName);
            intent.putExtra("receiverAccount", receiverAccountNumber);
            intent.putExtra("content", message);
            intent.putExtra("refId", UUID.randomUUID().toString().substring(0, 10).toUpperCase());
            
            // Set result for caller activity
            setResult(RESULT_OK, intent);
            
            startActivity(intent);
            finish();

        }).addOnFailureListener(e -> {
            // Reset transaction flag on failure
            isTransactionProcessing = false;
            android.util.Log.e("TransferDetailsActivity", "✗ Transaction failed, flag reset: " + e.getMessage());
            
            setResult(RESULT_CANCELED);
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
        DocumentReference savingRef = db.collection("savings").document(currentUserId);
        DocumentReference creditRef = db.collection("credit_cards").document(currentUserId);

        // Chạy song song cả 4 queries
        userRef.get().addOnCompleteListener(taskUser -> {
            accRef.get().addOnCompleteListener(taskAcc -> {
                savingRef.get().addOnCompleteListener(taskSaving -> {
                    creditRef.get().addOnCompleteListener(taskCredit -> {
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

                            // 2. Xử lý Payment Account Info
                            DocumentSnapshot accDoc = taskAcc.getResult();
                            if (accDoc != null && accDoc.exists()) {
                                Double balance = accDoc.getDouble("balance");
                                if (balance != null) {
                                    currentUserBalance = balance;
                                    paymentBalance = balance;
                                }
                            }

                            // 3. Xử lý Saving Account Info
                            if (taskSaving.isSuccessful()) {
                                DocumentSnapshot savingDoc = taskSaving.getResult();
                                if (savingDoc != null && savingDoc.exists()) {
                                    Double balance = savingDoc.getDouble("balance");
                                    if (balance != null) savingBalance = balance;
                                }
                            }

                            // 4. Xử lý Credit Card Info
                            if (taskCredit.isSuccessful()) {
                                DocumentSnapshot creditDoc = taskCredit.getResult();
                                if (creditDoc != null && creditDoc.exists()) {
                                    Double limit = creditDoc.getDouble("credit_limit");
                                    Double debt = creditDoc.getDouble("debt");
                                    android.util.Log.d("TransferDetails", "Credit Card exists - Limit: " + limit + ", Debt: " + debt);
                                    
                                    if (limit != null) {
                                        if (debt == null) debt = 0.0;
                                        creditAvailable = limit - debt;
                                        android.util.Log.d("TransferDetails", "Credit Available calculated: " + creditAvailable);
                                    } else {
                                        // Default credit limit nếu null
                                        creditAvailable = 10000000.0;
                                        android.util.Log.w("TransferDetails", "Credit limit is null, using default: 10,000,000");
                                    }
                                } else {
                                    // Nếu document không tồn tại, sử dụng hạn mức mặc định
                                    creditAvailable = 10000000.0;
                                    android.util.Log.d("TransferDetails", "Credit card document does not exist, using default available: 10,000,000");
                                }
                            } else {
                                android.util.Log.e("TransferDetails", "Failed to fetch credit card info: " + taskCredit.getException());
                                creditAvailable = 0.0;
                            }

                            // 5. Load account options to spinner
                            loadAccountOptions();

                            // 6. Kiểm tra điều kiện để mở nút Transfer
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

    private void loadAccountOptions() {
        accountOptions.clear();
        
        // 0: Payment Account
        accountOptions.add("TK Thanh toán - " + formatMoney(paymentBalance) + " VNĐ");
        
        // 1: Saving Account
        accountOptions.add("TK Tiết kiệm - " + formatMoney(savingBalance) + " VNĐ");
        
        // 2: Credit Card
        accountOptions.add("Thẻ Credit - " + formatMoney(creditAvailable) + " VNĐ khả dụng");
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            accountOptions
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSourceAccount.setAdapter(adapter);
        
        // Set default selection to Payment account
        spinnerSourceAccount.setSelection(0);
        selectedAccountType = 0;
        
        // Handle spinner selection
        spinnerSourceAccount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedAccountType = position;
                android.util.Log.d("TransferDetails", "Selected account type: " + selectedAccountType + " - " + accountOptions.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedAccountType = 0; // Default to Payment account
            }
        });
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