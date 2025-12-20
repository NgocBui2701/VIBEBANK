package com.example.vibebank;

//import android.content.Intent;
//import android.os.Bundle;
//import android.text.Editable;
//import android.text.TextWatcher;
//import android.view.View;
//import android.widget.EditText;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//public class TransferActivity extends AppCompatActivity {
//    private ImageView btnBack;
//    private EditText edtSearch;
//    private LinearLayout btnNewRecipient;
//    private LinearLayout btnRecipient1, btnRecipient2;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_transfer);
//
//        // Initialize views
//        btnBack = findViewById(R.id.btnBack);
//        edtSearch = findViewById(R.id.edtSearch);
//        btnNewRecipient = findViewById(R.id.btnNewRecipient);
//        btnRecipient1 = findViewById(R.id.btnRecipient1);
//        btnRecipient2 = findViewById(R.id.btnRecipient2);
//
//        // Back button
//        btnBack.setOnClickListener(v -> finish());
//
//        // New recipient button
//        btnNewRecipient.setOnClickListener(v -> {
//            Intent intent = new Intent(this, TransferSelectBankActivity.class);
//            startActivity(intent);
//        });
//
//        // Saved recipients
//        btnRecipient1.setOnClickListener(v -> {
//            Intent intent = new Intent(this, TransferDetailsActivity.class);
//            intent.putExtra("bank", "VIBEBANK");
//            intent.putExtra("accountNumber", "0365349666");
//            intent.putExtra("accountName", "BUI THI BICH NGOC");
//            startActivity(intent);
//        });
//
//        btnRecipient2.setOnClickListener(v -> {
//            Intent intent = new Intent(this, TransferDetailsActivity.class);
//            intent.putExtra("bank", "VIBEBANK");
//            intent.putExtra("accountNumber", "0365349666");
//            intent.putExtra("accountName", "NGUYEN QUOC BAO");
//            startActivity(intent);
//        });
//
//        // Search functionality
//        edtSearch.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                // Implement search filter here
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {}
//        });
//    }
//}

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TransferActivity extends AppCompatActivity {

    private ListView lvSavedRecipients;
    private View btnAddNew; // Có thể là Button hoặc Layout
    private View btnBack;
    private EditText edtSearch;
    private SavedRecipientAdapter adapter;
    private RecyclerView rvSavedRecipients;
    private List<SavedRecipientAdapter.SavedRecipient> recipientList;
    private List<SavedRecipientAdapter.SavedRecipient> originalRecipientList; // List gốc
    private LinearLayout layoutHardcodedList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);

        rvSavedRecipients = findViewById(R.id.rvSavedRecipients); // Cần thêm ListView vào activity_transfer.xml nếu chưa có
        btnAddNew = findViewById(R.id.btnNewRecipient); // Hoặc ID layout chứa nút thêm mới
        btnBack = findViewById(R.id.btnBack);
        edtSearch = findViewById(R.id.edtSearch);

        // Ẩn danh sách mẫu hardcode trong XML
        layoutHardcodedList = findViewById(R.id.savedRecipientsList);
        if (layoutHardcodedList != null) {
            layoutHardcodedList.setVisibility(View.GONE);
        }

        // Setup RecyclerView
        recipientList = new ArrayList<>();
        originalRecipientList = new ArrayList<>();

        rvSavedRecipients.setLayoutManager(new LinearLayoutManager(this));

        // Khởi tạo Adapter và xử lý sự kiện click
        adapter = new SavedRecipientAdapter(recipientList, new SavedRecipientAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(SavedRecipientAdapter.SavedRecipient recipient) {
                // Chuyển sang màn hình chuyển tiền
                Intent intent = new Intent(TransferActivity.this, TransferDetailsActivity.class);
                intent.putExtra("receiverAccountNumber", recipient.accountNumber);
                intent.putExtra("receiverName", recipient.name);
                intent.putExtra("receiverUserId", recipient.userId);
                intent.putExtra("bankName", "VibeBank");
                startActivity(intent);
            }

            @Override
            public void onEditClick(SavedRecipientAdapter.SavedRecipient recipient, View view) {
                // Hiển thị Menu xóa khi bấm nút Edit
                showActionMenu(recipient, view);
            }
        });

        rvSavedRecipients.setAdapter(adapter);

        // Load dữ liệu
        loadSavedRecipients();

        setupSearch();

        // Nút thêm người nhận mới
        btnAddNew.setOnClickListener(v -> {
            Intent intent = new Intent(TransferActivity.this, TransferSelectBankActivity.class);
            startActivity(intent);
        });

        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSavedRecipients();
        if (edtSearch != null) edtSearch.setText("");
    }

    // Tìm kiếm người nhận
    private void setupSearch() {
        if (edtSearch == null) return;

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterRecipients(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterRecipients(String query) {
        List<SavedRecipientAdapter.SavedRecipient> filteredList = new ArrayList<>();

        if (query == null || query.isEmpty()) {
            filteredList.addAll(originalRecipientList);
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            for (SavedRecipientAdapter.SavedRecipient item : originalRecipientList) {
                // Tìm theo Tên hoặc Số tài khoản
                if (item.name.toLowerCase().contains(lowerCaseQuery) ||
                        item.accountNumber.contains(lowerCaseQuery)) {
                    filteredList.add(item);
                }
            }
        }

        // Cập nhật Adapter
        adapter.updateList(filteredList);
    }

    // Xóa người nhận
    private void showActionMenu(SavedRecipientAdapter.SavedRecipient recipient, View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenu().add("Xóa người nhận"); // Thêm option vào menu

        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Xóa người nhận")) {
                showDeleteConfirmDialog(recipient);
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

    private void showDeleteConfirmDialog(SavedRecipientAdapter.SavedRecipient recipient) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa " + recipient.name + " khỏi danh bạ thụ hưởng?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    deleteRecipient(recipient);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteRecipient(SavedRecipientAdapter.SavedRecipient recipient) {
        SharedPreferences prefs = getSharedPreferences("SavedRecipients", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(recipient.accountNumber); // Key là số tài khoản
        editor.apply();

        originalRecipientList.removeIf(r -> r.accountNumber.equals(recipient.accountNumber));

        filterRecipients(edtSearch.getText().toString());

        Toast.makeText(this, "Đã xóa người nhận", Toast.LENGTH_SHORT).show();
    }

    private void loadSavedRecipients() {
        recipientList.clear();
        originalRecipientList.clear();

        SharedPreferences prefs = getSharedPreferences("SavedRecipients", MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String accountNumber = entry.getKey();
            Object valueObj = entry.getValue();

            if (valueObj instanceof String) {
                String value = (String) valueObj;
                String name = value;
                String userId = "";

                if (value.contains(";")) {
                    String[] parts = value.split(";");
                    name = parts[0];
                    if (parts.length > 1) {
                        userId = parts[1];
                    }
                }

                originalRecipientList.add(new SavedRecipientAdapter.SavedRecipient(accountNumber, name, userId));
            }
        }
        recipientList.addAll(originalRecipientList);
        adapter.notifyDataSetChanged();
    }
}
