package com.cs.mad.project.database;

import androidx.annotation.WorkerThread;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.cs.mad.project.model.Todo;
import com.cs.mad.project.model.TodoContact;

import java.util.List;

@Dao
public interface ITodoCRUDOperation {
    @WorkerThread
    @Query("SELECT * FROM Todo")
    List<Todo> getAllTodos();

    @WorkerThread
    @Query("SELECT * FROM Todo WHERE id = :id")
    Todo getTodoById(long id);

    @WorkerThread
    @Insert
    long insertTodo(Todo todo);

    @WorkerThread
    @Insert
    void insertTodos(List<Todo> todos);

    @WorkerThread
    @Update
    void updateTodo(Todo todo);

    @WorkerThread
    @Delete
    void deleteTodo(Todo todo);

    @WorkerThread
    @Query("DELETE FROM Todo")
    void deleteAllTodos();

    @WorkerThread
    @Query("SELECT * FROM TodoContact WHERE todoId = :todoId")
    List<TodoContact> getContactsForTodo(long todoId);
    
    @WorkerThread
    @Insert
    void insertTodoContact(TodoContact todoContact);
    
    @WorkerThread
    @Delete
    void deleteTodoContact(TodoContact todoContact);
}
