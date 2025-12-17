package com.calculator.notepadapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.widget.Button;
import android.widget.Toast;

import com.calculator.notepadapp.dao.NoteDao;
import com.calculator.notepadapp.model.Note;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * 笔记删除管理类
 * 负责处理单项删除和批量删除功能
 */
public class NoteDeleteManager {

    private Context context;
    private NoteDao noteDao;
    private ExecutorService executor;

    public NoteDeleteManager(Context context, NoteDao noteDao, ExecutorService executor) {
        this.context = context;
        this.noteDao = noteDao;
        this.executor = executor;
    }

    /**
     * 删除单个笔记（带确认对话框，删除按钮标红）
     */
    public void deleteSingleNote(Note note, OnDeleteListener listener) {
        if (note == null) {
            return;
        }

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("删除笔记")
                .setMessage("确定要删除笔记 \"" + note.title + "\" 吗？")
                .setPositiveButton("确认删除", (dialogInterface, which) -> {
                    // 在后台线程执行删除操作（软删除，进入回收站）
                    executor.execute(() -> {
                        note.isDeleted = true;
                        note.deletedAt = System.currentTimeMillis();
                        noteDao.update(note);
                        // 回到主线程显示提示
                        if (context instanceof MainActivity) {
                            ((MainActivity) context).runOnUiThread(() -> {
                                Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show();
                                if (listener != null) {
                                    listener.onDeleteSuccess();
                                }
                            });
                        }
                    });
                })
                .setNegativeButton("取消", (dialogInterface, which) -> {
                    // 取消删除，恢复item位置
                    if (listener != null) {
                        listener.onDeleteCancelled();
                    }
                })
                .create();

        dialog.show();

        // 将"确认删除"按钮设置为红色
        Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (positiveButton != null) {
            positiveButton.setTextColor(Color.RED);
        }
    }

    /**
     * 删除单个笔记（不带确认对话框，用于滑动删除）
     */
    public void deleteSingleNoteDirectly(Note note, OnDeleteListener listener) {
        if (note == null) {
            return;
        }

        executor.execute(() -> {
            note.isDeleted = true;
            note.deletedAt = System.currentTimeMillis();
            noteDao.update(note);
            if (context instanceof MainActivity) {
                ((MainActivity) context).runOnUiThread(() -> {
                    Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onDeleteSuccess();
                    }
                });
            }
        });
    }

    /**
     * 批量删除笔记（带确认对话框，删除按钮标红）
     */
    public void deleteMultipleNotes(List<Note> notes, OnDeleteListener listener) {
        if (notes == null || notes.isEmpty()) {
            Toast.makeText(context, "请选择要删除的笔记", Toast.LENGTH_SHORT).show();
            return;
        }

        int count = notes.size();
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("批量删除")
                .setMessage("确定要删除选中的 " + count + " 条笔记吗？")
                .setPositiveButton("确认删除", (dialogInterface, which) -> {
                    // 在后台线程执行批量删除（软删除，进入回收站）
                    executor.execute(() -> {
                        for (Note note : notes) {
                            note.isDeleted = true;
                            note.deletedAt = System.currentTimeMillis();
                            noteDao.update(note);
                        }
                        // 回到主线程显示提示
                        if (context instanceof MainActivity) {
                            ((MainActivity) context).runOnUiThread(() -> {
                                Toast.makeText(context, "已删除 " + count + " 条笔记", Toast.LENGTH_SHORT).show();
                                if (listener != null) {
                                    listener.onDeleteSuccess();
                                }
                            });
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .create();

        dialog.show();

        // 将"确认删除"按钮设置为红色
        Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (positiveButton != null) {
            positiveButton.setTextColor(Color.RED);
        }
    }

    /**
     * 删除回调接口
     */
    public interface OnDeleteListener {
        void onDeleteSuccess();

        default void onDeleteCancelled() {
            // 默认空实现
        }
    }
}

