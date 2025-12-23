package com.example.vibebank.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibebank.R;
import com.example.vibebank.model.Customer;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder> {

    private List<Customer> customers;
    private OnCustomerClickListener listener;

    public interface OnCustomerClickListener {
        void onCustomerClick(Customer customer);
    }

    public CustomerAdapter(List<Customer> customers, OnCustomerClickListener listener) {
        this.customers = customers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CustomerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_customer, parent, false);
        return new CustomerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomerViewHolder holder, int position) {
        Customer customer = customers.get(position);
        holder.bind(customer, listener);
    }

    @Override
    public int getItemCount() {
        return customers.size();
    }

    public void updateCustomers(List<Customer> newCustomers) {
        this.customers = newCustomers;
        notifyDataSetChanged();
    }

    static class CustomerViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomerName, tvAccountNumber, tvKycStatus, tvBalance;
        ImageView imgCustomerAvatar;

        public CustomerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvAccountNumber = itemView.findViewById(R.id.tvAccountNumber);
            tvKycStatus = itemView.findViewById(R.id.tvKycStatus);
            tvBalance = itemView.findViewById(R.id.tvBalance);
            imgCustomerAvatar = itemView.findViewById(R.id.imgCustomerAvatar);
        }

        public void bind(Customer customer, OnCustomerClickListener listener) {
            tvCustomerName.setText(customer.getFullName());
            tvAccountNumber.setText("STK: " + customer.getAccountNumber());
            tvKycStatus.setText(customer.getKycStatusDisplay());
            
            // Set status color
            if ("verified".equals(customer.getKycStatus())) {
                tvKycStatus.setBackgroundColor(Color.parseColor("#4CAF50")); // Green
            } else if ("pending".equals(customer.getKycStatus())) {
                tvKycStatus.setBackgroundColor(Color.parseColor("#FF9800")); // Orange
            } else {
                tvKycStatus.setBackgroundColor(Color.parseColor("#F44336")); // Red
            }

            NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
            tvBalance.setText("Số dư: " + formatter.format(customer.getBalance()));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCustomerClick(customer);
                }
            });
        }
    }
}


