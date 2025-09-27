package com.example.cookingassistant.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ItemDao {

    // --- Sync fetch (blocks caller) ---
    @Query("SELECT * FROM items ORDER BY name ASC")
    List<Item> getAllSync();

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    Item findByIdSync(int id);

    // --- LiveData fetch (auto-updates UI) ---
    @Query("SELECT * FROM items ORDER BY name ASC")
    LiveData<List<Item>> getAllLive();

    // --- Insert / Update / Delete ---
    @Insert
    long insert(Item item);

    @Update
    void update(Item item);

    @Query("UPDATE items SET quantity = :qty WHERE id = :id")
    void updateQuantity(int id, int qty);

    @Query("SELECT * FROM items WHERE LOWER(name)=LOWER(:name) LIMIT 1")
    Item findByName(String name);
    @Delete
    void delete(Item item);

    @Query("DELETE FROM items")
    void clearAll();
}
