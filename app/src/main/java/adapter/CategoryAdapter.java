package com.calculator.notepadapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.calculator.notepadapp.R;
import com.calculator.notepadapp.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<Category> categoryList = new ArrayList<>();
    private List<Long> noteCounts = new ArrayList<>();
    private OnCategoryClickListener clickListener;
    private int selectedCategoryId = -1; // 当前选中的分类ID

    public void setCategoryList(List<Category> categories, List<Long> counts) {
        this.categoryList = categories;
        this.noteCounts = counts;
        notifyDataSetChanged();
    }

    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.clickListener = listener;
    }

    public void setSelectedCategoryId(int categoryId) {
        this.selectedCategoryId = categoryId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categoryList.get(position);
        long noteCount = position < noteCounts.size() ? noteCounts.get(position) : 0L;

        holder.categoryName.setText(category.name);
        holder.noteCount.setText(String.valueOf(noteCount));

        // 显示或隐藏选中图标
        if (category.id == selectedCategoryId) {
            holder.checkIcon.setVisibility(View.VISIBLE);
        } else {
            holder.checkIcon.setVisibility(View.GONE);
        }

        // 点击事件
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onCategoryClick(category);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView categoryName, noteCount;
        ImageView checkIcon;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryName = itemView.findViewById(R.id.textViewCategoryName);
            noteCount = itemView.findViewById(R.id.textViewNoteCount);
            checkIcon = itemView.findViewById(R.id.imageViewCheck);
        }
    }

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }
}

