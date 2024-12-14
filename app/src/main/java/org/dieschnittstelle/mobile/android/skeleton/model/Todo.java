package org.dieschnittstelle.mobile.android.skeleton.model;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Todo {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;
    private String description;
    private boolean done;
    private boolean favourite;

    @Ignore
    private List<TodoContact> todoContacts = new ArrayList<>();

    // Diese Liste wird für die Server-Kommunikation verwendet
    @Ignore
    private List<String> contacts = new ArrayList<>();

    private long expiry;

    // Standardkonstruktor
    public Todo() {
    }

    // Getters und Setters für alle Felder

    // ID
    public int getId() {
        return id;
    }

    // Setze ID (optional, je nach Bedarf)
    public void setId(int id) {
        this.id = id;
    }

    // Name
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // Beschreibung
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Erledigt
    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    // Favorit
    public boolean isFavourite() {
        return favourite;
    }

    public void setFavourite(boolean favourite) {
        this.favourite = favourite;
    }

    public long getExpiry() {
        return expiry;
    }

    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }

    // Getter und Setter für todoContacts (für lokale Verwendung)
    @Ignore
    public List<TodoContact> getContacts() {
        return todoContacts;
    }

    @Ignore
    public void setContacts(List<TodoContact> contacts) {
        this.todoContacts = contacts;
    }

    // Getter und Setter für contacts (für Server-Kommunikation)
    public List<String> getContactIds() {
        return contacts;
    }

    public void setContactIds(List<String> contactIds) {
        this.contacts = contactIds;
    }

}
