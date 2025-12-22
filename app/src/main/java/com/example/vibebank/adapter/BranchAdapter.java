package com.example.vibebank.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android:view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vibebank.R;
import com.example.vibebank.model.Branch;

import java.util.List;

public class BranchAdapter extends RecyclerView.Adapter<BranchAdapter.BranchViewHolder> {

    private List<Branch> branches;
    private OnBranchClickListener listener;

    public interface OnBranchClickListener {
        void onBranchClick(Branch branch);
        void onDirectionClick(Branch branch);
    }

    public BranchAdapter(List<Branch> branches, OnBranchClickListener listener) {
        this.branches = branches;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BranchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_branch, parent, false);
        return new BranchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BranchViewHolder holder, int position) {
        Branch branch = branches.get(position);
        holder.bind(branch, listener);
    }

    @Override
    public int getItemCount() {
        return branches.size();
    }

    public void updateBranches(List<Branch> newBranches) {
        this.branches = newBranches;
        notifyDataSetChanged();
    }

    static class BranchViewHolder extends RecyclerView.ViewHolder {
        TextView tvBranchName, tvAddress, tvDistance;
        ImageView btnDirection;

        public BranchViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBranchName = itemView.findViewById(R.id.tvBranchName);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            btnDirection = itemView.findViewById(R.id.btnDirection);
        }

        public void bind(Branch branch, OnBranchClickListener listener) {
            tvBranchName.setText(branch.getName());
            tvAddress.setText(branch.getAddress());
            tvDistance.setText(branch.getDistanceText());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBranchClick(branch);
                }
            });

            btnDirection.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDirectionClick(branch);
                }
            });
        }
    }
}

