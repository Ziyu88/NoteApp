package com.calculator.notepadapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.calculator.notepadapp.R;
import com.calculator.notepadapp.model.Note;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class TrashNoteAdapter extends ListAdapter<Note, TrashNoteAdapter.TrashViewHolder> {

    private OnTrashActionListener actionListener;

    private static final DateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    private static final DiffUtil.ItemCallback<Note> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Note>() {
                @Override
                public boolean areItemsTheSame(@NonNull Note oldItem, @NonNull Note newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull Note oldItem, @NonNull Note newItem) {
                    return Objects.equals(oldItem.title, newItem.title)
                            && Objects.equals(oldItem.content, newItem.content)
                            && oldItem.deletedAt == newItem.deletedAt;
                }
            };

    public TrashNoteAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnTrashActionListener(OnTrashActionListener listener) {
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public TrashViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trash_note, parent, false);
        return new TrashViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrashViewHolder holder, int position) {
        Note note = getItem(position);
        holder.bind(note, actionListener);
    }

    static class TrashViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView content;
        TextView deletedAt;
        Button buttonRestore;
        Button buttonDeleteForever;

        TrashViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.textTrashTitle);
            content = itemView.findViewById(R.id.textTrashContent);
            deletedAt = itemView.findViewById(R.id.textTrashDeletedAt);
            buttonRestore = itemView.findViewById(R.id.buttonRestore);
            buttonDeleteForever = itemView.findViewById(R.id.buttonDeleteForever);
        }

        void bind(Note note, @Nullable OnTrashActionListener actionListener) {
            String titleText = (note.title != null && !note.title.trim().isEmpty()) ? note.title : "无标题";
            title.setText(titleText);

            if (note.content != null && !note.content.trim().isEmpty()) {
                content.setText(note.content);
                content.setVisibility(View.VISIBLE);
            } else {
                content.setText("");
                content.setVisibility(View.GONE);
            }
            if (note.deletedAt > 0) {
                deletedAt.setText("删除时间：" + DATE_FORMAT.format(new Date(note.deletedAt)));
            } else {
                deletedAt.setText("删除时间：未知");
            }

            buttonRestore.setOnClickListener(v -> {
                if (actionListener != null && getBindingAdapterPosition() != RecyclerView.NO_POSITION) {
                    actionListener.onRestore(note);
                }
            });

            buttonDeleteForever.setOnClickListener(v -> {
                if (actionListener != null && getBindingAdapterPosition() != RecyclerView.NO_POSITION) {
                    actionListener.onDeleteForever(note);
                }
            });
        }
    }

    public interface OnTrashActionListener {
        void onRestore(Note note);

        void onDeleteForever(Note note);
    }
}
