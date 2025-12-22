package com.example.vibebank;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.vibebank.utils.ExternalBankSimulator;
import com.example.vibebank.utils.VietQRParser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import org.json.JSONObject;

import java.io.InputStream;

public class ScanQRActivity extends AppCompatActivity {

    private ImageView btnBack;
    private LinearLayout btnGallery;
    private DecoratedBarcodeView barcodeScanner;
    private boolean isScanning = false;
    private ProgressDialog progressDialog;

    private static final int CAMERA_PERMISSION_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr);

        initViews();
        setupListeners();
        checkCameraPermission();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnGallery = findViewById(R.id.btnGallery);
        barcodeScanner = findViewById(R.id.barcodeScanner);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnGallery.setOnClickListener(v -> openGallery());
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
        } else {
            initScanner();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initScanner();
            } else {
                Toast.makeText(this, "Cần quyền camera để quét mã QR", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void initScanner() {
        barcodeScanner.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (!isScanning && result.getText() != null) {
                    isScanning = true;
                    barcodeScanner.pause();
                    processQRCode(result.getText());
                }
            }

            @Override
            public void possibleResultPoints(java.util.List<com.google.zxing.ResultPoint> resultPoints) {
                // Optional: Show scanning indicator
            }
        });
        barcodeScanner.resume();
    }

    /**
     * Process QR code - Try VietQR first, fallback to JSON
     */
    private void processQRCode(String qrContent) {
        // Log full content
        android.util.Log.d("ScanQR", "Full QR: " + qrContent);
        
        // Try parsing as VietQR (EMVCo standard)
        VietQRParser parser = new VietQRParser(qrContent);
        
        boolean isValid = parser.isValidVietQR();
        android.util.Log.d("ScanQR", "isValidVietQR: " + isValid);
        
        if (isValid) {
            // Real VietQR detected
            android.util.Log.d("ScanQR", "Handling VietQR");
            handleVietQR(parser);
        } else {
            // Try JSON format (internal VIBEBANK)
            android.util.Log.d("ScanQR", "Trying JSON format");
            handleJSONQR(qrContent);
        }
    }

    /**
     * Handle real VietQR code (EMVCo standard)
     */
    private void handleVietQR(VietQRParser parser) {
        String bankBIN = parser.getBankBIN();
        String accountNumber = parser.getAccountNumber();
        String amount = parser.getAmount();
        String description = parser.getDescription();

        android.util.Log.d("ScanQR", "BIN: " + bankBIN + ", Account: " + accountNumber);

        if (bankBIN == null || accountNumber == null) {
            Toast.makeText(this, "Dữ liệu QR không đầy đủ", Toast.LENGTH_SHORT).show();
            isScanning = false;
            barcodeScanner.resume();
            return;
        }

        showLoadingDialog("Đang tra cứu thông tin...");

        ExternalBankSimulator simulator = ExternalBankSimulator.getInstance();
        
        boolean isInternal = simulator.isInternalBank(bankBIN);
        android.util.Log.d("ScanQR", "isInternal: " + isInternal);
        
        if (isInternal) {
            // Internal VIBEBANK transfer - Query Firebase
            queryVibeBankAccount(accountNumber, amount, description);
        } else {
            // External bank - Try to find account in mock database
            android.util.Log.d("ScanQR", "Querying external bank: " + bankBIN);
            
            ExternalBankSimulator.AccountInfo accountInfo = 
                simulator.findAccountName(bankBIN, accountNumber);
            
            dismissLoadingDialog();
            
            // Get bank name from BIN
            String bankName = simulator.getBankName(bankBIN);
            
            if (accountInfo != null) {
                // Account found in mock database
                android.util.Log.d("ScanQR", "Found: " + accountInfo.accountName + " at " + accountInfo.bankName);
                
                navigateToConfirmActivity(
                    bankBIN,
                    accountInfo.bankName,
                    accountNumber,
                    accountInfo.accountName,
                    amount,
                    description,
                    false // External bank
                );
            } else {
                // Account not in mock database, but allow transfer anyway
                // Generate random Vietnamese name
                android.util.Log.d("ScanQR", "Account not found in simulator, generating random name");
                
                String randomName = simulator.getRandomVietnameseName(accountNumber);
                
                navigateToConfirmActivity(
                    bankBIN,
                    bankName,
                    accountNumber,
                    randomName,
                    amount,
                    description,
                    false // External bank
                );
            }
        }
    }

    /**
     * Handle JSON QR code (internal VIBEBANK format)
     */
    private void handleJSONQR(String qrContent) {
        try {
            JSONObject qrData = new JSONObject(qrContent);

            String bankCode = qrData.optString("bankCode", "");
            String bankName = qrData.optString("bankName", "");
            String accountNumber = qrData.optString("accountNumber", "");
            String accountName = qrData.optString("accountName", "");

            android.util.Log.d("ScanQR", "JSON parsed: " + bankCode + ", " + accountNumber);

            if (accountNumber.isEmpty() || accountName.isEmpty()) {
                Toast.makeText(this, "Mã QR không hợp lệ", Toast.LENGTH_SHORT).show();
                isScanning = false;
                barcodeScanner.resume();
                return;
            }

            boolean isInternal = "VIBEBANK".equals(bankCode);
            
            navigateToConfirmActivity(
                bankCode,
                bankName,
                accountNumber,
                accountName,
                null,
                null,
                isInternal
            );

        } catch (Exception e) {
            Toast.makeText(this, "Không thể đọc mã QR", Toast.LENGTH_SHORT).show();
            isScanning = false;
            barcodeScanner.resume();
        }
    }

    /**
     * Query VIBEBANK account from Firebase
     */
    private void queryVibeBankAccount(String accountNumber, String amount, String description) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        db.collection("users")
                .whereEqualTo("account_number", accountNumber)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    dismissLoadingDialog();
                    
                    if (!querySnapshot.isEmpty()) {
                        // Get first matching document
                        String fullName = querySnapshot.getDocuments().get(0).getString("full_name");
                        
                        if (fullName == null) {
                            Toast.makeText(ScanQRActivity.this, 
                                "Không tìm thấy tên tài khoản", 
                                Toast.LENGTH_SHORT).show();
                            isScanning = false;
                            barcodeScanner.resume();
                            return;
                        }
                        
                        navigateToConfirmActivity(
                            "VIBEBANK",
                            "VIBEBANK",
                            accountNumber,
                            fullName,
                            amount,
                            description,
                            true
                        );
                    } else {
                        Toast.makeText(ScanQRActivity.this, 
                            "Không tìm thấy tài khoản VIBEBANK", 
                            Toast.LENGTH_SHORT).show();
                        isScanning = false;
                        barcodeScanner.resume();
                    }
                })
                .addOnFailureListener(e -> {
                    dismissLoadingDialog();
                    Toast.makeText(ScanQRActivity.this, 
                        "Lỗi: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                    isScanning = false;
                    barcodeScanner.resume();
                });
    }

    /**
     * Navigate to Transfer Details Activity
     */
    private void navigateToConfirmActivity(String bankCode, String bankName, 
                                          String accountNumber, String accountName,
                                          String amount, String description,
                                          boolean isInternal) {
        try {
            Intent intent = new Intent(this, TransferDetailsActivity.class);
            intent.putExtra("receiverAccountNumber", accountNumber != null ? accountNumber : "");
            intent.putExtra("receiverName", accountName != null ? accountName : "Unknown");
            intent.putExtra("bankName", bankName != null ? bankName : "VIBEBANK");
            
            // For internal VIBEBANK transfers, we need to query userId
            // For external banks, receiverUserId can be null
            if (isInternal) {
                // Query to get receiverUserId from Firestore
                queryUserIdByAccountNumber(accountNumber, intent);
            } else {
                // External bank - no userId needed, but TransferDetailsActivity requires it
                // Use a placeholder for external banks
                intent.putExtra("receiverUserId", "EXTERNAL_BANK");
                startActivity(intent);
                finish();
            }
        } catch (Exception e) {
            android.util.Log.e("ScanQR", "Navigation error: " + e.getMessage());
            isScanning = false;
            barcodeScanner.resume();
        }
    }

    /**
     * Query userId by account number for internal transfers
     */
    private void queryUserIdByAccountNumber(String accountNumber, Intent intent) {
        if (accountNumber == null || accountNumber.isEmpty()) {
            isScanning = false;
            barcodeScanner.resume();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // IMPORTANT:
        // - account_number được lưu trong collection "accounts" (xem RegisterViewModel, TransferEnterAccountActivity)
        // - ID document trong "accounts" CHÍNH LÀ userId
        // Trước đây hàm này query sang "users" + tạo userId giả "VIBEBANK_xxx" nếu không tìm thấy,
        // dẫn tới giao dịch ghi vào sai userId => người nhận không thấy số dư & lịch sử.
        //
        // Sửa lại: luôn truy vấn "accounts" theo account_number và dùng documentId làm receiverUserId.
        db.collection("accounts")
                .whereEqualTo("account_number", accountNumber)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Lấy đúng userId thật từ documentId trong "accounts"
                        String userId = querySnapshot.getDocuments().get(0).getId();
                        intent.putExtra("receiverUserId", userId);
                    } else {
                        // Không tìm thấy tài khoản nội bộ -> coi như ngân hàng ngoài,
                        // không gán receiverUserId để tránh tạo userId ảo làm lệch dữ liệu.
                        intent.putExtra("receiverUserId", "EXTERNAL_BANK");
                    }

                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Nếu lỗi khi query, fallback sang coi như ngân hàng ngoài
                    intent.putExtra("receiverUserId", "EXTERNAL_BANK");
                    startActivity(intent);
                    finish();
                });
    }

    private void openGallery() {
        galleryLauncher.launch("image/*");
    }

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    decodeQRFromImage(uri);
                }
            }
    );

    private void decodeQRFromImage(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) {
                inputStream.close();
            }

            if (bitmap != null) {
                int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
                bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

                RGBLuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
                BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

                MultiFormatReader reader = new MultiFormatReader();
                Result result = reader.decode(binaryBitmap);

                processQRCode(result.getText());
                bitmap.recycle();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Không tìm thấy mã QR trong ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoadingDialog(String message) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void dismissLoadingDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (barcodeScanner != null) {
            barcodeScanner.resume();
            isScanning = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (barcodeScanner != null) {
            barcodeScanner.pause();
        }
        dismissLoadingDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissLoadingDialog();
    }
}
