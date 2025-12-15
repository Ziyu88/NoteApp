package com.calculator.notepadapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.calculator.notepadapp.adapter.NoteAdapter;

/**
 * 滑动删除回调类
 * 实现向左滑动显示删除背景和图标
 */
public class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {

    private NoteAdapter adapter;
    private Drawable deleteIcon;
    private ColorDrawable background;
    private Context context;
    private OnSwipeListener swipeListener;

    public SwipeToDeleteCallback(Context context, NoteAdapter adapter) {
        super(0, ItemTouchHelper.LEFT);
        this.context = context;
        this.adapter = adapter;
        
        // 设置删除图标
        deleteIcon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_delete);
        if (deleteIcon != null) {
            deleteIcon.setTint(Color.WHITE);
        }
        
        // 设置红色背景
        background = new ColorDrawable(Color.RED);
    }

    public void setOnSwipeListener(OnSwipeListener listener) {
        this.swipeListener = listener;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, 
                         @NonNull RecyclerView.ViewHolder viewHolder, 
                         @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        if (swipeListener != null) {
            // 显示删除按钮，而不是直接删除
            swipeListener.onSwipeToDelete(position);
        }
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        // 设置滑动阈值为30%，更容易触发
        return 0.3f;
    }

    @Override
    public void onChildDraw(@NonNull Canvas c,
                           @NonNull RecyclerView recyclerView,
                           @NonNull RecyclerView.ViewHolder viewHolder,
                           float dX, float dY,
                           int actionState,
                           boolean isCurrentlyActive) {

        View itemView = viewHolder.itemView;
        int itemHeight = itemView.getBottom() - itemView.getTop();

        // 只在向左滑动时绘制
        if (dX < 0) {
            // 计算圆形按钮的位置和大小
            int buttonSize = (int) (itemHeight * 0.6); // 圆形按钮大小为item高度的60%
            int buttonMargin = (itemHeight - buttonSize) / 2;

            // 圆心位置
            int centerX = itemView.getRight() - buttonMargin - buttonSize / 2;
            int centerY = itemView.getTop() + itemHeight / 2;
            int radius = buttonSize / 2;

            // 绘制红色圆形背景
            Paint circlePaint = new Paint();
            circlePaint.setColor(Color.RED);
            circlePaint.setAntiAlias(true);
            c.drawCircle(centerX, centerY, radius, circlePaint);

            // 绘制删除图标（垃圾桶）
            if (deleteIcon != null) {
                int iconSize = (int) (buttonSize * 0.5); // 图标大小为按钮的50%
                int iconLeft = centerX - iconSize / 2;
                int iconTop = centerY - iconSize / 2;
                int iconRight = iconLeft + iconSize;
                int iconBottom = iconTop + iconSize;

                deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                deleteIcon.draw(c);
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    /**
     * 滑动监听接口
     */
    public interface OnSwipeListener {
        void onSwipeToDelete(int position);
    }
}

