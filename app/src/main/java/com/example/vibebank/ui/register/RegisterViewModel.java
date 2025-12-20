package com.example.vibebank.ui.register;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.vibebank.data.manager.RegisterDataManager;
import com.example.vibebank.utils.PasswordUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class RegisterViewModel extends ViewModel {
    private final RegisterDataManager dataManager;

    // LiveData chứa thông báo lỗi (Nếu null nghĩa là hợp lệ)
    public MutableLiveData<String> emailError = new MutableLiveData<>();
    public MutableLiveData<String> cccdError = new MutableLiveData<>();
    public MutableLiveData<String> phoneError = new MutableLiveData<>();

    // LiveData để chặn nút Next khi đang check (Loading)
    public MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public MutableLiveData<Boolean> registrationResult = new MutableLiveData<>();

    public MutableLiveData<CheckResult> finalCheckAction = new MutableLiveData<>();

    public static class CheckResult {
        public boolean isSuccess;
        public String errorMsg;
        public String errorField;

        public CheckResult(boolean isSuccess, String errorMsg, String errorField) {
            this.isSuccess = isSuccess;
            this.errorMsg = errorMsg;
            this.errorField = errorField;
        }
    }

    public void performFinalCheck(String email, String phone, String cccd) {
        isLoading.setValue(true); // Báo Activity hiện loading
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // BƯỚC 1: CHECK EMAIL
        db.collection("users").whereEqualTo("email", email).get()
                .addOnSuccessListener(emailSnap -> {
                    if (!emailSnap.isEmpty()) {
                        isLoading.setValue(false);
                        finalCheckAction.setValue(new CheckResult(false, "Email này đã được sử dụng!", "EMAIL"));
                        return;
                    }

                    // BƯỚC 2: CHECK SỐ ĐIỆN THOẠI
                    db.collection("users").whereEqualTo("phone_number", phone).get()
                            .addOnSuccessListener(phoneSnap -> {
                                if (!phoneSnap.isEmpty()) {
                                    isLoading.setValue(false);
                                    finalCheckAction.setValue(new CheckResult(false, "Số điện thoại này đã được đăng ký!", "PHONE"));
                                    return;
                                }

                                // BƯỚC 3: CHECK CCCD
                                db.collection("kyc_documents").whereEqualTo("id_number", cccd).get()
                                        .addOnSuccessListener(cccdSnap -> {
                                            isLoading.setValue(false); // Tắt loading dù thành công hay thất bại
                                            if (!cccdSnap.isEmpty()) {
                                                finalCheckAction.setValue(new CheckResult(false, "Số CCCD này đã tồn tại!", "CCCD"));
                                            } else {
                                                // --- TẤT CẢ ĐỀU SẠCH ---
                                                finalCheckAction.setValue(new CheckResult(true, null, null));
                                            }
                                        })
                                        .addOnFailureListener(e -> handleCheckError("Lỗi kiểm tra CCCD"));
                            })
                            .addOnFailureListener(e -> handleCheckError("Lỗi kiểm tra SĐT"));
                })
                .addOnFailureListener(e -> handleCheckError("Lỗi kiểm tra Email"));
    }

    private void handleCheckError(String msg) {
        isLoading.setValue(false);
        // Lỗi mạng thì cho phép đi tiếp hoặc báo lỗi tùy bạn, ở đây mình báo lỗi
        finalCheckAction.setValue(new CheckResult(false, msg, "GENERAL"));
    }

    public RegisterViewModel() {
        this.dataManager = RegisterDataManager.getInstance();
    }

    public void saveStep1(String email, String phone, String cccd) {
        dataManager.setEmail(email);
        dataManager.setPhone(phone);
        dataManager.setCccd(cccd);
    }

    public void checkEmailExistence(String email) {
        if (email.isEmpty()) return;

        isLoading.setValue(true);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    isLoading.setValue(false);
                    if (!querySnapshot.isEmpty()) {
                        // Tìm thấy -> Báo lỗi
                        emailError.setValue("Email này đã được đăng ký!");
                    } else {
                        // Không tìm thấy -> Hợp lệ (null)
                        emailError.setValue(null);
                    }
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    // Lỗi mạng thì tạm thời bỏ qua hoặc báo lỗi chung
                    emailError.setValue(null);
                });
    }

    public void checkCccdExistence(String cccd) {
        if (cccd.isEmpty()) return;

        isLoading.setValue(true);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("kyc_documents")
                .whereEqualTo("id_number", cccd)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    isLoading.setValue(false);
                    if (!querySnapshot.isEmpty()) {
                        cccdError.setValue("Số CCCD này đã tồn tại trong hệ thống!");
                    } else {
                        cccdError.setValue(null);
                    }
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    cccdError.setValue(null);
                });
    }

    public void checkPhoneExistence(String phone) {
        if (phone.isEmpty()) return;
        isLoading.setValue(true);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .whereEqualTo("phone_number", phone)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // isLoading.setValue(false); // Khoan hãy tắt loading ở đây để dùng cho chuỗi chain
                    if (!querySnapshot.isEmpty()) {
                        phoneError.setValue("Số điện thoại này đã được đăng ký!");
                        isLoading.setValue(false); // Tìm thấy lỗi thì tắt loading
                    } else {
                        phoneError.setValue(null); // Không trùng -> OK
                        // Lưu ý: Không tắt loading ở đây để Activity xử lý tiếp bước sau
                    }
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    phoneError.setValue(null); // Lỗi mạng tạm thời cho qua
                });
    }
    public void saveStep2(String fullName, String birthDate, String gender, String address, String issueDate) {
        dataManager.setFullName(fullName);
        dataManager.setBirthDate(birthDate);
        dataManager.setGender(gender);
        dataManager.setAddress(address);
        dataManager.setIssueDate(issueDate);
    }

    public void saveStep3(String frontPath, String backPath) {
        dataManager.setFrontIdCardPath(frontPath);
        dataManager.setBackIdCardPath(backPath);
    }
