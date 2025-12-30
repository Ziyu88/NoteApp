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

    public void setNoteList(List<Note> notes) {
        int oldSize = noteList == null ? 0 : noteList.size();
        this.noteList = notes;
        int newSize = noteList == null ? 0 : noteList.size();
        if (oldSize == 0) {
            if (newSize > 0) {
                notifyItemRangeInserted(0, newSize);
            }
            return;
        }
        if (newSize == 0) {
            notifyItemRangeRemoved(0, oldSize);
            return;
        }
        int minSize = Math.min(oldSize, newSize);
        notifyItemRangeChanged(0, minSize);
        if (newSize > oldSize) {
            notifyItemRangeInserted(oldSize, newSize - oldSize);
        } else if (oldSize > newSize) {
            notifyItemRangeRemoved(newSize, oldSize - newSize);
        }
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
            String titleText = (note.title != null && !note.title.trim().isEmpty())
                    ? note.title
                    : itemView.getContext().getString(R.string.note_title_fallback);
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
                deletedAt.setText(itemView.getContext().getString(
                        R.string.trash_deleted_time,
                        sdf.format(new Date(note.deletedAt))
                ));
            } else {
                deletedAt.setText(itemView.getContext().getString(R.string.trash_deleted_time_unknown));
            }
        }
    }

    public interface OnTrashActionListener {
        void onRestore(Note note);

        void onDeleteForever(Note note);
    }
}
