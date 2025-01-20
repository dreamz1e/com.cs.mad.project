package com.cs.mad.project.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.cs.mad.project.model.Todo;
import com.cs.mad.project.model.TodoContact;

@Database(entities = {Todo.class, TodoContact.class}, version = 4)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract ITodoCRUDOperation todoCRUDOperation();
}
