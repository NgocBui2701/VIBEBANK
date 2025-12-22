package com.example.vibebank.ui.register;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.vibebank.R;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@androidx.camera.core.ExperimentalGetImage
public class RegisterFaceAuthActivity extends AppCompatActivity {

    private PreviewView viewFinder;
    private TextView txtInstruction;
    private ExecutorService cameraExecutor;
    private boolean isVerified = false; // Cờ đánh dấu đã xong chưa

    // Xin quyền Camera
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(this, "Cần quyền Camera để xác thực", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_face_auth);

        viewFinder = findViewById(R.id.viewFinder);
        txtInstruction = findViewById(R.id.txtInstruction);
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    @androidx.camera.core.ExperimentalGetImage
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // 1. Preview
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                // 2. Image Analysis (Xử lý AI)
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    if (isVerified) {
                        imageProxy.close();
                        return;
                    }

                    @androidx.camera.core.ExperimentalGetImage
                    android.media.Image mediaImage = imageProxy.getImage();

                    if (mediaImage != null) {
                        InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                        detectFaces(image, imageProxy);
                    } else {
                        imageProxy.close();
                    }
                });

                // 3. Chọn Camera trước
                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                Log.e("FaceAuth", "Lỗi mở camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void detectFaces(InputImage image, androidx.camera.core.ImageProxy imageProxy) {
        // Cấu hình phát hiện nụ cười và mở mắt
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // Quan trọng để check cười
                .build();

        FaceDetector detector = FaceDetection.getClient(options);

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (faces.isEmpty()) {
                        updateInstruction("Không tìm thấy khuôn mặt", false);
                    } else {
                        Face face = faces.get(0);
                        checkLiveness(face);
                    }
                })
                .addOnFailureListener(e -> Log.e("FaceAuth", "Lỗi AI", e))
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void checkLiveness(Face face) {
        // Lấy xác suất cười (0.0 -> 1.0)
        Float smileProb = face.getSmilingProbability();

        // Lấy xác suất mở mắt
        Float leftEye = face.getLeftEyeOpenProbability();
        Float rightEye = face.getRightEyeOpenProbability();

        if (smileProb != null && smileProb > 0.6) {
            // Nếu đang cười -> Xác thực thành công
            onSuccess();
        } else if (leftEye != null && rightEye != null && (leftEye < 0.1 || rightEye < 0.1)) {
            // Mắt nhắm
            updateInstruction("Vui lòng mở to mắt", false);
        } else {
            // Mặt bình thường -> Yêu cầu cười
            updateInstruction("Vui lòng CƯỜI để xác thực", true);
        }
    }

    private void updateInstruction(String text, boolean isWarning) {
        runOnUiThread(() -> {
            txtInstruction.setText(text);
            txtInstruction.setTextColor(isWarning ?
                    ContextCompat.getColor(this, android.R.color.holo_red_light) :
                    ContextCompat.getColor(this, android.R.color.white));
        });
    }

    private void onSuccess() {
        if (isVerified) return;
        isVerified = true;

        runOnUiThread(() -> {
            txtInstruction.setText("Xác thực thành công!");
            txtInstruction.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light));
        });

        // Chờ 1s rồi chuyển màn hình
        viewFinder.postDelayed(() -> {
            // Chuyển đến Register4Activity (Xem lại thông tin)
            Intent intent = new Intent(RegisterFaceAuthActivity.this, Register4Activity.class);
            startActivity(intent);
            finish();
        }, 1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}