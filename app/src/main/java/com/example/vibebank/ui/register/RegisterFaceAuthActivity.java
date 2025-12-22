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

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@androidx.camera.core.ExperimentalGetImage
public class RegisterFaceAuthActivity extends AppCompatActivity {

    private PreviewView viewFinder;
    private TextView txtInstruction;
    private ExecutorService cameraExecutor;
    private boolean isVerified = false; // Cờ đánh dấu đã xong chưa
    private boolean isFromStaff = false; // Được gọi từ Staff hay không
    private String savedFacePath = null; // Lưu đường dẫn ảnh đã save

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
        
        // Kiểm tra xem có được gọi từ Staff không
        isFromStaff = getIntent().getBooleanExtra("FROM_STAFF", false);

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
                        
                        // Nếu từ staff, save frame để return về
                        if (isFromStaff && savedFacePath == null) {
                            savedFacePath = saveImageFrame(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                        }
                        
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

        // Chờ 1s
        viewFinder.postDelayed(() -> {
            if (isFromStaff) {
                // Nếu từ Staff → Chụp ảnh và return về
                saveFaceImageAndReturn();
            } else {
                // Nếu từ Customer registration → Chuyển đến Register4Activity
                Intent intent = new Intent(RegisterFaceAuthActivity.this, Register4Activity.class);
                startActivity(intent);
                finish();
            }
        }, 1000);
    }
    
    private String saveImageFrame(android.media.Image mediaImage, int rotationDegrees) {
        try {
            // Convert Image to Bitmap
            android.graphics.Bitmap bitmap = mediaImageToBitmap(mediaImage);
            
            // Save to file
            File faceFile = new File(getCacheDir(), "staff_face_" + System.currentTimeMillis() + ".jpg");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(faceFile);
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            
            return faceFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e("FaceAuth", "Error saving face image", e);
            return null;
        }
    }
    
    private android.graphics.Bitmap mediaImageToBitmap(android.media.Image image) {
        android.media.Image.Plane[] planes = image.getPlanes();
        java.nio.ByteBuffer yBuffer = planes[0].getBuffer();
        java.nio.ByteBuffer uBuffer = planes[1].getBuffer();
        java.nio.ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        android.graphics.YuvImage yuvImage = new android.graphics.YuvImage(nv21, android.graphics.ImageFormat.NV21, 
                image.getWidth(), image.getHeight(), null);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        yuvImage.compressToJpeg(new android.graphics.Rect(0, 0, image.getWidth(), image.getHeight()), 90, out);
        byte[] imageBytes = out.toByteArray();
        return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }
    
    private void saveFaceImageAndReturn() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("FACE_VERIFIED", true);
        
        if (savedFacePath != null) {
            resultIntent.putExtra("FACE_IMAGE_PATH", savedFacePath);
        }
        
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}