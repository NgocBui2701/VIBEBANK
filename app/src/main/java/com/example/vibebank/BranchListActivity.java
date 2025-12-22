package com.example.vibebank;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibebank.adapter.BranchAdapter;
import com.example.vibebank.model.Branch;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BranchListActivity extends AppCompatActivity implements BranchAdapter.OnBranchClickListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;

    private RecyclerView recyclerBranches;
    private BranchAdapter adapter;
    private List<Branch> branches;
    private ImageView btnBack;
    private ProgressBar progressBar;
    private LinearLayout layoutEmpty;

    private FusedLocationProviderClient fusedLocationClient;
    private LatLng currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_branch_list);

        initializeViews();
        setupRecyclerView();
        setupListeners();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        loadBranches();
        checkLocationPermission();
    }

    private void initializeViews() {
        recyclerBranches = findViewById(R.id.recyclerBranches);
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);
        layoutEmpty = findViewById(R.id.layoutEmpty);
    }

    private void setupRecyclerView() {
        branches = new ArrayList<>();
        adapter = new BranchAdapter(branches, this);
        recyclerBranches.setLayoutManager(new LinearLayoutManager(this));
        recyclerBranches.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadBranches() {
        branches.clear();

        // Chi nhánh 1: Quận 1 (Đông Du)
        branches.add(new Branch(
                "br1",
                "VIBEBANK - Chi nhánh Đông Du",
                "34-36 Đông Du, Bến Nghé, Quận 1",
                new LatLng(10.7751, 106.7018),
                "1900 6666",
                "8:00 - 17:00 (Thứ 2 - Thứ 6)"
        ));

        // Chi nhánh 2: Quận 3 (Võ Văn Tần)
        branches.add(new Branch(
                "br2",
                "VIBEBANK - Chi nhánh Võ Văn Tần",
                "220 Võ Văn Tần, Phường 5, Quận 3",
                new LatLng(10.7789, 106.6918),
                "1900 6666",
                "8:00 - 17:00 (Thứ 2 - Thứ 6)"
        ));

        // Chi nhánh 3: Quận Bình Thạnh
        branches.add(new Branch(
                "br3",
                "VIBEBANK - Chi nhánh Bình Thạnh",
                "180 Xô Viết Nghệ Tĩnh, Phường 21, Bình Thạnh",
                new LatLng(10.8023, 106.7144),
                "1900 6666",
                "8:00 - 17:00 (Thứ 2 - Thứ 6)"
        ));

        // Chi nhánh 4: Quận 7 (Phú Mỹ Hưng)
        branches.add(new Branch(
                "br4",
                "VIBEBANK - Chi nhánh Phú Mỹ Hưng",
                "Crescent Mall, Nguyễn Văn Linh, Quận 7",
                new LatLng(10.7281, 106.7195),
                "1900 6666",
                "8:00 - 17:00 (Thứ 2 - Thứ 6)"
        ));

        // Chi nhánh 5: Thủ Đức
        branches.add(new Branch(
                "br5",
                "VIBEBANK - Chi nhánh Thủ Đức",
                "216 Võ Văn Ngân, Linh Chiểu, Thủ Đức",
                new LatLng(10.8483, 106.7717),
                "1900 6666",
                "8:00 - 17:00 (Thứ 2 - Thứ 6)"
        ));

        adapter.updateBranches(branches);
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    progressBar.setVisibility(View.GONE);

                    if (location != null) {
                        currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        calculateDistances();
                        sortBranchesByDistance();
                    } else {
                        Toast.makeText(this, "Không xác định được vị trí. Hiển thị danh sách mặc định.", 
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi lấy vị trí: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void calculateDistances() {
        if (currentLocation == null) return;

        for (Branch branch : branches) {
            float[] results = new float[1];
            android.location.Location.distanceBetween(
                    currentLocation.latitude, currentLocation.longitude,
                    branch.getLocation().latitude, branch.getLocation().longitude,
                    results
            );
            branch.setDistanceInKm(results[0] / 1000f); // Convert to km
        }
    }

    private void sortBranchesByDistance() {
        Collections.sort(branches, Comparator.comparing(Branch::getDistanceInKm));
        adapter.updateBranches(branches);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Quyền truy cập vị trí bị từ chối. Hiển thị danh sách mặc định.", 
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // BranchAdapter.OnBranchClickListener implementation
    @Override
    public void onBranchClick(Branch branch) {
        openInGoogleMaps(branch);
    }

    @Override
    public void onDirectionClick(Branch branch) {
        openInGoogleMaps(branch);
    }

    private void openInGoogleMaps(Branch branch) {
        // Mở Google Maps với chế độ navigation
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" +
                branch.getLocation().latitude + "," + branch.getLocation().longitude +
                "&mode=d"); // mode=d for driving

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            // Fallback: mở trên web browser nếu không có Google Maps
            Uri webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" +
                    branch.getLocation().latitude + "," + branch.getLocation().longitude);
            Intent webIntent = new Intent(Intent.ACTION_VIEW, webUri);
            startActivity(webIntent);
        }
    }
}

