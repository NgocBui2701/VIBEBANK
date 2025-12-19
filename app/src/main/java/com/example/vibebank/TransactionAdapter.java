package com.example.vibebank;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private List<TransactionHistoryActivity.Transaction> transactions;
    private Context context;
    private String lastDateHeader = "";

    public TransactionAdapter(List<TransactionHistoryActivity.Transaction> transactions, Context context) {
        this.transactions = transactions;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransactionHistoryActivity.Transaction transaction = transactions.get(position);

        // Show date header if different from previous
        String currentDateHeader = transaction.getDateHeader();
        if (position == 0 || !currentDateHeader.equals(lastDateHeader)) {
            holder.txtDateHeader.setVisibility(View.VISIBLE);
            holder.txtDateHeader.setText(currentDateHeader);
            lastDateHeader = currentDateHeader;
        } else {
            holder.txtDateHeader.setVisibility(View.GONE);
        }

        // Set transaction details
        holder.txtName.setText(transaction.getName());
        holder.txtDescription.setText(transaction.getDescription());
        holder.txtTime.setText(transaction.getTimeString());

        // Format amount
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        String formattedAmount = formatter.format(Math.abs(transaction.getAmount()));
        
        if (transaction.isIncome()) {
            holder.txtAmount.setText("+ " + formattedAmount);
            holder.txtAmount.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
            holder.imgIcon.setImageResource(R.drawable.ic_arrow_down);
            holder.imgIcon.setColorFilter(context.getResources().getColor(android.R.color.holo_green_dark));
        } else {
            holder.txtAmount.setText("- " + formattedAmount);
            holder.txtAmount.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
            holder.imgIcon.setImageResource(R.drawable.ic_arrow_up);
            holder.imgIcon.setColorFilter(context.getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtDateHeader, txtName, txtDescription, txtTime, txtAmount;
        ImageView imgIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDateHeader = itemView.findViewById(R.id.txtDateHeader);
            txtName = itemView.findViewById(R.id.txtName);
            txtDescription = itemView.findViewById(R.id.txtDescription);
            txtTime = itemView.findViewById(R.id.txtTime);
            txtAmount = itemView.findViewById(R.id.txtAmount);
            imgIcon = itemView.findViewById(R.id.imgIcon);
        }
    }
}
