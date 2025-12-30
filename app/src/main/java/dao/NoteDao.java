package com.calculator.notepadapp.dao;

import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.calculator.notepadapp.model.Note;
import java.util.List;

@Dao
public interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertNote(Note note);

    @Update
    void update(Note note);

    @Delete
    void delete(Note note);

    @Query("DELETE FROM Note WHERE id = :id")
    void deleteById(int id);

    @Query("SELECT * FROM Note WHERE isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    LiveData<List<Note>> getAllNotes();

    @Query("SELECT * FROM Note WHERE (title LIKE :search OR content LIKE :search) AND isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    LiveData<List<Note>> searchNotes(String search);

    @WorkerThread
    @Query("SELECT * FROM Note WHERE id = :id LIMIT 1")
    Note getNoteById(int id);
    @WorkerThread
    @Query("SELECT COUNT(*) FROM Note WHERE title = :title AND isDeleted = 0")
    long countNotesByTitle(String title);

    @Query("SELECT * FROM Note WHERE categoryId = :categoryId AND isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    LiveData<List<Note>> getNotesByCategory(int categoryId);

    @WorkerThread
    @Query("SELECT COUNT(*) FROM Note WHERE categoryId = :categoryId AND isDeleted = 0")
    long countNotesByCategory(int categoryId);

    @WorkerThread
    @Query("SELECT COUNT(*) FROM Note WHERE isDeleted = 0")
    Long countAllNotes();

    // 物理删除已在回收站中超过指定时间的笔记
    @WorkerThread
    @Query("DELETE FROM Note WHERE isDeleted = 1 AND deletedAt > 0 AND deletedAt < :threshold")
    int deleteExpiredDeletedNotes(long threshold);

}
