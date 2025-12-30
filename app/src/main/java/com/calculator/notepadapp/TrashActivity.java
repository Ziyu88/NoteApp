package com.calculator.notepadapp;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.calculator.notepadapp.adapter.TrashNoteAdapter;
import com.calculator.notepadapp.model.Note;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrashActivity extends AppCompatActivity {
    private NoteDatabase noteDatabase;
    private TrashNoteAdapter adapter;
    private TextView textEmpty;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.applyTheme(this);
        setContentView(R.layout.activity_trash);

        noteDatabase = NoteDatabase.getInstance(this);

        ImageButton buttonBack = findViewById(R.id.buttonTrashBack);
        buttonBack.setOnClickListener(v -> finish());
        textEmpty = findViewById(R.id.textTrashEmpty);
        RecyclerView recyclerView = findViewById(R.id.recyclerViewTrash);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        adapter = new TrashNoteAdapter();
        recyclerView.setAdapter(adapter);

        adapter.setOnTrashActionListener(new TrashNoteAdapter.OnTrashActionListener() {
            @Override
            public void onRestore(Note note) {
                confirmRestore(note);
            }

            @Override
            public void onDeleteForever(Note note) {
                confirmDeleteForever(note);
            }
        });

        noteDatabase.noteDao().getDeletedNotes().observe(this, notes -> {
            List<Note> noteList = notes == null ? java.util.Collections.emptyList() : notes;
            adapter.setNoteList(noteList);
            boolean isEmpty = noteList.isEmpty();
            textEmpty.setVisibility(isEmpty ? TextView.VISIBLE : TextView.GONE);
        });
    }
    private void confirmRestore(Note note) {
        String title = (note.title != null && !note.title.trim().isEmpty()) ? note.title : "无标题";
        new AlertDialog.Builder(this)
                .setTitle("恢复笔记")
                .setMessage("确定要恢复笔记 \"" + title + "\" 吗？")
                .setPositiveButton("恢复", (dialog, which) -> restoreNote(note))
                .setNegativeButton("取消", null)
                .show();
    }
    private void restoreNote(Note note) {
        executor.execute(() -> {
            noteDatabase.noteDao().restoreNoteById(note.id);
            runOnUiThread(() -> showToastIfActive("已恢复"));
        });
    }

    private void confirmDeleteForever(Note note) {
        String title = (note.title != null && !note.title.trim().isEmpty()) ? note.title : "无标题";
        new AlertDialog.Builder(this)
                .setTitle("彻底删除")
                .setMessage("确定要彻底删除笔记 \"" + title + "\" 吗？")
                .setPositiveButton("删除", (dialog, which) -> deleteForever(note))
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteForever(Note note) {
        executor.execute(() -> {
            noteDatabase.noteDao().deleteById(note.id);
            runOnUiThread(() -> showToastIfActive("已彻底删除"));
        });
    }

    private void showToastIfActive(String message) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}