package com.calculator.notepadapp.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.calculator.notepadapp.model.Category;
import java.util.List;

@Dao
public interface CategoryDao {
    @Insert
    void insert(Category category);

    @Update
    void update(Category category);

    @Delete
    void delete(Category category);

    @Query("SELECT * FROM Category WHERE isDeleted = 0 ORDER BY createdAt ASC")
    LiveData<List<Category>> getAllCategories();

    @Query("SELECT * FROM Category WHERE id = :id")
    Category getCategoryById(int id);

    @Query("SELECT COUNT(*) FROM Category WHERE name = :name AND isDeleted = 0")
    int countCategoriesByName(String name);

    @Query("SELECT COUNT(*) FROM Note WHERE categoryId = :categoryId AND isDeleted = 0")
    int countNotesByCategory(int categoryId);
}

