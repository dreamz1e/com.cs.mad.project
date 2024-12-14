package org.dieschnittstelle.mobile.android.skeleton.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import org.dieschnittstelle.mobile.android.skeleton.model.Todo;
import org.dieschnittstelle.mobile.android.skeleton.model.TodoContact;

@Database(entities = {Todo.class, TodoContact.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ITodoCRUDOperation todoCRUDOperation();
}
