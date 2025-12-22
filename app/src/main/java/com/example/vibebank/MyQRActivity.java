package com.example.vibebank;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class MyQRActivity extends AppCompatActivity {

    private ImageView btnBack, ivQRCode;
    private LinearLayout btnDownload, btnShare;
    private TextView tvAccountName, tvAccountNumber;

    private String accountNumber, accountName;
    private Bitmap qrBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_qr);

        initViews();
        loadUserData();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnDownload = findViewById(R.id.btnDownload);
        btnShare = findViewById(R.id.btnShare);
        ivQRCode = findViewById(R.id.ivQRCode);
        tvAccountName = findViewById(R.id.tvAccountName);
        tvAccountNumber = findViewById(R.id.tvAccountNumber);
    }

    private void loadUserData() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Load name from users collection
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (!userDoc.exists()) {
                        Toast.makeText(MyQRActivity.this, "Không tìm thấy thông tin tài khoản!", 
                            Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    // Get name
                    accountName = userDoc.getString("full_name");
                    if (accountName == null) {
                        accountName = userDoc.getString("name");
                    }

                    // Try to get account number from users collection first
                    accountNumber = userDoc.getString("account_number");
                    if (accountNumber == null) {
                        accountNumber = userDoc.getString("accountNumber");
                    }

                    if (accountNumber != null) {
                        // Already have both, generate QR
                        displayQRCode();
                    } else {
                        // Need to get account number from accounts collection
                        loadAccountNumber(userId);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MyQRActivity.this, "Lỗi: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void loadAccountNumber(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        db.collection("accounts")
                .document(userId)
                .get()
                .addOnSuccessListener(accountDoc -> {
                    if (accountDoc.exists()) {
                        accountNumber = accountDoc.getString("account_number");
                        if (accountNumber == null) {
                            accountNumber = accountDoc.getString("accountNumber");
                        }
                    }
                    
                    if (accountNumber == null) {
                        // Generate account number based on userId
                        accountNumber = "VB" + userId.substring(0, Math.min(10, userId.length())).toUpperCase();
                    }
                    
                    displayQRCode();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MyQRActivity.this, "Lỗi load account: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void displayQRCode() {
        if (accountName == null || accountNumber == null) {
            Toast.makeText(MyQRActivity.this, 
                "Dữ liệu: name=" + accountName + ", acc=" + accountNumber, 
                Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        tvAccountName.setText(accountName);
        tvAccountNumber.setText(accountNumber);
        generateQRCode();
    }

    private void generateQRCode() {
        try {
            JSONObject qrData = new JSONObject();
            qrData.put("bankCode", "VIBEBANK");
            qrData.put("bankName", "VIBEBANK");
            qrData.put("accountNumber", accountNumber);
            qrData.put("accountName", accountName);
            qrData.put("type", "VIETQR");

            String qrContent = qrData.toString();

            // Thêm encoding hint UTF-8 để hỗ trợ tiếng Việt
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1); // Giảm margin để QR đẹp hơn

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, 512, 512, hints);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            qrBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    qrBitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

            ivQRCode.setImageBitmap(qrBitmap);

        } catch (Exception e) {
            Toast.makeText(this, "Không thể tạo mã QR", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnDownload.setOnClickListener(v -> saveQRCode());
        btnShare.setOnClickListener(v -> shareQRCode());
    }

    private void saveQRCode() {
        if (qrBitmap == null) return;

        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "VIBEBANK_QR_" + System.currentTimeMillis() + ".png");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            
            if (uri != null) {
                OutputStream outputStream = getContentResolver().openOutputStream(uri);
                qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.close();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi khi lưu mã QR", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareQRCode() {
        if (qrBitmap == null) return;

        try {
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            
            File file = new File(cachePath, "qr_code.png");
            FileOutputStream stream = new FileOutputStream(file);
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            Uri contentUri = FileProvider.getUriForFile(this, 
                "com.example.vibebank.fileprovider", file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(shareIntent, "Chia sẻ mã QR"));

        } catch (Exception e) {
            Toast.makeText(this, "Lỗi khi chia sẻ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
