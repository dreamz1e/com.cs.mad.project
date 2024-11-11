package org.dieschnittstelle.mobile.android.skeleton.database;

import androidx.annotation.WorkerThread;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import org.dieschnittstelle.mobile.android.skeleton.model.Todo;

import java.util.List;

@Dao
public interface ITodoCRUDOperation {
    @WorkerThread
    @Query("SELECT * FROM Todo")
    List<Todo> getAllTodos();

    @WorkerThread
    @Query("SELECT * FROM Todo WHERE id = :id")
    Todo getTodoById(int id);

    @WorkerThread
    @Insert
    void insertTodo(Todo todo);

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
}
