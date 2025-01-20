package com.cs.mad.project.database;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.cs.mad.project.model.Todo.Location;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Converters {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static String fromContactsList(List<String> contacts) {
        if (contacts == null) {
            return null;
        }
        return gson.toJson(contacts);
    }

    @TypeConverter
    public static List<String> toContactsList(String contactsString) {
        if (contactsString == null) {
            return new ArrayList<>();
        }
        Type listType = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(contactsString, listType);
    }

    @TypeConverter
    public static String fromLocation(Location location) {
        if (location == null) {
            return null;
        }
        return gson.toJson(location);
    }

    @TypeConverter
    public static Location toLocation(String locationString) {
        if (locationString == null) {
            return null;
        }
        return gson.fromJson(locationString, Location.class);
    }
} 