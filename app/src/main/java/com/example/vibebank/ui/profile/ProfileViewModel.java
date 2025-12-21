package com.example.vibebank.ui.profile;

import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class ProfileViewModel extends ViewModel {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String UPLOAD_PRESET = "vibebank_upload";

    private String currentUserPasswordHash = "";

    // LiveData cho từng trường thông tin
    public MutableLiveData<String> fullName = new MutableLiveData<>();
    public MutableLiveData<String> phone = new MutableLiveData<>();
    public MutableLiveData<String> email = new MutableLiveData<>();
    public MutableLiveData<String> address = new MutableLiveData<>();
    public MutableLiveData<String> gender = new MutableLiveData<>();
    public MutableLiveData<String> birthday = new MutableLiveData<>();
    public MutableLiveData<String> avatarUrl = new MutableLiveData<>();
    // Account Info
    public MutableLiveData<String> accountNumber = new MutableLiveData<>();
    public MutableLiveData<String> accountType = new MutableLiveData<>();
    public MutableLiveData<String> openDate = new MutableLiveData<>();

    // KYC Info
    public MutableLiveData<String> cccd = new MutableLiveData<>();
    public MutableLiveData<String> issueDate = new MutableLiveData<>();

    public MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    public MutableLiveData<String> toastMessage = new MutableLiveData<>();

    public void loadUserProfile(String userId) {
        if (userId == null || userId.isEmpty()) return;
        isLoading.setValue(true);

        // 1. Lấy thông tin cơ bản từ bảng "users"
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        fullName.setValue(doc.getString("full_name"));
                        phone.setValue(doc.getString("phone_number"));
                        email.setValue(doc.getString("email"));
                        address.setValue(doc.getString("address"));
                        birthday.setValue(doc.getString("birth_date"));
                        gender.setValue(doc.getString("gender"));
                        avatarUrl.setValue(doc.getString("avatar_url"));
                        currentUserPasswordHash = doc.getString("password_hash");
                    }
                });

        // 2. Lấy thông tin tài khoản từ bảng "accounts"
        db.collection("accounts").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        accountNumber.setValue(doc.getString("account_number"));

                        // Chuyển loại tài khoản sang tiếng Việt
                        String type = doc.getString("account_type");
                        if ("checking".equals(type)) accountType.setValue("Thanh toán (Cá nhân)");
                        else accountType.setValue(type);

                        // Format ngày mở
                        Timestamp timestamp = doc.getTimestamp("created_at");
                        if (timestamp != null) {
                            openDate.setValue(formatDate(timestamp.toDate()));
                        }
                    }
                });

        // 3. Lấy thông tin giấy tờ từ bảng "kyc_documents"
        db.collection("kyc_documents").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        cccd.setValue(doc.getString("id_number"));
                        issueDate.setValue(doc.getString("issued_date"));
                    }
                    isLoading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileViewModel", "Lỗi lấy data", e);
                    isLoading.setValue(false);
                });
    }

    // Hàm cập nhật thông tin chung (Dùng cho cả Personal và Contact)
    public void updateUserProfile(String userId, Map<String, Object> updates) {
        if (userId == null) return;
        isLoading.setValue(true);

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    isLoading.setValue(false);
                    toastMessage.setValue("Cập nhật thành công!");

                    // Cập nhật lại LiveData để UI tự đổi theo (Không cần tải lại từ DB -> Tiết kiệm Read)
                    if (updates.containsKey("address")) address.setValue((String) updates.get("address"));
                    if (updates.containsKey("email")) email.setValue((String) updates.get("email"));
                    if (updates.containsKey("birth_date")) birthday.setValue((String) updates.get("birth_date"));
                    if (updates.containsKey("gender")) gender.setValue((String) updates.get("gender"));
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    toastMessage.setValue("Lỗi cập nhật: " + e.getMessage());
                });
    }

    public void uploadAvatar(Uri imageUri, String userId) {
        if (userId == null || imageUri == null) return;

        isLoading.setValue(true);

        String requestId = MediaManager.get().upload(imageUri)
                .unsigned(UPLOAD_PRESET) // Chế độ không cần mật khẩu server
                .option("folder", "profile_images") // Thư mục trên Cloudinary
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        // Bắt đầu upload
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        // Có thể cập nhật thanh progress nếu muốn
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        // Upload thành công -> Lấy link ảnh
                        String downloadUrl = (String) resultData.get("secure_url");
                        Log.d("Cloudinary", "Link ảnh: " + downloadUrl);

                        // Cập nhật link vào Firestore (vẫn dùng hàm cũ)
                        updateFirestoreAvatarUrl(userId, downloadUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        isLoading.postValue(false); // Dùng postValue vì callback chạy ở background thread
                        toastMessage.postValue("Lỗi upload");
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        // Mạng lỗi, tự động thử lại sau
                    }
                })
                .dispatch();
    }

    // Hàm phụ cập nhật Firestore
    private void updateFirestoreAvatarUrl(String userId, String downloadUrl) {
        db.collection("users").document(userId)
                .update("avatar_url", downloadUrl)
                .addOnSuccessListener(aVoid -> {
                    isLoading.setValue(false);
                    toastMessage.setValue("Đổi ảnh đại diện thành công!");
                    // Cập nhật lại LiveData để UI (nếu cần) load lại
                    avatarUrl.setValue(downloadUrl);
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    toastMessage.setValue("Lỗi lưu dữ liệu");
                });
    }

    public void checkEmailExists(String email, OnCheckEmailListener listener) {
        isLoading.setValue(true);
        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    isLoading.setValue(false);
                    if (!querySnapshot.isEmpty()) {
                        // Tìm thấy document có email này -> Đã tồn tại
                        listener.onResult(true);
                    } else {
                        // Không tìm thấy -> Chưa tồn tại (Hợp lệ)
                        listener.onResult(false);
                    }
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    toastMessage.setValue("Lỗi kiểm tra email: " + e.getMessage());
                });
    }

    public interface OnCheckEmailListener {
        void onResult(boolean isExists);
    }

    public boolean validatePassword(String inputPassword) {
        if (currentUserPasswordHash == null || currentUserPasswordHash.isEmpty()) return false;
        return com.example.vibebank.utils.PasswordUtils.checkPassword(inputPassword, currentUserPasswordHash);
    }

    private String formatDate(Date date) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date);
    }
}