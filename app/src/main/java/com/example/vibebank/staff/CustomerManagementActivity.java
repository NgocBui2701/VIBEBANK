package com.example.vibebank.staff;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.vibebank.R;
import com.example.vibebank.adapter.CustomerAdapter;
import com.example.vibebank.model.Customer;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerManagementActivity extends AppCompatActivity 
        implements CustomerAdapter.OnCustomerClickListener {

    private RecyclerView recyclerCustomers;
    private CustomerAdapter adapter;
    private List<Customer> allCustomers = new ArrayList<>();
    private List<Customer> displayCustomers = new ArrayList<>();
    
    private TextInputEditText edtSearch;
    private Chip chipAll, chipPending, chipVerified;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout layoutEmpty;
    private ImageView btnBack;

    private FirebaseFirestore db;
    private String currentFilter = "ALL"; // ALL, PENDING, VERIFIED
    private String currentSearchQuery = "";

    // Cache để lưu balance của từng user
    private Map<String, Double> balanceCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_management);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        setupListeners();
        loadCustomers();
    }

    private void initViews() {
        recyclerCustomers = findViewById(R.id.recyclerCustomers);
        edtSearch = findViewById(R.id.edtSearch);
        chipAll = findViewById(R.id.chipAll);
        chipPending = findViewById(R.id.chipPending);
        chipVerified = findViewById(R.id.chipVerified);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupRecyclerView() {
        adapter = new CustomerAdapter(displayCustomers, this);
        recyclerCustomers.setLayoutManager(new LinearLayoutManager(this));
        recyclerCustomers.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        swipeRefresh.setOnRefreshListener(this::loadCustomers);

        // Filter chips
        chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "ALL";
                filterCustomers();
            }
        });

        chipPending.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "PENDING";
                filterCustomers();
            }
        });

        chipVerified.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentFilter = "VERIFIED";
                filterCustomers();
            }
        });

        // Search
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().toLowerCase().trim();
                filterCustomers();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadCustomers() {
        swipeRefresh.setRefreshing(true);
        allCustomers.clear();
        balanceCache.clear();

        // Load all customers (users with role = customer)
        db.collection("users")
                .whereEqualTo("role", "customer")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot userDoc : querySnapshot.getDocuments()) {
                        String userId = userDoc.getId();
                        String fullName = userDoc.getString("full_name");
                        String phone = userDoc.getString("phone_number");
                        String email = userDoc.getString("email");

                        // Load KYC status
                        loadCustomerKycAndBalance(userId, fullName, phone, email);
                    }
                })
                .addOnFailureListener(e -> {
                    swipeRefresh.setRefreshing(false);
                });
    }

    private void loadCustomerKycAndBalance(String userId, String fullName, String phone, String email) {
        // Load KYC document
        db.collection("kyc_documents").document(userId)
                .get()
                .addOnSuccessListener(kycDoc -> {
                    String kycStatus = "pending";
                    if (kycDoc.exists()) {
                        kycStatus = kycDoc.getString("status");
                        if (kycStatus == null) kycStatus = "pending";
                    }

                    String finalKycStatus = kycStatus;

                    // Load account balance
                    db.collection("accounts").document(userId)
                            .get()
                            .addOnSuccessListener(accDoc -> {
                                double balance = 0;
                                String accountNumber = phone; // Default

                                if (accDoc.exists()) {
                                    Double balanceValue = accDoc.getDouble("balance");
                                    if (balanceValue != null) balance = balanceValue;

                                    String accNum = accDoc.getString("account_number");
                                    if (accNum != null) accountNumber = accNum;
                                }

                                Customer customer = new Customer(
                                        userId, fullName, phone, email,
                                        accountNumber, finalKycStatus, balance
                                );

                                allCustomers.add(customer);
                                filterCustomers();
                                swipeRefresh.setRefreshing(false);
                            });
                });
    }

    private void filterCustomers() {
        displayCustomers.clear();

        for (Customer customer : allCustomers) {
            // Filter by KYC status
            if (!currentFilter.equals("ALL")) {
                if (currentFilter.equals("PENDING") && !"pending".equals(customer.getKycStatus())) {
                    continue;
                }
                if (currentFilter.equals("VERIFIED") && !"verified".equals(customer.getKycStatus())) {
                    continue;
                }
            }

            // Filter by search query
            if (!currentSearchQuery.isEmpty()) {
                boolean matchName = customer.getFullName().toLowerCase().contains(currentSearchQuery);
                boolean matchPhone = customer.getPhoneNumber().toLowerCase().contains(currentSearchQuery);
                boolean matchAccount = customer.getAccountNumber().toLowerCase().contains(currentSearchQuery);

                if (!matchName && !matchPhone && !matchAccount) {
                    continue;
                }
            }

            displayCustomers.add(customer);
        }

        adapter.updateCustomers(displayCustomers);

        // Show/Hide empty state
        if (displayCustomers.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerCustomers.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerCustomers.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCustomerClick(Customer customer) {
        Intent intent = new Intent(this, CustomerDetailActivity.class);
        intent.putExtra("userId", customer.getUserId());
        intent.putExtra("fullName", customer.getFullName());
        intent.putExtra("phoneNumber", customer.getPhoneNumber());
        intent.putExtra("email", customer.getEmail());
        intent.putExtra("accountNumber", customer.getAccountNumber());
        intent.putExtra("kycStatus", customer.getKycStatus());
        intent.putExtra("balance", customer.getBalance());
        startActivity(intent);
    }
}

