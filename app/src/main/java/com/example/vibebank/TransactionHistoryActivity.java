package com.example.vibebank;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;

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
    private List<Transaction> allTransactions;
    private List<Transaction> filteredTransactions;
    
    private int selectedDays = 7; // 7, 30, 60, -1 for all
    private String selectedType = "all"; // all, receive, send

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        initViews();
        setupListeners();
        loadTransactions();
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

        recyclerTransactions.setLayoutManager(new LinearLayoutManager(this));
        allTransactions = new ArrayList<>();
        filteredTransactions = new ArrayList<>();
        adapter = new TransactionAdapter(filteredTransactions, this);
        recyclerTransactions.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // Time filter buttons
        btn7Days.setOnClickListener(v -> selectTimeFilter(7, btn7Days));
        btn30Days.setOnClickListener(v -> selectTimeFilter(30, btn30Days));
        btn60Days.setOnClickListener(v -> selectTimeFilter(60, btn60Days));
        btnAllTime.setOnClickListener(v -> selectTimeFilter(-1, btnAllTime));

        // Type filter chips
        chipAll.setOnClickListener(v -> {
            selectedType = "all";
            filterTransactions();
        });
        chipReceive.setOnClickListener(v -> {
            selectedType = "receive";
            filterTransactions();
        });
        chipSend.setOnClickListener(v -> {
            selectedType = "send";
            filterTransactions();
        });

        // Search
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTransactions();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Swipe to refresh
        swipeRefresh.setOnRefreshListener(() -> {
            new Handler().postDelayed(() -> {
                loadTransactions();
                swipeRefresh.setRefreshing(false);
            }, 1000);
        });
    }

    private void selectTimeFilter(int days, MaterialButton selectedButton) {
        selectedDays = days;

        // Reset all buttons
        resetButtonStyle(btn7Days);
        resetButtonStyle(btn30Days);
        resetButtonStyle(btn60Days);
        resetButtonStyle(btnAllTime);

        // Highlight selected
        selectedButton.setBackgroundColor(getResources().getColor(android.R.color.black));
        selectedButton.setTextColor(getResources().getColor(android.R.color.white));

        filterTransactions();
    }

    private void resetButtonStyle(MaterialButton button) {
        button.setBackgroundColor(getResources().getColor(android.R.color.white));
        button.setTextColor(getResources().getColor(android.R.color.black));
    }

    private void loadTransactions() {
        // Sample data - Replace with actual data from database
        allTransactions.clear();
        
        Calendar cal = Calendar.getInstance();
        
        // Today's transactions
        allTransactions.add(new Transaction("BUI THI BICH NGOC", "Chuyển tiền cơm Khói", 
            30000, true, cal.getTime(), "receive"));
        allTransactions.add(new Transaction("NGUYEN QUOC BAO", "Chuyển tiền cơm Khói", 
            -30000, false, cal.getTime(), "send"));

        // Yesterday
        cal.add(Calendar.DAY_OF_MONTH, -1);
        allTransactions.add(new Transaction("NGUYEN ANH KIET", "Chuyển tiền Ngo gia", 
            30000, true, cal.getTime(), "receive"));
        allTransactions.add(new Transaction("NGUYEN QUOC BAO", "Ba roi trung", 
            -35000, false, cal.getTime(), "send"));

        // 3 days ago
        cal.add(Calendar.DAY_OF_MONTH, -2);
        allTransactions.add(new Transaction("TRAN VAN AN", "Thanh toán hóa đơn", 
            50000, true, cal.getTime(), "receive"));
        
        // 10 days ago
        cal.add(Calendar.DAY_OF_MONTH, -7);
        allTransactions.add(new Transaction("LE THI MAI", "Trả tiền cafe", 
            -45000, false, cal.getTime(), "send"));

        // 35 days ago
        cal.add(Calendar.DAY_OF_MONTH, -25);
        allTransactions.add(new Transaction("PHAM MINH TU", "Chia tiền ăn", 
            80000, true, cal.getTime(), "receive"));

        filterTransactions();
    }

    private void filterTransactions() {
        filteredTransactions.clear();
        
        Calendar cutoffDate = Calendar.getInstance();
        if (selectedDays > 0) {
            cutoffDate.add(Calendar.DAY_OF_MONTH, -selectedDays);
        }

        String searchQuery = edtSearch.getText().toString().toLowerCase().trim();

        long totalIncome = 0;
        long totalExpense = 0;

        for (Transaction transaction : allTransactions) {
            // Filter by time
            if (selectedDays > 0 && transaction.getDate().before(cutoffDate.getTime())) {
                continue;
            }

            // Filter by type
            if (!selectedType.equals("all")) {
                if (!transaction.getType().equals(selectedType)) {
                    continue;
                }
            }

            // Filter by search
            if (!searchQuery.isEmpty()) {
                if (!transaction.getName().toLowerCase().contains(searchQuery) &&
                    !transaction.getDescription().toLowerCase().contains(searchQuery)) {
                    continue;
                }
            }

            filteredTransactions.add(transaction);

            // Calculate totals
            if (transaction.isIncome()) {
                totalIncome += transaction.getAmount();
            } else {
                totalExpense += Math.abs(transaction.getAmount());
            }
        }

        updateSummary(totalIncome, totalExpense);
        adapter.notifyDataSetChanged();
        
        // Show/hide empty state
        if (filteredTransactions.isEmpty()) {
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
