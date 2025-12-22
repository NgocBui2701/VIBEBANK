package com.example.vibebank.ui.register;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.vibebank.R;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomCameraActivity extends AppCompatActivity {
    private PreviewView viewFinder;
    private ImageButton btnCapture;
    private TextView tvInstruction;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private boolean useFrontCamera = false; // Mặc định dùng camera sau

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_camera);

        viewFinder = findViewById(R.id.viewFinder);
        btnCapture = findViewById(R.id.btnCapture);
        tvInstruction = findViewById(R.id.tvInstruction);

        // Lấy hướng dẫn từ Intent
        String type = getIntent().getStringExtra("TYPE");
        useFrontCamera = getIntent().getBooleanExtra("USE_FRONT_CAMERA", false);
        
        if(type != null) {
            if (type.contains("KHUÔN MẶT")) {
                tvInstruction.setText("Chụp " + type);
            } else {
                tvInstruction.setText("Chụp " + type + " CCCD");
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor();
        startCamera();

        btnCapture.setOnClickListener(v -> takePhoto());
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Cấu hình Preview (Xem trước)
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                // Cấu hình Chụp ảnh
                imageCapture = new ImageCapture.Builder().build();

                // Chọn Camera (trước hoặc sau tùy intent)
                CameraSelector cameraSelector = useFrontCamera ? 
                        CameraSelector.DEFAULT_FRONT_CAMERA : 
                        CameraSelector.DEFAULT_BACK_CAMERA;

                try {
                    cameraProvider.unbindAll();
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
                } catch (Exception exc) {
                    Log.e("CameraX", "Use case binding failed", exc);
                }

            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraX", "Error starting camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        // Tạo file tạm
        File photoFile = new File(getCacheDir(), "ekyc_" + System.currentTimeMillis() + ".jpg");

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("IMAGE_PATH", photoFile.getAbsolutePath());
                setResult(RESULT_OK, resultIntent);
                finish();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Toast.makeText(CustomCameraActivity.this, "Lỗi chụp ảnh: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}