package com.cs.mad.project.repository;

import android.content.Context;
import android.os.Looper;
import android.util.Log;
import android.os.Handler;

import com.cs.mad.project.DatabaseClient;
import com.cs.mad.project.model.Todo;
import com.cs.mad.project.model.TodoContact;
import com.cs.mad.project.database.ITodoCRUDOperation;
import com.cs.mad.project.remote.RetrofitClient;
import com.cs.mad.project.remote.ITodoAPIService;
import com.cs.mad.project.util.MADAsyncTask;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TodoRepository {
    private static final String TAG = "TodoRepository";
    private final ITodoCRUDOperation todoCRUDOperation;
    private final ITodoAPIService apiService;
    private boolean isWebAvailable = true;  // Add this class field

    public TodoRepository(Context context) {
        todoCRUDOperation = DatabaseClient.getInstance(context).getAppDatabase().todoCRUDOperation();
        apiService = RetrofitClient.getInstance().getApiService();
    }

    // Synchronisationsmethode beim Start der App
    public void synchronizeData(boolean isWebAvailable) {
        new MADAsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                if (isWebAvailable) {
                    List<Todo> localTodos = todoCRUDOperation.getAllTodos();

                    if (localTodos.isEmpty()) {
                        // Keine lokalen Todos, hole von der Webanwendung
                        fetchTodosFromWeb(); // Make sure fetchTodosFromWeb is also asynchronous
                    } else {
                        // Lokale Todos vorhanden, sende an die Webanwendung
                        sendTodosToWeb(localTodos); // Make sure sendTodosToWeb is also asynchronous
                    }
                } else {
                    // Webanwendung nicht verfügbar, Warnung wird in der UI gehandhabt
                    Log.w(TAG, "Webanwendung beim Start nicht verfügbar.");
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                // Optional: Update UI or perform other actions after synchronization
            }
        }.execute();
    }

    public Todo createServerTodo(Todo localTodo) {
        Todo serverTodo = new Todo();
        
        // Only copy the fields that the server expects
        serverTodo.setId(localTodo.getId());
        serverTodo.setName(localTodo.getName());
        serverTodo.setDescription(localTodo.getDescription());
        serverTodo.setDone(localTodo.isDone());
        serverTodo.setFavourite(localTodo.isFavourite());
        serverTodo.setExpiry(localTodo.getExpiry());
        serverTodo.setLocation(localTodo.getLocation());
        
        // Handle contacts properly
        List<String> contactIds = new ArrayList<>();
        if (localTodo.getTodoContacts() != null) {
            for (TodoContact contact : localTodo.getTodoContacts()) {
                if (contact != null && contact.getContactId() != null) {
                    contactIds.add(contact.getContactId());
                }
            }
        }
        serverTodo.setContacts(contactIds);
        
        // Ensure todoContacts is null for server communication
        serverTodo.setTodoContacts(null);
        
        return serverTodo;
    }

    public void updateTodo(Todo todo, Runnable onSuccess) {
        new Thread(() -> {
            try {
                // First update locally
                todoCRUDOperation.updateTodo(todo);
                
                // Run success callback immediately after local update
                runSuccessCallback(onSuccess);
                
                // Try to sync with server if web is available
                if (isWebAvailable) {
                    try {
                        Todo serverTodo = createServerTodo(todo);
                        Response<Todo> response = apiService.createTodo(serverTodo).execute();
                        
                        if (!response.isSuccessful()) {
                            Log.e(TAG, "Failed to update todo on server: " + response.code());
                            isWebAvailable = false;
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Server not available, operating in offline mode");
                        isWebAvailable = false;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating todo locally", e);
            }
        }).start();
    }

    private void runSuccessCallback(Runnable onSuccess) {
        if (onSuccess != null) {
            new Handler(Looper.getMainLooper()).post(onSuccess);
        }
    }

    public boolean checkWebAvailability() {
        try {
            Response<List<Todo>> response = apiService.readAllTodos().execute();
            isWebAvailable = response.isSuccessful();  // Update cached status
            return isWebAvailable;
        } catch (IOException e) {
            isWebAvailable = false;  // Update cached status
            return false;
        }
    }

    // Methode zum Abrufen eines Todos nach ID
    public Todo getTodoById(long id) {
        Todo todo = todoCRUDOperation.getTodoById(id);
        if (todo != null) {
            // Load contacts for this todo
            List<TodoContact> contacts = todoCRUDOperation.getContactsForTodo(id);
            todo.setTodoContacts(contacts);
            
            // Update the contacts list with contact IDs
            List<String> contactIds = new ArrayList<>();
            if (contacts != null) {
                for (TodoContact contact : contacts) {
                    if (contact.getContactId() != null) {
                        contactIds.add(contact.getContactId());
                    }
                }
            }
            todo.setContacts(contactIds);
        }
        return todo;
    }

    // Methode zum Einfügen eines neuen Todos
    public void insertTodo(final Todo todo) {
        new MADAsyncTask<Todo, Void, Todo>() {
            @Override
            protected Todo doInBackground(Todo... todos) {
                try {
                    // Zuerst an Backend senden
                    Response<Todo> response = apiService.createTodo(todos[0]).execute();
                    if (response.isSuccessful() && response.body() != null) {
                        Todo createdTodo = response.body();
                        // Dann in lokale DB speichern
                        todoCRUDOperation.insertTodo(createdTodo);
                        return createdTodo;
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Fehler beim Erstellen des Todos", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Todo result) {
                if (result != null) {
                    // Optional: UI aktualisieren
                }
            }
        }.execute(todo);
    }

    private void fetchTodosFromWeb() {
        new MADAsyncTask<Void, Void, List<Todo>>() {
            @Override
            protected List<Todo> doInBackground(Void... voids) {
                try {
                    Response<List<Todo>> response = apiService.readAllTodos().execute();
                    if (response.isSuccessful()) {
                        return response.body();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Fehler beim Abrufen der Todos von der Webanwendung", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(List<Todo> todosFromWeb) {
                if (todosFromWeb != null) {
                    new MADAsyncTask<List<Todo>, Void, Void>() {
                        @Override
                        protected Void doInBackground(List<Todo>... lists) {
                            todoCRUDOperation.insertTodos(lists[0]);
                            return null;
                        }
                        // Optional: onPostExecute to update UI
                    }.execute(todosFromWeb);
                }
            }
        }.execute();
    }

    private void sendTodosToWeb(List<Todo> localTodos) {
        // Zuerst alle entfernten Todos löschen
        Call<Boolean> deleteCall = apiService.deleteAllTodos();
        deleteCall.enqueue(new Callback<Boolean>() {
            @Override
            public void onResponse(Call<Boolean> call, Response<Boolean> response) {
                if (response.isSuccessful() && Boolean.TRUE.equals(response.body())) {
                    // Lokale Todos an die Webanwendung senden
                    for (Todo todo : localTodos) {
                        Call<Todo> createCall = apiService.createTodo(todo);
                        createCall.enqueue(new Callback<Todo>() {
                            @Override
                            public void onResponse(Call<Todo> call, Response<Todo> response) {
                                // Erfolgreiche Erstellung
                            }

                            @Override
                            public void onFailure(Call<Todo> call, Throwable t) {
                                Log.e(TAG, "Fehler beim Erstellen des Todos auf der Webanwendung", t);
                            }
                        });
                    }
                } else {
                    Log.e(TAG, "Fehler beim Löschen der entfernten Todos");
                }
            }

            @Override
            public void onFailure(Call<Boolean> call, Throwable t) {
                Log.e(TAG, "Fehler beim Löschen der entfernten Todos", t);
            }
        });
    }

    public void deleteTodo(Todo todo) {
        new Thread(() -> {
            try {
                // First delete locally
                todoCRUDOperation.deleteTodo(todo);
                
                // Try to sync with server only if web is available
                if (isWebAvailable) {
                    try {
                        Response<Boolean> response = apiService.deleteTodo((int)todo.getId()).execute();
                        if (!response.isSuccessful()) {
                            Log.e(TAG, "Failed to delete todo on server: " + response.code());
                            isWebAvailable = false;
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Server not available, operating in offline mode");
                        isWebAvailable = false;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error deleting todo locally", e);
            }
        }).start();
    }

    // Methode zum Löschen lokaler Todos
    public void deleteLocalTodos() {
        new MADAsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                todoCRUDOperation.deleteAllTodos();
                return null;
            }
            // Optional: onPostExecute to update UI
        }.execute();
    }

    // Methode zum Löschen entfernter Todos
    public void deleteRemoteTodos() {
        Call<Boolean> deleteCall = apiService.deleteAllTodos();
        deleteCall.enqueue(new Callback<Boolean>() {
            @Override
            public void onResponse(Call<Boolean> call, Response<Boolean> response) {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Fehler beim Löschen der entfernten Todos");
                }
            }

            @Override
            public void onFailure(Call<Boolean> call, Throwable t) {
                Log.e(TAG, "Fehler beim Löschen der entfernten Todos", t);
            }
        });
    }

    // Methode zum Abrufen aller Todos
    public List<Todo> getAllTodos() {
        List<Todo> todos = todoCRUDOperation.getAllTodos();
        // Load contacts for each todo
        for (Todo todo : todos) {
            List<TodoContact> contacts = todoCRUDOperation.getContactsForTodo(todo.getId());
            todo.setTodoContacts(contacts);
            
            // Update the contacts list with contact IDs
            List<String> contactIds = new ArrayList<>();
            if (contacts != null) {
                for (TodoContact contact : contacts) {
                    if (contact.getContactId() != null) {
                        contactIds.add(contact.getContactId());
                    }
                }
            }
            todo.setContacts(contactIds);
        }
        return todos;
    }

    // Methode zum Überprüfen der Verfügbarkeit der Webanwendung
    public boolean isWebApplicationAvailable() {
        // Implementiere eine Methode, um die Erreichbarkeit der Webanwendung zu prüfen
        // Zum Beispiel einen Ping oder eine Testanfrage senden
        return true; // Platzhalter
    }

    public ITodoAPIService getApiService() {
        return apiService;
    }

    public void deleteAllTodosLocally() {
        todoCRUDOperation.deleteAllTodos();
    }

    public void insertTodosLocally(List<Todo> todos) {
        todoCRUDOperation.insertTodos(todos);
    }

    public void insertTodoContact(TodoContact todoContact) {
        todoCRUDOperation.insertTodoContact(todoContact);
    }

    public void deleteTodoContact(TodoContact todoContact) {
        todoCRUDOperation.deleteTodoContact(todoContact);
    }

    public List<TodoContact> getContactsForTodo(int todoId) {
        return todoCRUDOperation.getContactsForTodo(todoId);
    }

    public void synchronizeWithBackend() {
        new Thread(() -> {
            List<Todo> localTodos = getAllTodos();
            try {
                if (localTodos.isEmpty()) {
                    // Fetch from server if no local todos exist
                    Response<List<Todo>> response = apiService.readAllTodos().execute();
                    if (response.isSuccessful() && response.body() != null) {
                        insertTodosLocally(response.body());
                    }
                } else {
                    // Send local todos to server
                    Response<Boolean> deleteResponse = apiService.deleteAllTodos().execute();
                    if (deleteResponse.isSuccessful()) {
                        for (Todo localTodo : localTodos) {
                            Todo serverTodo = createServerTodo(localTodo);
                            Response<Todo> createResponse = apiService.createTodo(serverTodo).execute();
                            if (!createResponse.isSuccessful()) {
                                throw new IOException("Failed to create todo on server: " + createResponse.code());
                            }
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Synchronization error: " + e.getMessage(), e);
            }
        }).start();
    }

    public void createTodo(Todo todo, Runnable onSuccess) {
        new Thread(() -> {
            try {
                // First create locally
                long localId = todoCRUDOperation.insertTodo(todo);
                todo.setId(localId);  // Set the generated ID
                
                // Run success callback immediately after local creation
                runSuccessCallback(onSuccess);
                
                // Try to sync with server if web is available
                if (isWebAvailable) {
                    try {
                        Todo serverTodo = createServerTodo(todo);
                        Response<Todo> response = apiService.createTodo(serverTodo).execute();
                        
                        if (!response.isSuccessful()) {
                            Log.e(TAG, "Failed to create todo on server: " + response.code());
                            isWebAvailable = false;
                        } else if (response.body() != null) {
                            // Update local todo with server data but keep local ID
                            Todo serverResponse = response.body();
                            serverResponse.setId(localId);
                            todoCRUDOperation.updateTodo(serverResponse);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Server not available, operating in offline mode");
                        isWebAvailable = false;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating todo locally", e);
            }
        }).start();
    }
}
