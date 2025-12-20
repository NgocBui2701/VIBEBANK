package com.example.vibebank;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SavedRecipientAdapter extends RecyclerView.Adapter<SavedRecipientAdapter.ViewHolder> {

    private List<SavedRecipient> recipientList;
    private OnItemClickListener listener;

    // Interface để xử lý sự kiện click
    public interface OnItemClickListener {
        void onItemClick(SavedRecipient recipient);
        void onEditClick(SavedRecipient recipient, View view);
    }

    // Class model đơn giản để chứa dữ liệu
    public static class SavedRecipient {
        String accountNumber;
        String name;
        String userId;

        public SavedRecipient(String accountNumber, String name, String userId) {
            this.accountNumber = accountNumber;
            this.name = name;
            this.userId = userId;
        }
    }

    public SavedRecipientAdapter(List<SavedRecipient> recipientList, OnItemClickListener listener) {
        this.recipientList = recipientList;
        this.listener = listener;
    }

    // Hàm cập nhật danh sách mới (Dùng cho tính năng tìm kiếm)
    public void updateList(List<SavedRecipient> newList) {
        this.recipientList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saved_recipients, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SavedRecipient recipient = recipientList.get(position);

        // Ánh xạ View
        TextView tvName = holder.itemView.findViewById(R.id.tvRecipientName);
        TextView tvDetail = holder.itemView.findViewById(R.id.tvRecipientDetail);
        ImageView btnEdit = holder.itemView.findViewById(R.id.btnEdit);

        // Set dữ liệu
        if (tvName != null) tvName.setText(recipient.name);
        else ((TextView)((android.widget.LinearLayout)((android.widget.LinearLayout)holder.itemView).getChildAt(1)).getChildAt(0)).setText(recipient.name);

        String detailText = recipient.accountNumber + ", VibeBank";
        if (tvDetail != null) tvDetail.setText(detailText);
        else ((TextView)((android.widget.LinearLayout)((android.widget.LinearLayout)holder.itemView).getChildAt(1)).getChildAt(1)).setText(detailText);

        holder.itemView.setOnClickListener(v -> listener.onItemClick(recipient));

        // Sự kiện click vào nút Edit (để hiện menu xóa)
        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> listener.onEditClick(recipient, v));
        }
    }

    @Override
    public int getItemCount() {
        return recipientList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}