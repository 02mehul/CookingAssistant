package com.example.cookingassistant.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

// Bump version when schema changes
@Database(
        entities = { Item.class, ShoppingItem.class },  // <-- include ShoppingItem
        version = 3,                                     // <-- bump to 3
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract ItemDao itemDao();
    public abstract ShoppingItemDao shoppingItemDao();

    // Migration v1 -> v2: add columns on items
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE items ADD COLUMN category TEXT");
            db.execSQL("ALTER TABLE items ADD COLUMN expiryEpoch INTEGER");
        }
    };

    // Migration v2 -> v3: create shopping_items table
    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override public void migrate(SupportSQLiteDatabase db) {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `shopping_items` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`name` TEXT NOT NULL, " +
                            "`measure` TEXT, " +
                            "`quantity` INTEGER NOT NULL, " +
                            "`checked` INTEGER NOT NULL)"
            );
        }
    };

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "pantry.db")                 // keep your existing name
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                            // Dev alternative if you don't care about preserving data:
                            // .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
