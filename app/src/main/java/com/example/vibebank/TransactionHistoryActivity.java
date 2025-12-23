package com.example.vibebank;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.vibebank.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionHistoryActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView txtTotalIncome, txtTotalExpense;
    private MaterialButton btn7Days, btn30Days, btn60Days, btnAllTime;
    private TextInputEditText edtSearch;
    private Chip chipAll, chipReceive, chipSend;
    private RecyclerView recyclerTransactions;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout layoutEmpty;

    private TransactionAdapter adapter;
    private List<Transaction> allTransactions = new ArrayList<>();;
    private List<Transaction> displayTransactions = new ArrayList<>();

    private int currentDayFilter = -1; // -1: All, 7, 30, 60
    private String currentTypeFilter = "ALL"; // ALL, RECEIVED, SENT
    private String currentSearchQuery = "";

    private FirebaseFirestore db;
    private String currentUserId;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);
        
        // Lấy ID người dùng hiện tại từ session
        currentUserId = sessionManager.getCurrentUserId();
        
        // Fallback: thử lấy từ FirebaseAuth nếu chưa có trong session
        if (currentUserId == null || currentUserId.isEmpty()) {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            }
        }

        // Kiểm tra lần cuối
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không xác định được người dùng.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initViews();
        setupListeners();
        setupRecycler();

        loadData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        txtTotalIncome = findViewById(R.id.txtTotalIncome);
        txtTotalExpense = findViewById(R.id.txtTotalExpense);
        
        btn7Days = findViewById(R.id.btn7Days);
        btn30Days = findViewById(R.id.btn30Days);
        btn60Days = findViewById(R.id.btn60Days);
        btnAllTime = findViewById(R.id.btnAllTime);
        
        edtSearch = findViewById(R.id.edtSearch);

        chipAll = findViewById(R.id.chipAll);
        chipReceive = findViewById(R.id.chipReceive);
        chipSend = findViewById(R.id.chipSend);
        
        recyclerTransactions = findViewById(R.id.recyclerTransactions);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        // Mặc định chọn Tất cả thời gian
        updateTimeFilterUI(btnAllTime);
    }

    private void setupRecycler() {
        adapter = new TransactionAdapter(displayTransactions, this);
        recyclerTransactions.setLayoutManager(new LinearLayoutManager(this));
        recyclerTransactions.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        swipeRefresh.setOnRefreshListener(this::loadData);

        btn7Days.setOnClickListener(v -> {
            currentDayFilter = 7;
            updateTimeFilterUI(btn7Days);
            filterTransactions();
        });
        btn30Days.setOnClickListener(v -> {
            currentDayFilter = 30;
            updateTimeFilterUI(btn30Days);
            filterTransactions();
        });
        btn60Days.setOnClickListener(v -> {
            currentDayFilter = 60;
            updateTimeFilterUI(btn60Days);
            filterTransactions();
        });
        btnAllTime.setOnClickListener(v -> {
            currentDayFilter = -1;
            updateTimeFilterUI(btnAllTime);
            filterTransactions();
        });

        // Type Filters (Chips)
        chipAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentTypeFilter = "ALL";
                filterTransactions();
            }
        });
        chipReceive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentTypeFilter = "RECEIVED";
                filterTransactions();
            }
        });
        chipSend.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentTypeFilter = "SENT";
                filterTransactions();
            }
        });

        // Search
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().toLowerCase().trim();
                filterTransactions();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Swipe to refresh
        swipeRefresh.setOnRefreshListener(() -> {
            new Handler().postDelayed(() -> {
                loadData();
                swipeRefresh.setRefreshing(false);
            }, 1000);
        });
    }

    private void loadData() {
        if (currentUserId == null) {
            swipeRefresh.setRefreshing(false);
            return;
        }

        swipeRefresh.setRefreshing(true);

        allTransactions.clear();

        // Đọc transactions từ cả 3 collections: accounts, savings, credit_cards
        com.google.android.gms.tasks.Task<QuerySnapshot> accountTask = 
            db.collection("accounts").document(currentUserId)
                .collection("transactions")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get();
        
        com.google.android.gms.tasks.Task<QuerySnapshot> savingTask = 
            db.collection("savings").document(currentUserId)
                .collection("transactions")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get();
        
        com.google.android.gms.tasks.Task<QuerySnapshot> creditTask = 
            db.collection("credit_cards").document(currentUserId)
                .collection("transactions")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get();

        // Đợi cả 3 tasks hoàn thành
        com.google.android.gms.tasks.Tasks.whenAllComplete(accountTask, savingTask, creditTask)
                .addOnSuccessListener(tasks -> {
                    // Process account transactions
                    if (accountTask.isSuccessful() && accountTask.getResult() != null) {
                        processTransactions(accountTask.getResult());
                    }
                    
                    // Process saving transactions
                    if (savingTask.isSuccessful() && savingTask.getResult() != null) {
                        processTransactions(savingTask.getResult());
                    }
                    
                    // Process credit transactions
                    if (creditTask.isSuccessful() && creditTask.getResult() != null) {
                        processTransactions(creditTask.getResult());
                    }

                    // Sort tất cả transactions theo timestamp (mới nhất trước)
                    java.util.Collections.sort(allTransactions, (t1, t2) -> t2.getDate().compareTo(t1.getDate()));

                    // Sau khi tải xong thì lọc và hiển thị
                    filterTransactions();
                    swipeRefresh.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    swipeRefresh.setRefreshing(false);
                });
    }

    // Helper method to process transactions from a query result
    private void processTransactions(QuerySnapshot queryDocumentSnapshots) {
        for (DocumentSnapshot doc : queryDocumentSnapshots) {
            try {
                String type = doc.getString("type"); // "SENT" hoặc "RECEIVED"
                Double amount = doc.getDouble("amount");
                String content = doc.getString("content");
                String relatedName = doc.getString("relatedAccountName");
                Timestamp timestamp = doc.getTimestamp("timestamp");

                if (amount != null && type != null) {
                    allTransactions.add(new Transaction(
                            relatedName != null ? relatedName : "Giao dịch",
                            content != null ? content : "",
                            amount.longValue(),
                            "RECEIVED".equals(type), // isIncome nếu type là RECEIVED
                            timestamp != null ? timestamp.toDate() : new Date(),
                            type
                    ));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Logic lọc trung tâm: Lọc theo Thời gian, Loại, và Tìm kiếm cùng lúc
    private void filterTransactions() {
        displayTransactions.clear();

        long totalIncome = 0;
        long totalExpense = 0;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // Xác định mốc thời gian lọc
        Date filterDateLimit = null;
        if (currentDayFilter != -1) {
            cal.add(Calendar.DAY_OF_YEAR, -currentDayFilter);
            filterDateLimit = cal.getTime();
        }

        for (Transaction trans : allTransactions) {
            // 1. Lọc theo Thời gian
            if (filterDateLimit != null && trans.getDate().before(filterDateLimit)) {
                continue; // Bỏ qua nếu cũ hơn mốc thời gian
            }

            // 2. Lọc theo Loại (All/Income/Expense)
            if (!currentTypeFilter.equals("ALL")) {
                if (currentTypeFilter.equals("RECEIVED") && !trans.isIncome()) continue;
                if (currentTypeFilter.equals("SENT") && trans.isIncome()) continue;
            }

            // 3. Lọc theo Tìm kiếm (Tên hoặc Nội dung)
            if (!currentSearchQuery.isEmpty()) {
                boolean matchName = trans.getName().toLowerCase().contains(currentSearchQuery);
                boolean matchDesc = trans.getDescription().toLowerCase().contains(currentSearchQuery);
                if (!matchName && !matchDesc) continue;
            }

            // Thỏa mãn tất cả điều kiện -> Thêm vào list hiển thị
            displayTransactions.add(trans);

            if (trans.isIncome()) {
                totalIncome += trans.getAmount();
            } else {
                totalExpense += trans.getAmount();
            }
        }

        // Cập nhật UI
        adapter.notifyDataSetChanged();
        updateSummary(totalIncome, totalExpense);

        // Show/Hide Empty state
        if (displayTransactions.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerTransactions.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerTransactions.setVisibility(View.VISIBLE);
        }
    }

    private void updateSummary(long income, long expense) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        
        txtTotalIncome.setText("+ " + formatter.format(income) + " đ");
        txtTotalExpense.setText("- " + formatter.format(expense) + " đ");
    }

    private void updateTimeFilterUI(MaterialButton selectedBtn) {
        // Reset style
        btn7Days.setStrokeColor(getColorStateList(R.color.gray_transaction));
        btn30Days.setStrokeColor(getColorStateList(R.color.gray_transaction));
        btn60Days.setStrokeColor(getColorStateList(R.color.gray_transaction));
        btnAllTime.setStrokeColor(getColorStateList(R.color.gray_transaction));

        btn7Days.setBackgroundColor(getColor(R.color.white));
        btn30Days.setBackgroundColor(getColor(R.color.white));
        btn60Days.setBackgroundColor(getColor(R.color.white));
        btnAllTime.setBackgroundColor(getColor(R.color.white));

        // Set selected style
        selectedBtn.setStrokeColor(getColorStateList(R.color.blue_transaction));
        selectedBtn.setBackgroundColor(getColor(R.color.blue_transaction));
    }

    // Transaction model class
    public static class Transaction {
        private String name;
        private String description;
        private long amount;
        private boolean isIncome;
        private Date date;
        private String type; // "receive" or "send"

        public Transaction(String name, String description, long amount, boolean isIncome, Date date, String type) {
            this.name = name;
            this.description = description;
            this.amount = amount;
            this.isIncome = isIncome;
            this.date = date;
            this.type = type;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public long getAmount() { return amount; }
        public boolean isIncome() { return isIncome; }
        public Date getDate() { return date; }
        public String getType() { return type; }

        public String getDateHeader() {
            Calendar today = Calendar.getInstance();
            Calendar yesterday = Calendar.getInstance();
            yesterday.add(Calendar.DAY_OF_MONTH, -1);
            
            Calendar transactionDate = Calendar.getInstance();
            transactionDate.setTime(date);

            if (isSameDay(today, transactionDate)) {
                return "Hôm nay";
            } else if (isSameDay(yesterday, transactionDate)) {
                return "Hôm qua";
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("'Thứ' EEEE, dd 'tháng' MM, yyyy", new Locale("vi", "VN"));
                return sdf.format(date);
            }
        }

        public String getTimeString() {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return sdf.format(date);
        }

        private boolean isSameDay(Calendar cal1, Calendar cal2) {
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                   cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
        }
    }
}
