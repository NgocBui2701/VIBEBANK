package com.example.vibebank.ui.forgotpassword;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ForgotPasswordViewModel extends ViewModel {

    public MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    public MutableLiveData<String> errorMessage = new MutableLiveData<>();
    // Trả về UID nếu xác thực thông tin thành công
    public MutableLiveData<String> verifiedUid = new MutableLiveData<>();

    public void verifyUserInfo(String fullName, String cccd, String phone) {
        isLoading.setValue(true);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Tìm user theo số điện thoại trước (Vì SĐT là duy nhất)
        db.collection("users")
                .whereEqualTo("phone_number", phone)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        isLoading.setValue(false);
                        errorMessage.setValue("Số điện thoại này chưa được đăng ký!");
                    } else {
                        // 2. Lấy thông tin user và check Tên
                        DocumentSnapshot userDoc = querySnapshot.getDocuments().get(0);
                        String dbName = userDoc.getString("full_name");
                        String uid = userDoc.getId();

                        if (dbName == null || !dbName.equalsIgnoreCase(fullName)) {
                            isLoading.setValue(false);
                            errorMessage.setValue("Thông tin không trùng khớp!");
                            return;
                        }

                        // 3. Check tiếp CCCD trong bảng kyc_documents bằng UID
                        checkCccd(db, uid, cccd);
                    }
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue("Lỗi kết nối: " + e.getMessage());
                });
    }

    private void checkCccd(FirebaseFirestore db, String uid, String inputCccd) {
        db.collection("kyc_documents").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    isLoading.setValue(false);
                    if (documentSnapshot.exists()) {
                        String dbCccd = documentSnapshot.getString("id_number");
                        if (dbCccd != null && dbCccd.equals(inputCccd)) {
                            // --- TẤT CẢ KHỚP ---
                            verifiedUid.setValue(uid);
                        } else {
                            errorMessage.setValue("Thông tin không trùng khớp!");
                        }
                    } else {
                        errorMessage.setValue("Không tìm thấy dữ liệu định danh (KYC)!");
                    }
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    errorMessage.setValue("Lỗi kiểm tra CCCD: " + e.getMessage());
                });
    }
}