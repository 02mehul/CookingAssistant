package com.example.cookingassistant.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "items")
public class Item {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public String name;
    public int quantity;
    public String category; // optional
    public Long expiryEpoch; // optional

    public Item(String name, int quantity) {
        this.name = name; this.quantity = quantity;
    }
}
