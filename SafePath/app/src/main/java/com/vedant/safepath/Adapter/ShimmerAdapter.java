package com.vedant.safepath.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.vedant.safepath.R;

public class ShimmerAdapter extends RecyclerView.Adapter<ShimmerAdapter.ShimmerViewHolder> {

    private int shimmerItemCount = 5;

    public static class ShimmerViewHolder extends RecyclerView.ViewHolder {
        public ShimmerViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    @NonNull
    @Override
    public ShimmerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.shimmer_video_placeholder, parent, false);
        return new ShimmerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShimmerViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return shimmerItemCount;
    }
}

