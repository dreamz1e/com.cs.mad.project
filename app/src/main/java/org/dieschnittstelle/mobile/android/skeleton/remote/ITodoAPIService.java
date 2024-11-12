package org.dieschnittstelle.mobile.android.skeleton.remote;

import org.dieschnittstelle.mobile.android.skeleton.model.Todo;
import org.dieschnittstelle.mobile.android.skeleton.model.User;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.*;

public interface ITodoAPIService {

    @POST("/api/todos")
    Call<Todo> createTodo(@Body Todo todo);

    @GET("/api/todos")
    Call<List<Todo>> readAllTodos();

    @GET("/api/todos/{todoId}")
    Call<Todo> readTodo(@Path("todoId") int todoId);

    @PUT("/api/todos/{todoId}")
    Call<Todo> updateTodo(@Path("todoId") int todoId, @Body Todo todo);

    @DELETE("/api/todos/{todoId}")
    Call<Boolean> deleteTodo(@Path("todoId") int todoId);

    @DELETE("/api/todos")
    Call<Boolean> deleteAllTodos();

    @PUT("/api/todos/reset")
    Call<Boolean> reset();

    @PUT("/api/users/auth")
    Call<Boolean> authenticateUser(@Body User user);

    @PUT("/api/users/prepare")
    Call<Boolean> prepare(@Body User user);
}
