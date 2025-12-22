package com.example.vibebank;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.vibebank.ui.profile.ProfileActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "HomeActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    private TextView txtUserName;
    private TextView txtBalance;
    private ImageView btnToggleBalance;
    private ImageView btnNotification;
    private ImageView btnMenu;
    
    // Custom bottom nav
    private LinearLayout navHome, navHistory, navQR, navTransfer, navSupport;
    private TextView txtHome, txtHistory, txtTransfer, txtSupport;
    
    // Google Maps
    private MapView mapView;
    private GoogleMap googleMap;
    private ScrollView scrollView;

    private boolean isBalanceVisible = false;
    private String actualBalance = "1,234,567,890";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.home), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        txtUserName = findViewById(R.id.txtUserName);
        txtBalance = findViewById(R.id.txtBalance);
        btnToggleBalance = findViewById(R.id.btnToggleBalance);
        btnNotification = findViewById(R.id.btnNotification);
        btnMenu = findViewById(R.id.btnMenu);
        scrollView = findViewById(R.id.scrollView);
        
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
        
        // Initialize MapView
        mapView = findViewById(R.id.mapView);
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);
        
        // Prevent ScrollView from scrolling when touching the map
        mapView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        // Disable ScrollView scrolling
                        scrollView.requestDisallowInterceptTouchEvent(true);
                        return false;
                    case MotionEvent.ACTION_UP:
                        // Enable ScrollView scrolling
                        scrollView.requestDisallowInterceptTouchEvent(false);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        // Disable ScrollView scrolling
                        scrollView.requestDisallowInterceptTouchEvent(true);
                        return false;
                    default:
                        return true;
                }
            }
        });

        // Set user name from Intent or default
        String userName = getIntent().getStringExtra("userName");
        if (userName != null && !userName.isEmpty()) {
            txtUserName.setText(userName.toUpperCase());
        }

        // Toggle balance visibility
        btnToggleBalance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isBalanceVisible = !isBalanceVisible;
                if (isBalanceVisible) {
                    txtBalance.setText(actualBalance + " VND");
                    btnToggleBalance.setImageResource(R.drawable.ic_eye_off);
                } else {
                    txtBalance.setText("********* VND");
                    btnToggleBalance.setImageResource(R.drawable.ic_eye);
                }
            }
        });

        // Notification button
        btnNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(HomeActivity.this, "Thông báo", Toast.LENGTH_SHORT).show();
            }
        });

        // Menu button
        btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(HomeActivity.this, "Menu", Toast.LENGTH_SHORT).show();
            }
        });

        // Bottom navigation clicks
        setupBottomNav();

        // Function buttons
        setupFunctionButtons();
    }
    
    private void setupBottomNav() {
        navHome.setOnClickListener(v -> selectNavItem(0));
        navHistory.setOnClickListener(v -> selectNavItem(1));
        navQR.setOnClickListener(v -> {
            Toast.makeText(this, "Quét mã QR", Toast.LENGTH_SHORT).show();
        });
        navTransfer.setOnClickListener(v -> selectNavItem(3));
        navSupport.setOnClickListener(v -> selectNavItem(4));
        
        // Set home as selected by default
        selectNavItem(0);
        
        // Check location permission
        checkLocationPermission();
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
        findViewById(R.id.btnElectricity).setOnClickListener(v -> 
            Toast.makeText(this, "Tiền điện", Toast.LENGTH_SHORT).show());
        
        findViewById(R.id.btnWater).setOnClickListener(v -> 
            Toast.makeText(this, "Tiền nước", Toast.LENGTH_SHORT).show());
        
        findViewById(R.id.btnTopup).setOnClickListener(v -> 
            Toast.makeText(this, "Nạp cước", Toast.LENGTH_SHORT).show());
        
        findViewById(R.id.btnTicket).setOnClickListener(v -> 
            Toast.makeText(this, "Vé máy bay", Toast.LENGTH_SHORT).show());
        
        findViewById(R.id.btnMovie).setOnClickListener(v -> 
            Toast.makeText(this, "Vé xem phim", Toast.LENGTH_SHORT).show());
        
        findViewById(R.id.btnHotel).setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, HotelBookingActivity.class);
            startActivity(intent);
        });
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
        mapView.onDestroy();
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
