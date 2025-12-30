package com.calculator.notepadapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.calculator.notepadapp.R;
import com.calculator.notepadapp.model.Note;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TrashNoteAdapter extends RecyclerView.Adapter<TrashNoteAdapter.TrashViewHolder> {

    private List<Note> noteList;
    private OnTrashActionListener actionListener;

    public void setNoteList(Object notes) {
        this.noteList = notes;
        notifyDataSetChanged();
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
        Note note = noteList.get(position);
        holder.bind(note);

        holder.buttonRestore.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onRestore(note);
            }
        });

        holder.buttonDeleteForever.setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onDeleteForever(note);
            }
        });
    }

    @Override
    public int getItemCount() {
        return noteList == null ? 0 : noteList.size();
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

        void bind(Note note) {
            String titleText = (note.title != null && !note.title.trim().isEmpty()) ? note.title : "无标题";
            title.setText(titleText);

            if (note.content != null && !note.content.trim().isEmpty()) {
                content.setText(note.content);
                content.setVisibility(View.VISIBLE);
            } else {
                content.setText("");
                content.setVisibility(View.GONE);
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            if (note.deletedAt > 0) {
                deletedAt.setText("删除时间：" + sdf.format(new Date(note.deletedAt)));
            } else {
                deletedAt.setText("删除时间：未知");
            }
        }
    }

    public interface OnTrashActionListener {
        void onRestore(Note note);

        void onDeleteForever(Note note);
    }
}
