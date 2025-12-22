package com.example.vibebank;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter cho RecyclerView hiển thị lịch sử mã rút tiền
 */
public class WithdrawCodeHistoryAdapter extends RecyclerView.Adapter<WithdrawCodeHistoryAdapter.ViewHolder> {
    
    private Context context;
    private List<WithdrawCodeHistoryActivity.WithdrawCodeItem> items;
    
    public WithdrawCodeHistoryAdapter(Context context, List<WithdrawCodeHistoryActivity.WithdrawCodeItem> items) {
        this.context = context;
        this.items = items;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_withdraw_code, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WithdrawCodeHistoryActivity.WithdrawCodeItem item = items.get(position);
        
        holder.tvCode.setText(item.code != null ? item.code : "");
        holder.tvAmount.setText(item.getFormattedAmount());
        holder.tvDate.setText(item.getFormattedDate());
        holder.tvStatus.setText(item.status != null ? item.status : "Đã rút");
    }
    
    @Override
    public int getItemCount() {
        return items.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCode;
        TextView tvAmount;
        TextView tvDate;
        TextView tvStatus;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvCode = itemView.findViewById(R.id.tvCode);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
