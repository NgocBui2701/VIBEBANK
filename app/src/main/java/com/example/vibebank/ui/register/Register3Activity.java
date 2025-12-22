package com.example.vibebank.ui.register;

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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
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

    // Thêm TextView để hiển thị trạng thái đã chụp hay chưa (UX)
    private TextView txtStatusFront, txtStatusBack;

    private RegisterViewModel viewModel;

    // Biến lưu đường dẫn ảnh
    private String frontCardPath = null;
    private String backCardPath = null;
    private boolean isCapturingFront = true;

    // Launcher xin quyền Camera (Cách mới)
    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openCamera();
                } else {
                    Toast.makeText(this, "Bạn cần cấp quyền Camera để chụp ảnh giấy tờ", Toast.LENGTH_SHORT).show();
                }
            });

    // Launcher nhận kết quả từ Camera
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    // Giả sử CustomCameraActivity trả về key "IMAGE_PATH"
                    String path = result.getData().getStringExtra("IMAGE_PATH");
                    if (path != null) {
                        processCapturedImage(path);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register3);

        // DataManager là Singleton nên ViewModel sẽ tự lấy lại được dữ liệu cũ
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

        // Nếu layout có TextView trạng thái thì findView, không thì bỏ qua
        // txtStatusFront = findViewById(R.id.txtStatusFront);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnCaptureFront.setOnClickListener(v -> {
            isCapturingFront = true;
            checkCameraPermissionAndOpen();
        });

        btnCaptureBack.setOnClickListener(v -> {
            isCapturingFront = false;
            checkCameraPermissionAndOpen();
        });

        btnNext.setOnClickListener(v -> {
            if (validateImages()) {
                // 1. Lưu đường dẫn vào Singleton Data Manager
                viewModel.saveStep3(frontCardPath, backCardPath);

                // 2. Chuyển sang bước tiếp theo
                Intent intent = new Intent(Register3Activity.this, RegisterFaceAuthActivity.class);
                startActivity(intent);
            }
        });
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        // Mở Activity Camera tùy chỉnh của bạn
        // Nếu chưa có CustomCameraActivity, hãy tạo nó hoặc dùng Intent mặc định MediaStore
        Intent intent = new Intent(this, CustomCameraActivity.class);
        intent.putExtra("TYPE", isCapturingFront ? "MẶT TRƯỚC CCCD" : "MẶT SAU CCCD");
        cameraLauncher.launch(intent);
    }

    private void processCapturedImage(String path) {
        // Xử lý xoay ảnh và hiển thị
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        try {
            bitmap = rotateImageIfRequired(bitmap, path);
        } catch (IOException e) {
            e.printStackTrace();
        }

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

    // --- XỬ LÝ XOAY ẢNH (FIX LỖI SAMSUNG/XIAOMI) ---
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
}