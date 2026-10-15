package com.vedant.safepath.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vedant.safepath.Model.CategorySection;
import com.vedant.safepath.R;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private Context context;
    private List<CategorySection> categoryList;

    public CategoryAdapter(Context context, List<CategorySection> categoryList) {
        this.context = context;
        this.categoryList = categoryList;
    }

    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView categoryTitle;
        RecyclerView categoryRecycler;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryTitle = itemView.findViewById(R.id.categoryTitle);
            categoryRecycler = itemView.findViewById(R.id.categoryRecycler);
        }
    }


    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category_section, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategorySection section = categoryList.get(position);
        holder.categoryTitle.setText(section.getCategoryTitle());

        LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        holder.categoryRecycler.setLayoutManager(layoutManager);

        if (section.getVideoList() == null) {
            ShimmerAdapter shimmerAdapter = new ShimmerAdapter();
            holder.categoryRecycler.setAdapter(shimmerAdapter);
        } else {
            VideoAdapter videoAdapter = new VideoAdapter(context, section.getVideoList());
            holder.categoryRecycler.setAdapter(videoAdapter);
        }
    }


    @Override
    public int getItemCount() {
        return categoryList.size();
    }
}
