package com.example.vibebank.ui.home;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.vibebank.utils.SessionManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.NumberFormat;
import java.util.Locale;

public class HomeViewModel extends ViewModel {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration balanceListener;

    // LiveData chứa dữ liệu để UI quan sát
    public MutableLiveData<String> userName = new MutableLiveData<>();
    public MutableLiveData<Double> balanceValue = new MutableLiveData<>();
    public MutableLiveData<String> balanceFormatted = new MutableLiveData<>();
    public MutableLiveData<String> avatarUrl = new MutableLiveData<>();


    // Load thông tin người dùng (Tên, Avatar) từ session
    public void loadUserProfile(Context context) {
        SessionManager sessionManager = new SessionManager(context);
        
        // Lấy từ session trước
        String fullName = sessionManager.getUserFullName();
        String avatar = sessionManager.getAvatarUrl();
        
        if (fullName != null && !fullName.isEmpty()) {
            userName.setValue(fullName.toUpperCase());
        }
        
        if (avatar != null && !avatar.isEmpty()) {
            avatarUrl.setValue(avatar);
        }
        
        // Nếu chưa có avatar trong session, thử lấy từ Firestore (fallback)
        if ((avatar == null || avatar.isEmpty()) || (fullName == null || fullName.isEmpty() || fullName.equals("Quý khách"))) {
            String userId = sessionManager.getCurrentUserId();
            if (userId != null && !userId.isEmpty()) {
                loadUserProfileFromFirestore(userId, sessionManager);
            }
        }
    }
    
    // Fallback: Load từ Firestore nếu chưa có trong session
    private void loadUserProfileFromFirestore(String userId, SessionManager sessionManager) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("full_name");
                        if (fullName != null && !fullName.isEmpty()) {
                            userName.setValue(fullName.toUpperCase());
                            sessionManager.saveUserFullName(fullName);
                        }
                        String avatarUrl = documentSnapshot.getString("avatar_url");
                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            this.avatarUrl.setValue(avatarUrl);
                            sessionManager.saveAvatarUrl(avatarUrl);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("HomeViewModel", "Lỗi lấy user profile", e));
    }

    // Load số dư (Lắng nghe Realtime)
    public void startListeningBalance(String userId) {
        if (userId == null || userId.isEmpty()) return;

        // Lắng nghe thay đổi ở collection "accounts"
        balanceListener = db.collection("accounts").document(userId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.e("HomeViewModel", "Lỗi lắng nghe số dư", e);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        // Lấy số dư (cần xử lý cẩn thận kiểu dữ liệu int/long/double)
                        Number rawBalance = documentSnapshot.getDouble("balance");
                        double balance = (rawBalance != null) ? rawBalance.doubleValue() : 0.0;

                        // Cập nhật giá trị gốc
                        balanceValue.setValue(balance);

                        // Cập nhật giá trị đã format (VD: 1.000.000 VND)
                        balanceFormatted.setValue(formatCurrency(balance));
                    }
                });
    }

    // Hàm format tiền tệ Việt Nam
    private String formatCurrency(double amount) {
        Locale localeVN = new Locale("vi", "VN");
        NumberFormat currencyVN = NumberFormat.getCurrencyInstance(localeVN);
        String formatted = currencyVN.format(amount);
        // Mặc định nó sẽ ra "1.000.000 đ", ta đổi thành " VND" cho đẹp nếu muốn
        return formatted.replace("VND", "").trim();
    }

    public String getAvatarUrl() {
        return avatarUrl.getValue();
    }

    // Hủy lắng nghe khi thoát màn hình để tránh rò rỉ bộ nhớ
    @Override
    protected void onCleared() {
        super.onCleared();
        if (balanceListener != null) {
            balanceListener.remove();
        }
    }
}