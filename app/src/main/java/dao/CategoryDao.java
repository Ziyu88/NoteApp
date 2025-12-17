package com.calculator.notepadapp.dao;

import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.calculator.notepadapp.model.Category;
import java.util.List;

@Dao
public interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(Category category);

    @Update
    void update(Category category);

    @Delete
    void delete(Category category);

    @Query("SELECT * FROM Category WHERE isDeleted = 0 ORDER BY createdAt ASC")
    LiveData<List<Category>> getAllCategories();

    @WorkerThread

    @Query("SELECT COUNT(*) FROM Category WHERE name = :name AND isDeleted = 0")
    int countCategoriesByName(String name);


}

