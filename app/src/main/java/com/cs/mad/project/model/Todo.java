package com.cs.mad.project.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Todo implements Serializable {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String name;
    private String description;
    private long expiry;
    private boolean done;
    private boolean favourite;
    private List<String> contacts;
    private Location location;

    @Ignore
    private List<TodoContact> todoContacts;

    // Standard Constructor Room
    public Todo() {
        contacts = new ArrayList<>();
        todoContacts = new ArrayList<>();
    }

    // Constructor mit Parametern
    @Ignore
    public Todo(String name, String description) {
        this();
        this.name = name;
        this.description = description;
    }

    // Standard getters and setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public long getExpiry() { return expiry; }
    public void setExpiry(long expiry) { this.expiry = expiry; }
    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }
    public boolean isFavourite() { return favourite; }
    public void setFavourite(boolean favourite) { this.favourite = favourite; }
    public List<String> getContacts() { return contacts; }
    public void setContacts(List<String> contacts) { this.contacts = contacts; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }

    // Local-only methoden für TodoContact handling
    @Ignore
    public List<TodoContact> getTodoContacts() { return todoContacts; }
    @Ignore
    public void setTodoContacts(List<TodoContact> todoContacts) { this.todoContacts = todoContacts; }

    // Location classes
    // Wird voraussichtlich nicht benötigt
    public static class Location implements Serializable {
        private String name;
        private LatLng latlng;

        public Location() {}
        
        public Location(String name, LatLng latlng) {
            this.name = name;
            this.latlng = latlng;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public LatLng getLatlng() { return latlng; }
        public void setLatlng(LatLng latlng) { this.latlng = latlng; }
    }

    public static class LatLng implements Serializable {
        private double lat;
        private double lng;

        public LatLng() {}

        public LatLng(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }

        public double getLat() { return lat; }
        public void setLat(double lat) { this.lat = lat; }
        public double getLng() { return lng; }
        public void setLng(double lng) { this.lng = lng; }
    }
}
