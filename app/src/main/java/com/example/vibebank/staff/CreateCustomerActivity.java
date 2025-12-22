package com.example.vibebank.staff;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.vibebank.R;
import com.example.vibebank.utils.CloudinaryHelper;
import com.example.vibebank.utils.PasswordUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CreateCustomerActivity extends AppCompatActivity {

    private TextInputEditText edtFullName, edtPhone, edtEmail, edtAddress, edtIdNumber;
    private MaterialButton btnCreate;
    private ImageView btnBack, imgFrontId, imgBackId, imgFace;
    private CardView cardFrontId, cardBackId, cardFace;
    private TextView tvFrontIdStatus, tvBackIdStatus, tvFaceStatus;
    private ProgressBar progressBar;
    
    private FirebaseFirestore db;
    
    // Image paths
    private String frontIdPath, backIdPath, facePath;
    private String frontIdUrl, backIdUrl, faceUrl;
    
    // Upload type
    private String currentUploadType = ""; // "front", "back", "face"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_customer);

        db = FirebaseFirestore.getInstance();
        CloudinaryHelper.initCloudinary(this);

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        edtFullName = findViewById(R.id.edtFullName);
        edtPhone = findViewById(R.id.edtPhone);
        edtEmail = findViewById(R.id.edtEmail);
        edtAddress = findViewById(R.id.edtAddress);
        edtIdNumber = findViewById(R.id.edtIdNumber);
        btnCreate = findViewById(R.id.btnCreate);
        progressBar = findViewById(R.id.progressBar);
        
        cardFrontId = findViewById(R.id.cardFrontId);
        cardBackId = findViewById(R.id.cardBackId);
        cardFace = findViewById(R.id.cardFace);
        
        imgFrontId = findViewById(R.id.imgFrontId);
        imgBackId = findViewById(R.id.imgBackId);
        imgFace = findViewById(R.id.imgFace);
        
        tvFrontIdStatus = findViewById(R.id.tvFrontIdStatus);
        tvBackIdStatus = findViewById(R.id.tvBackIdStatus);
        tvFaceStatus = findViewById(R.id.tvFaceStatus);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        cardFrontId.setOnClickListener(v -> {
            currentUploadType = "front";
            cameraLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
        });
        
        cardBackId.setOnClickListener(v -> {
            currentUploadType = "back";
            cameraLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
        });
        
        cardFace.setOnClickListener(v -> {
            currentUploadType = "face";
            cameraLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
        });
        
        btnCreate.setOnClickListener(v -> createCustomer());
    }
    
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        if (imageBitmap != null) {
                            handleCapturedImage(imageBitmap);
                        }
                    }
                }
            }
    );
    
    private void handleCapturedImage(Bitmap bitmap) {
        try {
            // Save bitmap to temp file
            File tempFile = new File(getCacheDir(), currentUploadType + "_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            
            String filePath = tempFile.getAbsolutePath();
            
            // Update UI
            switch (currentUploadType) {
                case "front":
                    frontIdPath = filePath;
                    imgFrontId.setImageBitmap(bitmap);
                    imgFrontId.setAlpha(1.0f);
                    tvFrontIdStatus.setText("✓ Đã chụp");
                    tvFrontIdStatus.setTextColor(getColor(R.color.success));
                    break;
                case "back":
                    backIdPath = filePath;
                    imgBackId.setImageBitmap(bitmap);
                    imgBackId.setAlpha(1.0f);
                    tvBackIdStatus.setText("✓ Đã chụp");
                    tvBackIdStatus.setTextColor(getColor(R.color.success));
                    break;
                case "face":
                    facePath = filePath;
                    imgFace.setImageBitmap(bitmap);
                    imgFace.setAlpha(1.0f);
                    tvFaceStatus.setText("✓ Đã chụp");
                    tvFaceStatus.setTextColor(getColor(R.color.success));
                    break;
            }
            
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi lưu ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void createCustomer() {
        String fullName = edtFullName.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String address = edtAddress.getText().toString().trim();
        String idNumber = edtIdNumber.getText().toString().trim();

        // Validate
        if (fullName.isEmpty() || phone.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin bắt buộc (*)", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (frontIdPath == null || backIdPath == null || facePath == null) {
            Toast.makeText(this, "Vui lòng chụp đầy đủ ảnh CCCD và khuôn mặt", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate default password
        String defaultPassword = "Vibebank@" + phone.substring(Math.max(0, phone.length() - 4));

        // Show loading
        progressBar.setVisibility(View.VISIBLE);
        btnCreate.setEnabled(false);

        // Upload images first
        uploadAllImages(fullName, phone, email, address, idNumber, defaultPassword);
    }
    
    private void uploadAllImages(String fullName, String phone, String email, 
                                 String address, String idNumber, String password) {
        // Upload front ID
        uploadToCloudinary(frontIdPath, url -> {
            frontIdUrl = url;
            
            // Upload back ID
            uploadToCloudinary(backIdPath, url2 -> {
                backIdUrl = url2;
                
                // Upload face
                uploadToCloudinary(facePath, url3 -> {
                    faceUrl = url3;
                    
                    // All images uploaded, create account
                    createFirebaseAccount(fullName, phone, email, address, idNumber, password);
                });
            });
        });
    }
    
    private void uploadToCloudinary(String filePath, OnUploadSuccessListener listener) {
        MediaManager.get().upload(filePath)
                .unsigned("vibebank_upload")
                .option("folder", "ekyc_docs")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}
                    
                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        listener.onSuccess(url);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo errorInfo) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            btnCreate.setEnabled(true);
                            Toast.makeText(CreateCustomerActivity.this, 
                                "Lỗi upload ảnh: " + errorInfo.getDescription(), 
                                Toast.LENGTH_LONG).show();
                        });
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo errorInfo) {}
                }).dispatch();
    }
    
    private void createFirebaseAccount(String fullName, String phone, String email, 
                                       String address, String idNumber, String password) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        
        // Create Firebase Auth account
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    saveCustomerData(uid, fullName, phone, email, address, idNumber, password);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnCreate.setEnabled(true);
                    Toast.makeText(this, "Lỗi tạo tài khoản: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                });
    }

    private void saveCustomerData(String uid, String fullName, String phone, String email, 
                                  String address, String idNumber, String password) {
        WriteBatch batch = db.batch();

        // User document
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("full_name", fullName);
        userMap.put("phone_number", phone);
        userMap.put("email", email);
        userMap.put("address", address);
        userMap.put("created_at", new Date());
        userMap.put("avatar_url", faceUrl); // Use face image as avatar
        userMap.put("role", "customer");
        userMap.put("password_hash", PasswordUtils.hashPassword(password));

        // Account document
        Map<String, Object> accountMap = new HashMap<>();
        accountMap.put("account_number", phone);
        accountMap.put("account_type", "checking");
        accountMap.put("balance", 0);
        accountMap.put("created_at", new Date());

        // KYC document with images
        Map<String, Object> kycMap = new HashMap<>();
        kycMap.put("id_number", idNumber);
        kycMap.put("front_image_url", frontIdUrl);
        kycMap.put("back_image_url", backIdUrl);
        kycMap.put("face_image_url", faceUrl);
        kycMap.put("status", "verified"); // Staff tạo → tự động verify
        kycMap.put("created_at", new Date());
        kycMap.put("verified_by", "staff");

        batch.set(db.collection("users").document(uid), userMap);
        batch.set(db.collection("accounts").document(uid), accountMap);
        batch.set(db.collection("kyc_documents").document(uid), kycMap);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    btnCreate.setEnabled(true);
                    
                    new android.app.AlertDialog.Builder(this)
                            .setTitle("Thành công")
                            .setMessage("Tạo tài khoản thành công!\n\n" +
                                    "Email: " + email + "\n" +
                                    "Mật khẩu: " + password + "\n\n" +
                                    "Vui lòng ghi lại và cung cấp cho khách hàng.")
                            .setCancelable(false)
                            .setPositiveButton("Đóng", (d, w) -> finish())
                            .show();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnCreate.setEnabled(true);
                    Toast.makeText(this, "Lỗi lưu dữ liệu: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                });
    }
    
    private interface OnUploadSuccessListener {
        void onSuccess(String url);
    }
}
