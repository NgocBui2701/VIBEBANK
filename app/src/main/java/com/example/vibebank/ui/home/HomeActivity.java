package com.example.vibebank.ui.home;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.vibebank.AccountManagementActivity;
import com.example.vibebank.ElectricBillActivity;
import com.example.vibebank.WaterBillActivity;
import com.example.vibebank.TopupActivity;
import com.example.vibebank.FlightTicketActivity;
import com.example.vibebank.MyQRActivity;
import com.example.vibebank.NotificationsActivity;
import com.example.vibebank.ScanQRActivity;
import com.example.vibebank.ui.profile.ProfileActivity;
import com.example.vibebank.R;
import com.example.vibebank.TransactionHistoryActivity;
import com.example.vibebank.TransferActivity;
import com.example.vibebank.WithdrawCodeActivity;
import com.example.vibebank.utils.SessionManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "HomeActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    private TextView txtUserName;
    private TextView txtBalance;
    private ImageView btnToggleBalance;
    private ImageView btnNotification;
    private ImageView btnMenu;

    // Quick action buttons
    private LinearLayout btnQR;

    // Custom bottom nav
    private LinearLayout navHome, navHistory, navQR, navTransfer, navSupport;
    private TextView txtHome, txtHistory, txtTransfer, txtSupport;

    // Google Maps
    private MapView mapView;
    private GoogleMap googleMap;
    private ScrollView scrollView;

    private boolean isBalanceVisible = false;
    private String currentBalanceString = "0";

    private HomeViewModel viewModel;
    private SessionManager sessionManager;
    private ListenerRegistration notificationListener;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        db = FirebaseFirestore.getInstance();

        // Xin quyền
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // Kiểm tra Session
        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            // Chưa đăng nhập thì đá về Login
            sessionManager.logout();
            finish();
            return;
        }

        setContentView(R.layout.activity_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.home), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewModel = new androidx.lifecycle.ViewModelProvider(this).get(HomeViewModel.class);

        initViews();
        setupMap(savedInstanceState);
        setupBottomNav();
        setupFunctionButtons();
        setupListeners();

        setupData();

        // Bắt đầu lắng nghe thông báo
        listenForNotifications();
    }

    private void listenForNotifications() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) return;

        // Lắng nghe collection "notifications"
        // Chỉ lấy những thông báo của user này, chưa đọc, và sắp xếp theo thời gian mới nhất
        notificationListener = db.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("isRead", false)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            // Chỉ xử lý khi có bản ghi ĐƯỢC THÊM MỚI (ADDED)
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                String title = dc.getDocument().getString("title");
                                String message = dc.getDocument().getString("message");

                                // 1. Hiển thị thông báo lên thanh trạng thái Android
                                showSystemNotification(title, message);

                                // 2. Đánh dấu là đã đọc (để không hiện lại lần sau)
                                db.collection("notifications")
                                        .document(dc.getDocument().getId())
                                        .update("isRead", true);
                            }
                        }
                    }
                });
    }

    // Hàm hiển thị thông báo hệ thống (Notification Bar)
    private void showSystemNotification(String title, String message) {
        String channelId = "balance_fluctuation_channel";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Tạo Channel cho Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Biến động số dư",
                    NotificationManager.IMPORTANCE_HIGH // Quan trọng cao để hiện popup
            );
            channel.setDescription("Thông báo khi nhận tiền");
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification) // Đảm bảo bạn có icon này trong res/drawable (hoặc dùng ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // Tạo ID ngẫu nhiên để các thông báo không đè lên nhau
        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, builder.build());
    }

    private void initViews() {
        // Initialize views
        txtUserName = findViewById(R.id.txtUserName);
        txtBalance = findViewById(R.id.txtBalance);
        btnToggleBalance = findViewById(R.id.btnToggleBalance);
        btnNotification = findViewById(R.id.btnNotification);
        btnMenu = findViewById(R.id.btnMenu);
        scrollView = findViewById(R.id.scrollView);

        // Initialize quick actions
        btnQR = findViewById(R.id.btnQR);

        // Initialize bottom nav
        navHome = findViewById(R.id.navHome);
        navHistory = findViewById(R.id.navHistory);
        navQR = findViewById(R.id.navQR);
        navTransfer = findViewById(R.id.navTransfer);
        navSupport = findViewById(R.id.navSupport);

        txtHome = findViewById(R.id.txtHome);
        txtHistory = findViewById(R.id.txtHistory);
        txtTransfer = findViewById(R.id.txtTransfer);
        txtSupport = findViewById(R.id.txtSupport);
    }

    private void setupBottomNav() {
        navHome.setOnClickListener(v -> selectNavItem(0));
        navHistory.setOnClickListener(v -> selectNavItem(1));
        navQR.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(HomeActivity.this, ScanQRActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(HomeActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        });
        navTransfer.setOnClickListener(v -> selectNavItem(3));
        navSupport.setOnClickListener(v -> selectNavItem(4));

        // Set home as selected by default
        selectNavItem(0);

        // Check location permission
        checkLocationPermission();
    }

    private void setupListeners() {
        // Toggle balance visibility
        btnToggleBalance.setOnClickListener(v -> toggleBalanceVisibility());

        // QR Code button
        if (btnQR != null) {
            btnQR.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(HomeActivity.this, MyQRActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(HomeActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            });
        } else {
            Toast.makeText(this, "Không tìm thấy nút QR", Toast.LENGTH_SHORT).show();
        }

        // Notification button
        btnNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, NotificationsActivity.class);
                startActivity(intent);
            }
        });

        // Menu button
        btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(HomeActivity.this, "Menu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupData() {
        String userId = sessionManager.getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            sessionManager.logout();
            return;
        }

        String fullName = sessionManager.getUserFullName();
        txtUserName.setText(fullName);

        // 1. Load tên người dùng
        viewModel.loadUserProfile(userId);

        // Lắng nghe thay đổi tên từ ViewModel
        viewModel.userName.observe(this, name -> {
            if (name != null) txtUserName.setText(name);
            sessionManager.saveUserFullName(name);
        });

        // 2. Load và lắng nghe Số dư
        viewModel.startListeningBalance(userId);
        viewModel.balanceFormatted.observe(this, formattedBalance -> {
            // Lưu giá trị mới
            this.currentBalanceString = formattedBalance;
            // Nếu đang mở mắt (hiện tiền) thì cập nhật UI ngay
            if (isBalanceVisible) {
                txtBalance.setText(currentBalanceString + " VND");
            }
        });
    }

    private void toggleBalanceVisibility() {
        isBalanceVisible = !isBalanceVisible;
        if (isBalanceVisible) {
            // Hiện tiền
            txtBalance.setText(currentBalanceString + " VND");
            btnToggleBalance.setImageResource(R.drawable.ic_eye_off); // Icon mắt mở/đóng tùy resource bạn có
        } else {
            // Ẩn tiền
            txtBalance.setText("********* VND");
            btnToggleBalance.setImageResource(R.drawable.ic_eye);
        }
    }

    // --- CÁC PHẦN CẤU HÌNH MAP & NAV GIỮ NGUYÊN ---

    private void setupMap(Bundle savedInstanceState) {
        mapView = findViewById(R.id.mapView);
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);

        // Fix lỗi cuộn trang khi chạm vào map
        mapView.setOnTouchListener((v, event) -> {
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    scrollView.requestDisallowInterceptTouchEvent(true);
                    return false;
                case MotionEvent.ACTION_UP:
                    scrollView.requestDisallowInterceptTouchEvent(false);
                    return true;
                default:
                    return true;
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;

        // Set map type
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Enable location if permission granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }

        // Tọa độ Thành phố Hồ Chí Minh (trung tâm)
        LatLng hoChiMinhCity = new LatLng(10.7769, 106.7009);

        // Tạo các chi nhánh giả ở gần TPHCM
        addFakeBranches();

        // Di chuyển camera đến TPHCM
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(hoChiMinhCity, 13f));
    }

    private void addFakeBranches() {
        // Chi nhánh 1: Quận 1 (Đông Du)
        LatLng branch1 = new LatLng(10.7751, 106.7018);
        googleMap.addMarker(new MarkerOptions()
                .position(branch1)
                .title("VIBEBANK - Chi nhánh Đông Du")
                .snippet("34-36 Đông Du, Bến Nghé, Quận 1")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

        // Chi nhánh 2: Quận 3 (Võ Văn Tần)
        LatLng branch2 = new LatLng(10.7789, 106.6918);
        googleMap.addMarker(new MarkerOptions()
                .position(branch2)
                .title("VIBEBANK - Chi nhánh Võ Văn Tần")
                .snippet("220 Võ Văn Tần, Phường 5, Quận 3")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

        // Chi nhánh 3: Quận Bình Thạnh
        LatLng branch3 = new LatLng(10.8023, 106.7144);
        googleMap.addMarker(new MarkerOptions()
                .position(branch3)
                .title("VIBEBANK - Chi nhánh Bình Thạnh")
                .snippet("180 Xô Viết Nghệ Tĩnh, Phường 21, Bình Thạnh")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

        // Chi nhánh 4: Quận 7 (Phú Mỹ Hưng)
        LatLng branch4 = new LatLng(10.7281, 106.7195);
        googleMap.addMarker(new MarkerOptions()
                .position(branch4)
                .title("VIBEBANK - Chi nhánh Phú Mỹ Hưng")
                .snippet("Crescent Mall, Nguyễn Văn Linh, Quận 7")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

        // Chi nhánh 5: Thủ Đức
        LatLng branch5 = new LatLng(10.8483, 106.7717);
        googleMap.addMarker(new MarkerOptions()
                .position(branch5)
                .title("VIBEBANK - Chi nhánh Thủ Đức")
                .snippet("216 Võ Văn Ngân, Linh Chiểu, Thủ Đức")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (googleMap != null) {
                    try {
                        googleMap.setMyLocationEnabled(true);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void selectNavItem(int position) {
        // Reset all to gray
        txtHome.setTextColor(0xFF666666);
        txtHistory.setTextColor(0xFF666666);
        txtTransfer.setTextColor(0xFF666666);
        txtSupport.setTextColor(0xFF666666);

        // Set selected to brown
        switch (position) {
            case 0:
                txtHome.setTextColor(0xFFA0522D);
                break;
            case 1:
                txtHistory.setTextColor(0xFFA0522D);
                Intent historyIntent = new Intent(this, TransactionHistoryActivity.class);
                startActivity(historyIntent);
                break;
            case 3:
                txtTransfer.setTextColor(0xFFA0522D);
                Intent transferIntent = new Intent(this, TransferActivity.class);
                startActivity(transferIntent);
                break;
            case 4:
                txtSupport.setTextColor(0xFFA0522D);
                Intent profileIntent = new Intent(this, ProfileActivity.class);
                startActivity(profileIntent);
                break;
        }
    }

    private void setupFunctionButtons() {
        // Main functions
        findViewById(R.id.btnAccountManagement).setOnClickListener(v -> {
            Intent intent = new Intent(this, AccountManagementActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnTransfer).setOnClickListener(v -> {
            Intent intent = new Intent(this, TransferActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnQR).setOnClickListener(v ->
            Toast.makeText(this, "Mã QR của tôi", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnWithdraw).setOnClickListener(v -> {
            Intent intent = new Intent(this, WithdrawCodeActivity.class);
            startActivity(intent);
        });

        // Secondary functions
        findViewById(R.id.btnElectricity).setOnClickListener(v -> {
            Intent intent = new Intent(this, ElectricBillActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnWater).setOnClickListener(v -> {
            Intent intent = new Intent(this, WaterBillActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnTopup).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, TopupActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnTicket).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, FlightTicketActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnMovie).setOnClickListener(v ->
            Toast.makeText(this, "Vé xem phim", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btnHotel).setOnClickListener(v ->
            Toast.makeText(this, "Khách sạn", Toast.LENGTH_SHORT).show());
    }

    // MapView lifecycle methods
    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        mapView.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mapView != null) {
            mapView.onDestroy();
        }

        // Hủy lắng nghe notification
        if (notificationListener != null) {
            notificationListener.remove();
        }

        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }
        mapView.onSaveInstanceState(mapViewBundle);
    }
}
