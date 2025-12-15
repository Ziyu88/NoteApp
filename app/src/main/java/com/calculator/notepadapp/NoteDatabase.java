package com.calculator.notepadapp;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.calculator.notepadapp.dao.CategoryDao;
import com.calculator.notepadapp.dao.NoteDao;
import com.calculator.notepadapp.model.Category;
import com.calculator.notepadapp.model.Note;

// 修改点：在 @Database 注解中添加 exportSchema = false
@Database(entities = {Note.class, Category.class}, version = 4, exportSchema = false)
public abstract class NoteDatabase extends RoomDatabase {

    private static NoteDatabase instance;

    public abstract NoteDao noteDao();
    public abstract CategoryDao categoryDao();

    // 数据库迁移：从版本1到版本2
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 添加categoryId字段到Note表，默认值为0（未分类）
            database.execSQL("ALTER TABLE Note ADD COLUMN categoryId INTEGER NOT NULL DEFAULT 0");

            // 创建Category表
            database.execSQL("CREATE TABLE IF NOT EXISTS Category (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "name TEXT, " +
                    "createdAt INTEGER NOT NULL, " +
                    "isDeleted INTEGER NOT NULL)");
        }
    };

    // 数据库迁移：从版本3到版本4，添加 deletedAt 字段
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 添加 deletedAt 字段到 Note 表，默认值 0 表示未删除
            database.execSQL("ALTER TABLE Note ADD COLUMN deletedAt INTEGER NOT NULL DEFAULT 0");
        }
    };

    // 数据库迁移：从版本2到版本3
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // 添加isPinned字段到Note表，默认值为0（不置顶）
            database.execSQL("ALTER TABLE Note ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static synchronized NoteDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            NoteDatabase.class, "note_database")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4) // 添加迁移策略
                    .fallbackToDestructiveMigration() // 如果没有找到迁移路径，则清空数据库
                    .build();
        }
        return instance;
    }
}
