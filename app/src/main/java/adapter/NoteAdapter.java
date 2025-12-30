package com.calculator.notepadapp.adapter;

import android.animation.ObjectAnimator;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.calculator.notepadapp.R;
import com.calculator.notepadapp.model.Note;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private List<Note> noteList;
    private boolean isMultiSelectMode = false;
    private Set<Integer> selectedPositions = new HashSet<>();
    private OnItemClickListener itemClickListener;
    private OnItemLongClickListener itemLongClickListener;
    private OnDeleteClickListener deleteClickListener;
    // 当前搜索高亮关键字
    private String highlightQuery = "";

    public void setNoteList(List<Note> notes) {
        this.noteList = notes;
        notifyDataSetChanged();
    }

    /**
     * 设置当前需要高亮的搜索关键字
     */
    public void setHighlightQuery(String query) {
        if (query == null) {
            query = "";
        }
        this.highlightQuery = query.trim();
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.itemLongClickListener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteClickListener = listener;
    }

    /**
     * 获取指定位置的笔记
     */
    public Note getNoteAt(int position) {
        if (noteList != null && position >= 0 && position < noteList.size()) {
            return noteList.get(position);
        }
        return null;
    }

    /**
     * 进入多选模式
     */
    public void enterMultiSelectMode() {
        isMultiSelectMode = true;
        selectedPositions.clear();
        notifyDataSetChanged();
    }

    /**
     * 退出多选模式
     */
    public void exitMultiSelectMode() {
        isMultiSelectMode = false;
        selectedPositions.clear();
        notifyDataSetChanged();
    }

    /**
     * 是否处于多选模式
     */
    public boolean isMultiSelectMode() {
        return isMultiSelectMode;
    }

    /**
     * 切换选中状态
     */
    public void toggleSelection(int position) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position);
        } else {
            selectedPositions.add(position);
        }
        notifyItemChanged(position);
    }

    /**
     * 获取选中的笔记列表
     */
    public List<Note> getSelectedNotes() {
        List<Note> selected = new ArrayList<>();
        for (int position : selectedPositions) {
            if (position < noteList.size()) {
                selected.add(noteList.get(position));
            }
        }
        return selected;
    }

    /**
     * 获取选中数量
     */
    public int getSelectedCount() {
        return selectedPositions.size();
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView title, content, time, wordCount;
        CheckBox checkBox;
        View foregroundLayout;
        FrameLayout deleteButton;
        private float initialX = 0;
        private boolean isSwiping = false;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.textViewTitle);
            content = itemView.findViewById(R.id.textViewContent);
            time = itemView.findViewById(R.id.textViewTime);
            wordCount = itemView.findViewById(R.id.textViewWordCount);
            checkBox = itemView.findViewById(R.id.checkBoxSelect);
            foregroundLayout = itemView.findViewById(R.id.foregroundLayout);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }

        public void bind(Note note, boolean isMultiSelectMode, boolean isSelected, String highlightQuery) {
            // 标题：为空时显示“无标题”
            String titleText = (note.title != null && !note.title.trim().isEmpty()) ? note.title : "无标题";
            setHighlightedText(title, titleText, highlightQuery);

            // 内容预览：最多两行，item布局已做ellipsize
            if (content != null) {
                if (note.content != null && !note.content.trim().isEmpty()) {
                    setHighlightedText(content, note.content, highlightQuery);
                    content.setVisibility(View.VISIBLE);
                } else {
                    content.setText("");
                    content.setVisibility(View.GONE);
                }
            }

            // 显示更新时间
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            time.setText(sdf.format(new Date(note.updatedAt)));

            // 计算并显示字数
            int totalWords = 0;
            if (note.title != null) {
                totalWords += note.title.length();
            }
            if (note.content != null) {
                totalWords += note.content.length();
            }
            wordCount.setText(totalWords + "字");

            // 显示或隐藏复选框
            if (checkBox != null) {
                checkBox.setVisibility(isMultiSelectMode ? View.VISIBLE : View.GONE);
                checkBox.setChecked(isSelected);
            }

            // 设置卡片背景色：使用主题颜色，支持日间/夜间模式
            int cardColor = ContextCompat.getColor(itemView.getContext(), R.color.background_card);
            foregroundLayout.setBackgroundColor(cardColor);

            // 重置滑动位置
            resetSwipe();
        }

        public void setupSwipeGesture(OnDeleteClickListener deleteClickListener, OnItemClickListener itemClickListener, Note note, int position) {
            // 计算删除按钮的实际宽度（60dp按钮 + 16dp右边距 + 20dp额外空间）
            float density = itemView.getContext().getResources().getDisplayMetrics().density;
            float maxSwipeDistance = (60 + 16 + 20) * density; // 转换为像素

            final float[] startX = {0};
            final float[] startY = {0};
            final float[] lastX = {0};
            final long[] downTime = {0};

            foregroundLayout.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX[0] = event.getRawX();
                        startY[0] = event.getRawY();
                        lastX[0] = event.getRawX();
                        downTime[0] = System.currentTimeMillis();
                        isSwiping = false;
                        return false; // 返回false，让长按事件能够触发

                    case MotionEvent.ACTION_MOVE:
                        float currentX = event.getRawX();
                        float deltaX = currentX - lastX[0];
                        float currentTranslation = foregroundLayout.getTranslationX();
                        float newTranslation = currentTranslation + deltaX;

                        // 检测是否开始滑动
                        float totalDeltaX = Math.abs(currentX - startX[0]);
                        float totalDeltaY = Math.abs(event.getRawY() - startY[0]);

                        // 只有水平滑动距离超过10px且大于垂直滑动时，才认为是滑动
                        if (totalDeltaX > 10 && totalDeltaX > totalDeltaY) {
                            isSwiping = true;
                            // 开始滑动时取消长按，避免误触发多选模式
                            foregroundLayout.cancelLongPress();
                            foregroundLayout.getParent().requestDisallowInterceptTouchEvent(true);
                        }

                        // 如果正在滑动，处理滑动逻辑
                        if (isSwiping) {
                            // 限制滑动范围：0 到 -maxSwipeDistance
                            if (newTranslation <= 0 && newTranslation >= -maxSwipeDistance) {
                                foregroundLayout.setTranslationX(newTranslation);
                            } else if (newTranslation > 0) {
                                foregroundLayout.setTranslationX(0);
                            } else if (newTranslation < -maxSwipeDistance) {
                                foregroundLayout.setTranslationX(-maxSwipeDistance);
                            }

                            lastX[0] = currentX;
                            return true; // 拦截事件
                        }

                        lastX[0] = currentX;
                        return false; // 不拦截，让其他事件（如长按）能够触发

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        long clickDuration = System.currentTimeMillis() - downTime[0];
                        float totalMovement = Math.abs(event.getRawX() - startX[0]);

                        // 判断是点击还是滑动
                        if (!isSwiping && clickDuration < 200 && totalMovement < 10) {
                            // 这是一个点击事件
                            if (itemClickListener != null) {
                                itemClickListener.onItemClick(note, position);
                            }
                            return true;
                        }

                        if (isSwiping) {
                            float finalTranslation = foregroundLayout.getTranslationX();

                            // 松手时的逻辑（降低阈值到25%）
                            if (finalTranslation < -maxSwipeDistance * 0.25f) {
                                // 滑动超过25%，保持显示删除按钮
                                animateToPosition(-maxSwipeDistance);
                            } else {
                                // 滑动少于25%，回弹到原位
                                resetSwipe();
                            }
                            isSwiping = false;
                            return true; // 滑动时拦截事件
                        }
                        return false; // 没有滑动，不拦截事件
                }
                return false;
            });

            // 删除按钮点击事件
            deleteButton.setOnClickListener(v -> {
                if (deleteClickListener != null) {
                    deleteClickListener.onDeleteClick(note, position);
                }
            });
        }

        private void animateToPosition(float position) {
            if (foregroundLayout != null) {
                ObjectAnimator animator = ObjectAnimator.ofFloat(foregroundLayout, "translationX", position);
                animator.setDuration(200);
                animator.start();
            }
        }

        public void resetSwipe() {
            animateToPosition(0);
        }

        /**
         * 将 textView 中和关键字匹配的部分高亮显示
         */
        private void setHighlightedText(TextView textView, String fullText, String query) {
            if (TextUtils.isEmpty(fullText) || TextUtils.isEmpty(query)) {
                textView.setText(fullText);
                return;
            }

            String lowerText = fullText.toLowerCase(Locale.getDefault());
            String lowerQuery = query.toLowerCase(Locale.getDefault());

            SpannableString spannable = new SpannableString(fullText);
            int index = lowerText.indexOf(lowerQuery);
            int highlightColor = ContextCompat.getColor(textView.getContext(), R.color.text_highlight);

            while (index >= 0) {
                int end = index + query.length();
                spannable.setSpan(new ForegroundColorSpan(highlightColor), index, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                index = lowerText.indexOf(lowerQuery, end);
            }

            textView.setText(spannable);
        }
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = noteList.get(position);
        boolean isSelected = selectedPositions.contains(position);
        holder.bind(note, isMultiSelectMode, isSelected, highlightQuery);

        // 设置滑动手势和删除按钮
        if (!isMultiSelectMode) {
            // 清除多选模式的点击监听器
            holder.foregroundLayout.setOnClickListener(null);
            if (holder.checkBox != null) {
                holder.checkBox.setOnClickListener(null);
            }

            holder.setupSwipeGesture(deleteClickListener, itemClickListener, note, position);
        } else {
            // 清除滑动手势的触摸监听器
            holder.foregroundLayout.setOnTouchListener(null);

            // 多选模式下，点击整个笔记项或复选框都能选中
            View.OnClickListener selectListener = v -> {
                toggleSelection(position);
                if (itemClickListener != null) {
                    itemClickListener.onItemClick(note, position);
                }
            };

            // 点击整个笔记项
            holder.foregroundLayout.setOnClickListener(selectListener);

            // 点击复选框（复选框会自动切换状态，但我们需要同步到selectedPositions）
            if (holder.checkBox != null) {
                holder.checkBox.setOnClickListener(selectListener);
            }
        }

        // 设置长按事件
        holder.foregroundLayout.setOnLongClickListener(v -> {
            if (!isMultiSelectMode && itemLongClickListener != null) {
                itemLongClickListener.onItemLongClick(note, position);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return noteList == null ? 0 : noteList.size();
    }

    /**
     * 点击事件接口
     */
    public interface OnItemClickListener {
        void onItemClick(Note note, int position);
    }

    /**
     * 长按事件接口
     */
    public interface OnItemLongClickListener {
        void onItemLongClick(Note note, int position);
    }

    /**
     * 删除按钮点击事件接口
     */
    public interface OnDeleteClickListener {
        void onDeleteClick(Note note, int position);
    }
}
