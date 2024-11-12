package org.dieschnittstelle.mobile.android.skeleton.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import org.dieschnittstelle.mobile.android.skeleton.model.Todo;

@Database(entities = {Todo.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ITodoCRUDOperation todoCRUDOperation();
}
