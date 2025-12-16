package com.calculator.notepadapp;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.calculator.notepadapp.dao.NoteDao;

/**
 * 定期清理回收站中超过保留时间的笔记
 */
public class TrashCleanupWorker extends Worker {

    // 30 天（毫秒）
    private static final long RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1000;

    public TrashCleanupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            long now = System.currentTimeMillis();
            long threshold = now - RETENTION_MILLIS;

            NoteDatabase db = NoteDatabase.getInstance(getApplicationContext());
            NoteDao noteDao = db.noteDao();
            noteDao.deleteExpiredDeletedNotes(threshold);

            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.retry();
        }
    }
}
