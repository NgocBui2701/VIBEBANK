package com.example.vibebank;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibebank.adapter.BranchAdapter;
import com.example.vibebank.model.Branch;
import com.example.vibebank.utils.DirectionsApiHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BranchMapActivity extends AppCompatActivity implements OnMapReadyCallback,
        BranchAdapter.OnBranchClickListener {

    private static final String TAG = "BranchMapActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    // Views
    private MapView mapView;
    private GoogleMap googleMap;
    private CardView header, bottomSheet;
    private ImageView btnBack, btnToggleView, btnBackFromList;
    private FloatingActionButton fabMyLocation;
    private LinearLayout listContainer;
    private RecyclerView recyclerBranches;
    
    // Bottom Sheet Views
    private TextView tvBottomSheetName, tvBottomSheetAddress, tvBottomSheetDistance, tvBottomSheetDuration;
    
    // Data
    private List<Branch> branches;
    private BranchAdapter adapter;
    private boolean isMapView = true;
    private Branch selectedBranch;
    private LatLng currentLocation;
    
    // Google Maps
    private FusedLocationProviderClient fusedLocationClient;
    private Polyline currentPolyline;
    private Map<Marker, Branch> markerBranchMap;
    
    // Directions API
    private DirectionsApiHelper directionsHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_branch_map);

        initializeViews();
        setupMap(savedInstanceState);
        setupRecyclerView();
        setupListeners();
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        directionsHelper = new DirectionsApiHelper();
        markerBranchMap = new HashMap<>();
        
        loadBranches();
        checkLocationPermission();
    }

    private void initializeViews() {
        mapView = findViewById(R.id.mapView);
        header = findViewById(R.id.header);
        bottomSheet = findViewById(R.id.bottomSheet);
        btnBack = findViewById(R.id.btnBack);
        btnToggleView = findViewById(R.id.btnToggleView);
        btnBackFromList = findViewById(R.id.btnBackFromList);
        fabMyLocation = findViewById(R.id.fabMyLocation);
        listContainer = findViewById(R.id.listContainer);
        recyclerBranches = findViewById(R.id.recyclerBranches);
        
        // Bottom Sheet
        tvBottomSheetName = findViewById(R.id.tvBottomSheetName);
        tvBottomSheetAddress = findViewById(R.id.tvBottomSheetAddress);
        tvBottomSheetDistance = findViewById(R.id.tvBottomSheetDistance);
        tvBottomSheetDuration = findViewById(R.id.tvBottomSheetDuration);
    }

    private void setupMap(Bundle savedInstanceState) {
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);
    }

    private void setupRecyclerView() {
        branches = new ArrayList<>();
        adapter = new BranchAdapter(branches, this);
        recyclerBranches.setLayoutManager(new LinearLayoutManager(this));
        recyclerBranches.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnBackFromList.setOnClickListener(v -> toggleView());
        btnToggleView.setOnClickListener(v -> toggleView());
        
        fabMyLocation.setOnClickListener(v -> moveToCurrentLocation());
        
        findViewById(R.id.btnShowDirections).setOnClickListener(v -> {
            if (selectedBranch != null && currentLocation != null) {
                showDirections(selectedBranch);
            }
        });
        
        findViewById(R.id.btnOpenGoogleMaps).setOnClickListener(v -> {
            if (selectedBranch != null) {
                openInGoogleMaps(selectedBranch);
            }
        });
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

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Enable location if permission granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            getCurrentLocation();
        }

        // Add markers for all branches
        for (Branch branch : branches) {
            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(branch.getLocation())
                    .title(branch.getName())
                    .snippet(branch.getAddress())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
            
            if (marker != null) {
                markerBranchMap.put(marker, branch);
            }
        }

        // Set marker click listener
        googleMap.setOnMarkerClickListener(marker -> {
            Branch branch = markerBranchMap.get(marker);
            if (branch != null) {
                showBranchDetails(branch);
                return true;
            }
            return false;
        });

        // Move camera to Ho Chi Minh City
        LatLng hoChiMinhCity = new LatLng(10.7769, 106.7009);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(hoChiMinhCity, 13f));
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        calculateDistances();
                        sortBranchesByDistance();
                    }
                });
    }

    private void calculateDistances() {
        if (currentLocation == null) return;

        for (Branch branch : branches) {
            float[] results = new float[1];
            Location.distanceBetween(
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

    private void moveToCurrentLocation() {
        if (currentLocation != null && googleMap != null) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f));
        } else {
            Toast.makeText(this, "Đang xác định vị trí...", Toast.LENGTH_SHORT).show();
            getCurrentLocation();
        }
    }

    private void toggleView() {
        isMapView = !isMapView;
        if (isMapView) {
            // Show map
            mapView.setVisibility(View.VISIBLE);
            listContainer.setVisibility(View.GONE);
            header.setVisibility(View.VISIBLE);
            fabMyLocation.setVisibility(View.VISIBLE);
        } else {
            // Show list
            mapView.setVisibility(View.GONE);
            listContainer.setVisibility(View.VISIBLE);
            header.setVisibility(View.GONE);
            fabMyLocation.setVisibility(View.GONE);
        }
    }

    private void showBranchDetails(Branch branch) {
        selectedBranch = branch;
        bottomSheet.setVisibility(View.VISIBLE);
        
        tvBottomSheetName.setText(branch.getName());
        tvBottomSheetAddress.setText(branch.getAddress());
        tvBottomSheetDistance.setText(branch.getDistanceText());
        tvBottomSheetDuration.setText("");
        
        // Move camera to branch
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(branch.getLocation(), 16f));
    }

    private void showDirections(Branch branch) {
        if (currentLocation == null) {
            Toast.makeText(this, "Không xác định được vị trí hiện tại", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Đang tính toán đường đi...", Toast.LENGTH_SHORT).show();

        directionsHelper.getDirections(currentLocation, branch.getLocation(),
                new DirectionsApiHelper.DirectionsCallback() {
                    @Override
                    public void onDirectionsReceived(List<LatLng> polylinePoints, String distance, String duration) {
                        // Clear previous polyline
                        if (currentPolyline != null) {
                            currentPolyline.remove();
                        }

                        // Draw new polyline
                        PolylineOptions polylineOptions = new PolylineOptions()
                                .addAll(polylinePoints)
                                .width(10)
                                .color(Color.BLUE)
                                .geodesic(true);

                        currentPolyline = googleMap.addPolyline(polylineOptions);

                        // Update bottom sheet
                        tvBottomSheetDuration.setText(" • " + duration);

                        Toast.makeText(BranchMapActivity.this,
                                "Khoảng cách: " + distance + " - Thời gian: " + duration,
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(BranchMapActivity.this,
                                "Lỗi: " + error,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openInGoogleMaps(Branch branch) {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" +
                branch.getLocation().latitude + "," + branch.getLocation().longitude);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Toast.makeText(this, "Vui lòng cài đặt Google Maps", Toast.LENGTH_SHORT).show();
        }
    }

    // BranchAdapter.OnBranchClickListener implementation
    @Override
    public void onBranchClick(Branch branch) {
        // Switch to map view and show branch details
        if (!isMapView) {
            toggleView();
        }
        showBranchDetails(branch);
    }

    @Override
    public void onDirectionClick(Branch branch) {
        if (!isMapView) {
            toggleView();
        }
        showBranchDetails(branch);
        showDirections(branch);
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
                        getCurrentLocation();
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
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
        if (directionsHelper != null) {
            directionsHelper.shutdown();
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

