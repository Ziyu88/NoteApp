package com.calculator.notepadapp;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 提供与生命周期绑定的数据库线程执行器，避免在 Activity/Fragment 中直接 new Thread。
 */
public class DatabaseViewModel extends AndroidViewModel {

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    public DatabaseViewModel(@NonNull Application application) {
        super(application);
    }

    /**
     * 在单线程的后台执行数据库操作。
     */
    public void execute(Runnable task) {
        ioExecutor.execute(task);
    }

    public ExecutorService getExecutor() {
        return ioExecutor;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        ioExecutor.shutdown();
    }
}