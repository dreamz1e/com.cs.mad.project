package org.dieschnittstelle.mobile.android.skeleton.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import org.dieschnittstelle.mobile.android.skeleton.model.Converters;
import org.dieschnittstelle.mobile.android.skeleton.model.Todo;

@Database(entities = {Todo.class}, version = 1)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract ITodoCRUDOperation todoCRUDOperation();
}
