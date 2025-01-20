package com.cs.mad.project.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.cs.mad.project.model.Todo;
import com.cs.mad.project.model.TodoContact;

@Database(entities = {Todo.class, TodoContact.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ITodoCRUDOperation todoCRUDOperation();
}
