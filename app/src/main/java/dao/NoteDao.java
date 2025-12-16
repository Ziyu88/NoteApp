package com.calculator.notepadapp.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.calculator.notepadapp.model.Note;
import java.util.List;

@Dao
public interface NoteDao {
    @Insert
    void insert(Note note);

    @Update
    void update(Note note);

    @Delete
    void delete(Note note);

    @Insert
    void insertNote(Note note);

    @Query("SELECT * FROM Note WHERE isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    LiveData<List<Note>> getAllNotes();

    @Query("SELECT * FROM Note WHERE (title LIKE :search OR content LIKE :search) AND isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    LiveData<List<Note>> searchNotes(String search);

    @Query("SELECT * FROM Note WHERE id = :id")
    Note getNoteById(int id);

    @Query("SELECT COUNT(*) FROM Note WHERE title = :title AND isDeleted = 0")
    int countNotesByTitle(String title);

    @Query("SELECT * FROM Note WHERE categoryId = :categoryId AND isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    LiveData<List<Note>> getNotesByCategory(int categoryId);

    @Query("SELECT COUNT(*) FROM Note WHERE categoryId = :categoryId AND isDeleted = 0")
    int countNotesByCategory(int categoryId);

    @Query("SELECT COUNT(*) FROM Note WHERE isDeleted = 0")
    int countAllNotes();

    // 物理删除已在回收站中超过指定时间的笔记
    @Query("DELETE FROM Note WHERE isDeleted = 1 AND deletedAt > 0 AND deletedAt < :threshold")
    void deleteExpiredDeletedNotes(long threshold);

}