//
//    public String getFrontImage() { return dataManager.getFrontIdCardPath(); }
//    public String getBackImage() { return dataManager.getBackIdCardPath(); }

    public String getFullName() { return dataManager.getFullName(); }
    public String getBirthDate() { return dataManager.getBirthDate(); }
    public String getGender() { return dataManager.getGender(); }
    public String getCccd() { return dataManager.getCccd(); }
    public String getAddress() { return dataManager.getAddress(); }
    public String getIssueDate() { return dataManager.getIssueDate(); }
    public String getPhone() { return dataManager.getPhone(); }

    public void saveAccountInfo(String password) {
        dataManager.setPassword(password);
        dataManager.setUsername(dataManager.getPhone());
    }

    public void registerUser() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            // Chưa đăng nhập (chưa verify OTP xong)
            registrationResult.postValue(false);
            return;
        }

        String uid = currentUser.getUid();
        // --- 1. CHUẨN BỊ DỮ LIỆU (Mapping từ DataManager sang Map) ---

        // A. Data cho Collection "users"
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("full_name", dataManager.getFullName());
        userMap.put("phone_number", dataManager.getPhone());
        userMap.put("email", dataManager.getEmail());
        userMap.put("address", dataManager.getAddress());
        userMap.put("created_at", new Date());
        String rawPassword = dataManager.getPassword();
        String hashedPassword = PasswordUtils.hashPassword(rawPassword);
        userMap.put("password_hash", hashedPassword);

        // B. Data cho Collection "kyc_documents"
        Map<String, Object> kycMap = new HashMap<>();
        kycMap.put("id_number", dataManager.getCccd());
        kycMap.put("issued_date", dataManager.getIssueDate());
        kycMap.put("issued_place", "Cục CSQLHC về TTXH");
        kycMap.put("front_image_url", dataManager.getFrontIdCardPath()); // Sau này cần upload ảnh lên Storage lấy link
        kycMap.put("back_image_url", dataManager.getBackIdCardPath());

        // C. Data cho Collection "accounts"
        Map<String, Object> accountMap = new HashMap<>();
        accountMap.put("account_number", dataManager.getPhone()); // Dùng SĐT làm số tài khoản
        accountMap.put("account_type", "checking");
        accountMap.put("balance", 0); // Số dư ban đầu
        accountMap.put("created_at", new Date());

        // --- 2. THỰC HIỆN GHI BATCH (Ghi 1 lần cả 3 bảng) ---
        WriteBatch batch = db.batch();

        // Định nghĩa đường dẫn lưu trữ
        // users/{uid}
        batch.set(db.collection("users").document(uid), userMap);

        // kyc_documents/{uid} (Dùng UID làm ID luôn cho dễ truy xuất 1-1)
        batch.set(db.collection("kyc_documents").document(uid), kycMap);

        // accounts/{uid}
        batch.set(db.collection("accounts").document(uid), accountMap);

        // --- 3. CAM KẾT GHI (COMMIT) ---
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firebase", "Đăng ký thành công toàn bộ dữ liệu!");
                    dataManager.clearData(); // Xóa dữ liệu tạm
                    registrationResult.postValue(true);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "Lỗi ghi database: " + e.getMessage());
                    registrationResult.postValue(false);
                });
    }
}