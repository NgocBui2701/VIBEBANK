package com.example.vibebank.ui.register;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.vibebank.R;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;

public class Register3Activity extends AppCompatActivity {

    private ImageView btnBack;
    private ImageView imgFront, imgBack;
    private LinearLayout layoutPlaceholderFront, layoutPlaceholderBack;
    private MaterialButton btnCaptureFront, btnCaptureBack, btnNext;
    private RegisterViewModel viewModel;

    // Biến lưu đường dẫn ảnh tạm thời
    private String frontCardPath = null;
    private String backCardPath = null;

    // Cờ đánh dấu đang chụp mặt nào (True = Trước, False = Sau)
    private boolean isCapturingFront = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register3);

        // Khởi tạo ViewModel (Dữ liệu đã được lưu từ Register 1, 2)
        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnNext = findViewById(R.id.btnNext);

        btnCaptureFront = findViewById(R.id.btnCaptureFront);
        btnCaptureBack = findViewById(R.id.btnCaptureBack);

        imgFront = findViewById(R.id.imgFront);
        imgBack = findViewById(R.id.imgBack);

        layoutPlaceholderFront = findViewById(R.id.layoutPlaceholderFront);
        layoutPlaceholderBack = findViewById(R.id.layoutPlaceholderBack);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // Bấm chụp mặt trước
        btnCaptureFront.setOnClickListener(v -> {
            isCapturingFront = true;
            checkCameraPermissionAndOpen();
        });

        // Bấm chụp mặt sau
        btnCaptureBack.setOnClickListener(v -> {
            isCapturingFront = false;
            checkCameraPermissionAndOpen();
        });

        // Bấm Tiếp theo
        btnNext.setOnClickListener(v -> {
            if (validateImages()) {
                // 1. Lưu đường dẫn ảnh vào ViewModel (Singleton Data Manager)
                viewModel.saveStep3(frontCardPath, backCardPath);

                // 2. Chuyển sang màn hình tạo Username/Password
                Intent intent = new Intent(Register3Activity.this, Register4Activity.class);
                startActivity(intent);
            }
        });
    }

    // --- LOGIC 1: CAMERA & PERMISSION ---
    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Xin quyền Camera nếu chưa có
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
        } else {
            // Đã có quyền -> Mở Camera
            openCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Bạn cần cấp quyền Camera để chụp ảnh giấy tờ", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openCamera() {
        Intent intent = new Intent(this, CustomCameraActivity.class);
        intent.putExtra("TYPE", isCapturingFront ? "MẶT TRƯỚC" : "MẶT SAU");
        cameraLauncher.launch(intent);
    }

    // --- LOGIC 2: XỬ LÝ KẾT QUẢ CHỤP ẢNH ---
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String path = result.getData().getStringExtra("IMAGE_PATH");
                    if (path != null) {
                        Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(path);
                        try {
                            bitmap = rotateImageIfRequired(bitmap, path);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        processCapturedImage(bitmap, path);
                    }
                }
            }
    );

    private void processCapturedImage(Bitmap bitmap, String path) {
        if (isCapturingFront) {
            frontCardPath = path;
            imgFront.setImageBitmap(bitmap);
            layoutPlaceholderFront.setVisibility(View.GONE);
            imgFront.setVisibility(View.VISIBLE);
            btnCaptureFront.setText("CHỤP LẠI MẶT TRƯỚC");
        } else {
            backCardPath = path;
            imgBack.setImageBitmap(bitmap);
            layoutPlaceholderBack.setVisibility(View.GONE);
            imgBack.setVisibility(View.VISIBLE);
            btnCaptureBack.setText("CHỤP LẠI MẶT SAU");
        }
    }

    private boolean validateImages() {
        if (frontCardPath == null) {
            Toast.makeText(this, "Vui lòng chụp mặt trước CCCD", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (backCardPath == null) {
            Toast.makeText(this, "Vui lòng chụp mặt sau CCCD", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private Bitmap rotateImageIfRequired(Bitmap img, String selectedImage) throws IOException {
        android.media.ExifInterface ei = new android.media.ExifInterface(selectedImage);
        int orientation = ei.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case android.media.ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case android.media.ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case android.media.ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }

    private Bitmap rotateImage(Bitmap source, float angle) {
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
}