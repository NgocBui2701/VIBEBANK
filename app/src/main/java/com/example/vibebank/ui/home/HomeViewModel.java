package com.example.vibebank.ui.home;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

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


    // Load thông tin người dùng (Tên, Avatar)
    public void loadUserProfile(String userId) {
        if (userId == null || userId.isEmpty()) return;

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("full_name");
                        if (fullName != null) {
                            userName.setValue(fullName.toUpperCase());
                        }
                        String avatarUrl = documentSnapshot.getString("avatar_url");
                        if (avatarUrl != null) {
                            this.avatarUrl.setValue(avatarUrl);
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