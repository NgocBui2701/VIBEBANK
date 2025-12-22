package com.example.vibebank.ui.register;

import android.content.Context;
import android.util.Log;

import androidx.annotation.CheckResult;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
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

    public MutableLiveData<String> emailError = new MutableLiveData<>();
    public MutableLiveData<String> cccdError = new MutableLiveData<>();
    public MutableLiveData<String> phoneError = new MutableLiveData<>();

    // LiveData cho UI
    public MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    public MutableLiveData<Boolean> registrationResult = new MutableLiveData<>();
    public MutableLiveData<CheckResult> finalCheckAction = new MutableLiveData<>();
    public MutableLiveData<String> toastMessage = new MutableLiveData<>();

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

    public RegisterViewModel() {
        this.dataManager = RegisterDataManager.getInstance();
    }

    // --- CÁC HÀM GET ĐỂ LOAD DỮ LIỆU LÊN REGISTER 4 ---
    // (Khớp với code Activity của bạn)
    public String getFullName() { return dataManager.getFullName(); }
    public String getBirthDate() { return dataManager.getBirthDate(); }
    public String getGender() { return dataManager.getGender(); }
    public String getCccd() { return dataManager.getCccd(); }
    public String getAddress() { return dataManager.getAddress(); }
    public String getIssueDate() { return dataManager.getIssueDate(); }
    public String getPhone() { return dataManager.getPhone(); }

    // Các getter khác nếu cần
    public String getEmail() { return dataManager.getEmail(); }


    // --- CÁC HÀM SAVE DỮ LIỆU TỪ CÁC BƯỚC TRƯỚC ---

    // Bước 1: Lưu Email, SĐT, Số CCCD (nhập tay hoặc check)
    public void saveStep1(String email, String phone, String cccd) {
        dataManager.setEmail(email);
        dataManager.setPhone(phone);
        dataManager.setCccd(cccd);
    }

    // Bước 2 hoặc OCR: Lưu thông tin cá nhân
    public void saveStep2(String fullName, String birthDate, String gender, String address, String issueDate) {
        dataManager.setFullName(fullName);
        dataManager.setBirthDate(birthDate);
        dataManager.setGender(gender);
        dataManager.setAddress(address);
        dataManager.setIssueDate(issueDate);
    }

    // Hàm này dùng cho eKYC (Module 1): Sau khi quét OCR xong thì gọi hàm này để điền tự động
    public void updateInfoFromOCR(String extractedCccd, String extractedName) {
        if (extractedCccd != null) dataManager.setCccd(extractedCccd);
        if (extractedName != null) dataManager.setFullName(extractedName);
        // Có thể thêm ngày sinh, quê quán nếu OCR đọc được
    }

    // Bước 3: Lưu ảnh
    public void saveStep3(String frontPath, String backPath) {
        dataManager.setFrontIdCardPath(frontPath);
        dataManager.setBackIdCardPath(backPath);
    }

    // Bước 5: Lưu mật khẩu
    public void saveAccountInfo(String password) {
        dataManager.setPassword(password);
        dataManager.setUsername(dataManager.getPhone());
    }

    // --- LOGIC UPLOAD & ĐĂNG KÝ (Dùng ở Register 5) ---
    // (Đoạn này giữ nguyên như tôi đã sửa ở câu trả lời trước)
    public void registerUser() {
        isLoading.setValue(true);
        Log.d("VibeBank_Register", "Bắt đầu quy trình đăng ký...");

        // 1. KIỂM TRA DỮ LIỆU ĐẦU VÀO (Quan trọng: Tránh lỗi NullPointerException)
        String email = dataManager.getEmail();
        String password = dataManager.getPassword();
        String frontPath = dataManager.getFrontIdCardPath();
        String backPath = dataManager.getBackIdCardPath();

        Log.d("VibeBank_Register", "Email: " + email);
        Log.d("VibeBank_Register", "FrontPath: " + frontPath);
        Log.d("VibeBank_Register", "BackPath: " + backPath);

        // Nếu dữ liệu bị mất do Crash hoặc Restart App -> Bắt buộc nhập lại từ đầu
        if (email == null || email.isEmpty() || frontPath == null || backPath == null) {
            isLoading.setValue(false);
            toastMessage.setValue("Dữ liệu bị lỗi hoặc phiên làm việc hết hạn. Vui lòng quay lại màn hình đăng ký từ đầu.");
            registrationResult.setValue(false);
            return;
        }

        // Nếu mật khẩu null
        if (password == null || password.isEmpty()) {
            isLoading.setValue(false);
            toastMessage.setValue("Mật khẩu không hợp lệ.");
            return;
        }

        // 2. BẮT ĐẦU UPLOAD
        Log.d("VibeBank_Register", "Dữ liệu OK. Bắt đầu upload ảnh mặt trước...");

        uploadToCloudinary(frontPath, frontUrl -> {
            Log.d("VibeBank_Register", "Upload trước thành công: " + frontUrl);
            Log.d("VibeBank_Register", "Bắt đầu upload ảnh mặt sau...");

            uploadToCloudinary(backPath, backUrl -> {
                Log.d("VibeBank_Register", "Upload sau thành công: " + backUrl);

                // 3. TẠO TÀI KHOẢN FIREBASE
                createAuthAndSaveFirestore(frontUrl, backUrl);
            });
        });
    }

    // Helper: Hàm upload ảnh lên Cloudinary
    private void uploadToCloudinary(String filePath, OnUploadSuccessListener listener) {
        MediaManager.get().upload(filePath)
                .unsigned("vibebank_upload")
                .option("folder", "ekyc_docs")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) { }
                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) { }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        listener.onSuccess(url);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo errorInfo) {
                        Log.e("VibeBank_Register", "Lỗi Upload Cloudinary: " + errorInfo.getDescription());
                        isLoading.postValue(false);
                        toastMessage.postValue("Lỗi upload ảnh: " + errorInfo.getDescription());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo errorInfo) { }
                }).dispatch();
    }

    // BƯỚC 3 & 4: TẠO USER AUTH VÀ GHI FIRESTORE
    private void createAuthAndSaveFirestore(String frontUrl, String backUrl) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        String email = dataManager.getEmail();
        String password = dataManager.getPassword();

        Log.d("VibeBank_Register", "Bắt đầu tạo Auth với Email: " + email);

        // A. Tạo tài khoản trên Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user != null) {
                        String uid = user.getUid();
                        Log.d("VibeBank_Register", "Tạo Auth thành công. UID: " + uid);
                        // B. Lưu dữ liệu chi tiết vào Firestore
                        saveDataToFirestore(uid, frontUrl, backUrl);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("VibeBank_Register", "Lỗi tạo Auth: " + e.getMessage());
                    isLoading.setValue(false);
                    registrationResult.setValue(false);
                    toastMessage.setValue("Không thể tạo tài khoản: " + e.getMessage());
                });
    }

    // BƯỚC 5: GHI BATCH VÀO FIRESTORE
    private void saveDataToFirestore(String uid, String frontUrl, String backUrl) {
        Log.d("VibeBank_Register", "Bắt đầu lưu vào Firestore...");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();

        try {
            // 1. Chuẩn bị User Map
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("full_name", dataManager.getFullName());
            userMap.put("phone_number", dataManager.getPhone());
            userMap.put("email", dataManager.getEmail());
            userMap.put("address", dataManager.getAddress());
            userMap.put("birth_date", dataManager.getBirthDate());
            userMap.put("gender", dataManager.getGender());
            userMap.put("created_at", new Date());
            userMap.put("avatar_url", "");
            userMap.put("password_hash", PasswordUtils.hashPassword(dataManager.getPassword()));
            userMap.put("role", "customer"); // Mặc định là khách hàng, admin sẽ đổi thành "staff" nếu cần

            // 2. Chuẩn bị KYC Map
            Map<String, Object> kycMap = new HashMap<>();
            kycMap.put("id_number", dataManager.getCccd());
            kycMap.put("issued_date", dataManager.getIssueDate());
            kycMap.put("issued_place", "Cục CSQLHC về TTXH");
            kycMap.put("front_image_url", frontUrl);
            kycMap.put("back_image_url", backUrl);
            kycMap.put("status", "pending");

            // 3. Chuẩn bị Account Map
            Map<String, Object> accountMap = new HashMap<>();
            accountMap.put("account_number", dataManager.getPhone());
            accountMap.put("account_type", "checking");
            accountMap.put("balance", 0);
            accountMap.put("created_at", new Date());

            // Gán vào Batch
            batch.set(db.collection("users").document(uid), userMap);
            batch.set(db.collection("kyc_documents").document(uid), kycMap);
            batch.set(db.collection("accounts").document(uid), accountMap);

            // Commit
            batch.commit()
                    .addOnSuccessListener(aVoid -> {
                        Log.d("VibeBank_Register", "Lưu Firestore THÀNH CÔNG!");
                        dataManager.clearData();
                        isLoading.setValue(false);
                        registrationResult.setValue(true);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("VibeBank_Register", "Lỗi lưu Firestore: " + e.getMessage());
                        isLoading.setValue(false);
                        registrationResult.setValue(false);
                        toastMessage.setValue("Lỗi lưu dữ liệu: " + e.getMessage());
                    });

        } catch (Exception e) {
            // Bắt lỗi null pointer khi get dữ liệu từ DataManager
            Log.e("VibeBank_Register", "Lỗi tạo data map: " + e.getMessage());
            isLoading.setValue(false);
            toastMessage.setValue("Lỗi xử lý dữ liệu: " + e.getMessage());
        }
    }

    // Interface callback
    private interface OnUploadSuccessListener {
        void onSuccess(String url);
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
}