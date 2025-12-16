package com.calculator.notepadapp.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Category {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public long createdAt;
    public boolean isDeleted;

    public Category(String name, long createdAt) {
        this.name = name;
        this.createdAt = createdAt;
        this.isDeleted = false;
    }
}

