package com.example.cookingassistant.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ShoppingItemDao {
    @Query("SELECT * FROM shopping_items ORDER BY checked ASC, name ASC")
    List<ShoppingItem> getAllSync();

    @Query("DELETE FROM shopping_items")
    void clearAll();

    @Query("SELECT * FROM shopping_items WHERE LOWER(name)=LOWER(:name) LIMIT 1")
    ShoppingItem findByName(String name);

    @Insert
    long insert(ShoppingItem i);

    @Insert
    void insertAll(List<ShoppingItem> items);

    @Update
    void update(ShoppingItem i);

    @Delete
    void delete(ShoppingItem i);
}
