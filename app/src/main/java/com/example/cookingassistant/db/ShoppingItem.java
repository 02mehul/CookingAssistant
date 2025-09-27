package com.example.cookingassistant.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "shopping_items")
public class ShoppingItem {
    @PrimaryKey(autoGenerate = true) public long id;
    @NonNull public String name = "";
    public String measure;
    public int quantity = 1;
    public boolean checked = false;
}
