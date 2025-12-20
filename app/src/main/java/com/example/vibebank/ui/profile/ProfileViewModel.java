package com.example.vibebank.ui.profile;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileViewModel extends ViewModel {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // LiveData cho từng trường thông tin
    public MutableLiveData<String> fullName = new MutableLiveData<>();
    public MutableLiveData<String> phone = new MutableLiveData<>();
    public MutableLiveData<String> email = new MutableLiveData<>();
    public MutableLiveData<String> address = new MutableLiveData<>();

    // Account Info
    public MutableLiveData<String> accountNumber = new MutableLiveData<>();
    public MutableLiveData<String> accountType = new MutableLiveData<>();
    public MutableLiveData<String> openDate = new MutableLiveData<>();

    // KYC Info
    public MutableLiveData<String> cccd = new MutableLiveData<>();
    public MutableLiveData<String> issueDate = new MutableLiveData<>();
    public MutableLiveData<String> gender = new MutableLiveData<>();
    public MutableLiveData<String> birthday = new MutableLiveData<>();

    public MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

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

                        // Nếu trong bảng users có lưu ngày sinh/giới tính thì lấy ở đây
                        // (Tùy thuộc vào lúc Đăng ký bạn lưu ở đâu)
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
                        issueDate.setValue(doc.getString("issue_date"));

                        String sex = doc.getString("gender");
                        gender.setValue(sex != null ? sex : "Chưa cập nhật");

                        Timestamp birthTs = doc.getTimestamp("birth_date");
                        birthday.setValue(birthTs != null ? formatDate(birthTs.toDate()) : "Chưa cập nhật");
                    }
                    isLoading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileViewModel", "Lỗi lấy data", e);
                    isLoading.setValue(false);
                });
    }

    private String formatDate(Date date) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date);
    }
}