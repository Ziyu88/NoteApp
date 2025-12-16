package com.calculator.notepadapp.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Note") // 明确指定表名
public class Note {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String content;

    // 与现有表结构保持一致：包含 createdAt 列
    public long createdAt;

    // 与现有表结构保持一致：updatedAt 无默认值
    public long updatedAt;

    // isPinned 在迁移中被设置了默认 0，保留默认值注解
    @ColumnInfo(defaultValue = "0")
    public boolean isPinned;

    // isDeleted 在表结构中没有默认值，不加 defaultValue
    public boolean isDeleted;

    // categoryId 在表结构中没有默认值，不加 defaultValue
    public int categoryId;

    // deletedAt 通过迁移添加，默认 0，与表结构一致
    @ColumnInfo(defaultValue = "0")
    public long deletedAt;

    // --- Getter 和 Setter 方法 ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(boolean pinned) {
        isPinned = pinned;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public long getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(long deletedAt) {
        this.deletedAt = deletedAt;
    }
}
