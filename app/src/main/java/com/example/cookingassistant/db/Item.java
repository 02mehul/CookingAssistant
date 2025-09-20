package com.example.cookingassistant.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "items")
public class Item {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;

    public int quantity;

    // Optional fields to look pro (can be null)
    public String category;     // e.g., "Dairy", "Vegetable"
    public Long expiryEpoch;    // ms since epoch; null if not set

    // Convenience constructor
    public Item(String name, int quantity) {
        this.name = name;
        this.quantity = quantity;
    }
}
