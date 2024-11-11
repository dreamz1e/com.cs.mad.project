package org.dieschnittstelle.mobile.android.skeleton;

import android.content.Context;

import androidx.room.Room;

import org.dieschnittstelle.mobile.android.skeleton.database.AppDatabase;

public class DatabaseClient {
    private Context context;
    private static DatabaseClient instance;

    private AppDatabase appDatabase;

    private DatabaseClient(Context context) {
        this.context = context;

        // Erstelle die Room-Datenbank
        appDatabase = Room.databaseBuilder(context, AppDatabase.class, "TodoDB").build();
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
