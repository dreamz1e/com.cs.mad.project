package com.cs.mad.project.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(foreignKeys = @ForeignKey(
    entity = Todo.class,
    parentColumns = "id",
    childColumns = "todoId",
    onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("todoId")}
)
public class TodoContact {
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private int todoId;
    private String contactId;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    
    // Getter
    public long getId() {
        return id;
    }
    
    public int getTodoId() {
        return todoId;
    }
    
    public String getContactId() {
        return contactId;
    }
    
    public String getContactName() {
        return contactName;
    }
    
    public String getContactEmail() {
        return contactEmail;
    }
    
    public String getContactPhone() {
        return contactPhone;
    }
    
    // Setter
    public void setId(long id) {
        this.id = id;
    }
    
    public void setTodoId(int todoId) {
        this.todoId = todoId;
    }
    
    public void setContactId(String contactId) {
        this.contactId = contactId;
    }
    
    public void setContactName(String contactName) {
        this.contactName = contactName;
    }
    
    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }
    
    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }
}