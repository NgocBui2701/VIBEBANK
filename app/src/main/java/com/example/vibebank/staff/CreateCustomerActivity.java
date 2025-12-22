package com.example.vibebank.staff;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.vibebank.R;
import com.example.vibebank.ui.register.CustomCameraActivity;
import com.example.vibebank.ui.register.RegisterFaceAuthActivity;
import com.example.vibebank.utils.CloudinaryHelper;
import com.example.vibebank.utils.PasswordUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
    
    // Current capture type
    private boolean isCapturingFront = true; // true: front, false: back
    
    // Request permission launcher
    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openCustomCamera();
                } else {
                    Toast.makeText(this, "Cần quyền Camera để chụp ảnh", Toast.LENGTH_SHORT).show();
                }
            });

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
        
        // Chụp CCCD mặt trước - Dùng CustomCameraActivity (giống Register3)
        cardFrontId.setOnClickListener(v -> {
            isCapturingFront = true;
            checkCameraPermissionAndOpen();
        });
        
        // Chụp CCCD mặt sau - Dùng CustomCameraActivity (giống Register3)
        cardBackId.setOnClickListener(v -> {
            isCapturingFront = false;
            checkCameraPermissionAndOpen();
        });
        
        // Chụp khuôn mặt - Dùng RegisterFaceAuthActivity (ML Kit + liveness check)
        cardFace.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterFaceAuthActivity.class);
            intent.putExtra("FROM_STAFF", true); // Đánh dấu là staff tạo, không tự động chuyển màn
            faceAuthLauncher.launch(intent);
        });
        
        btnCreate.setOnClickListener(v -> createCustomer());
    }
    
    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            openCustomCamera();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }
    
    private void openCustomCamera() {
        Intent intent = new Intent(this, CustomCameraActivity.class);
        intent.putExtra("TYPE", isCapturingFront ? "MẶT TRƯỚC" : "MẶT SAU");
        customCameraLauncher.launch(intent);
    }
    
    // Launcher cho CCCD (camera sau)
    private final ActivityResultLauncher<Intent> customCameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String path = result.getData().getStringExtra("IMAGE_PATH");
                    if (path != null) {
                        processCapturedImage(path);
                    }
                }
            }
    );
    
    // Launcher cho xác thực khuôn mặt (dùng RegisterFaceAuthActivity với ML Kit)
    private final ActivityResultLauncher<Intent> faceAuthLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    boolean verified = result.getData().getBooleanExtra("FACE_VERIFIED", false);
                    String path = result.getData().getStringExtra("FACE_IMAGE_PATH");
                    
                    if (verified && path != null) {
                        processFaceImage(path);
                    } else if (verified) {
                        // Verified nhưng chưa có ảnh (tạm thời)
                        tvFaceStatus.setText("✓ Đã xác thực");
                        tvFaceStatus.setTextColor(getColor(R.color.success));
                        Toast.makeText(this, "Xác thực khuôn mặt thành công", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );
    
    private void processCapturedImage(String path) {
        try {
            // Xử lý xoay ảnh (giống Register3)
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            bitmap = rotateImageIfRequired(bitmap, path);
            
            if (isCapturingFront) {
                frontIdPath = path;
                imgFrontId.setImageBitmap(bitmap);
                imgFrontId.setAlpha(1.0f);
                tvFrontIdStatus.setText("✓ Đã chụp");
                tvFrontIdStatus.setTextColor(getColor(R.color.success));
            } else {
                backIdPath = path;
                imgBackId.setImageBitmap(bitmap);
                imgBackId.setAlpha(1.0f);
                tvBackIdStatus.setText("✓ Đã chụp");
                tvBackIdStatus.setTextColor(getColor(R.color.success));
            }
            
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi xử lý ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    // Xử lý xoay ảnh (copy từ Register3Activity)
    private Bitmap rotateImageIfRequired(Bitmap img, String selectedImage) throws IOException {
        ExifInterface ei = new ExifInterface(selectedImage);
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }

    private Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
    
    private void processFaceImage(String path) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            bitmap = rotateImageIfRequired(bitmap, path);
            
            facePath = path;
            imgFace.setImageBitmap(bitmap);
            imgFace.setAlpha(1.0f);
            tvFaceStatus.setText("✓ Đã chụp");
            tvFaceStatus.setTextColor(getColor(R.color.success));
            
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi xử lý ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        
        if (frontIdPath == null || backIdPath == null) {
            Toast.makeText(this, "Vui lòng chụp đầy đủ ảnh CCCD (2 mặt)", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Face là optional (vì RegisterFaceAuthActivity có thể chỉ verify không save ảnh)
        if (facePath == null) {
            Toast.makeText(this, "Đang tạo tài khoản mà chưa có ảnh khuôn mặt. Tiếp tục?", Toast.LENGTH_SHORT).show();
            // Continue anyway
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
                
                // Upload face (nếu có)
                if (facePath != null) {
                    uploadToCloudinary(facePath, url3 -> {
                        faceUrl = url3;
                        createFirebaseAccount(fullName, phone, email, address, idNumber, password);
                    });
                } else {
                    // Không có ảnh khuôn mặt, tạo tài khoản luôn
                    faceUrl = "";
                    createFirebaseAccount(fullName, phone, email, address, idNumber, password);
                }
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
