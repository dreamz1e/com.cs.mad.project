package com.cs.mad.project;

import android.content.Context;
import androidx.room.Room;
import com.cs.mad.project.database.AppDatabase;

public class DatabaseClient {
    private Context context;
    private static DatabaseClient instance;

    private AppDatabase appDatabase;

    private DatabaseClient(Context context) {
        this.context = context;

        // Erstelle die Room-Datenbank
        appDatabase = Room.databaseBuilder(context, AppDatabase.class, "TodoDB")
                        .fallbackToDestructiveMigration().build();
    }

    public static synchronized DatabaseClient getInstance(Context context) {
        if (instance == null)
            instance = new DatabaseClient(context);
        return instance;
    }

    public AppDatabase getAppDatabase() {
        return appDatabase;
    }
}
