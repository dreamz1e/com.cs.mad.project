package org.dieschnittstelle.mobile.android.skeleton.repository;

import android.content.Context;
import android.util.Log;

import org.dieschnittstelle.mobile.android.skeleton.DatabaseClient;
import org.dieschnittstelle.mobile.android.skeleton.model.Todo;
import org.dieschnittstelle.mobile.android.skeleton.database.ITodoCRUDOperation;
import org.dieschnittstelle.mobile.android.skeleton.remote.RetrofitClient;
import org.dieschnittstelle.mobile.android.skeleton.remote.ITodoAPIService;
import org.dieschnittstelle.mobile.android.skeleton.util.MADAsyncTask;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TodoRepository {
    private static final String TAG = "TodoRepository";
    private ITodoAPIService apiService;
    private ITodoCRUDOperation todoCRUDOperation;

    public TodoRepository(Context context) {
        apiService = RetrofitClient.getInstance().getApiService();
        todoCRUDOperation = DatabaseClient.getInstance(context).getAppDatabase().todoCRUDOperation();
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

    public void updateTodo(final Todo todo) {
        new MADAsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                // Lokales Update in der SQLite-Datenbank
                todoCRUDOperation.updateTodo(todo);

                // Prüfung, ob die Webanwendung verfügbar ist
                if (isWebApplicationAvailable()) {
                    // Remote-Update auf der Webanwendung
                    try {
                        Response<Todo> response = apiService.updateTodo(todo.getId(), todo).execute();
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Todo erfolgreich auf der Webanwendung aktualisiert.");
                        } else {
                            Log.e(TAG, "Fehler beim Aktualisieren des Todos auf der Webanwendung: " + response.message());
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Fehler beim Aktualisieren des Todos auf der Webanwendung", e);
                    }
                } else {
                    Log.w(TAG, "Webanwendung nicht verfügbar. Änderungen werden später synchronisiert.");
                    // Hier könntest du das Todo als "zu synchronisieren" markieren
                }
                return null;
            }
        }.execute();
    }
    // Methode zum Abrufen eines Todos nach ID
    public Todo getTodoById(int id) {
        return todoCRUDOperation.getTodoById(id);
    }

    // Methode zum Einfügen eines neuen Todos
    public void insertTodo(final Todo todo) {
        // In lokale Datenbank einfügen
        todoCRUDOperation.insertTodo(todo);

        // Nach erfolgreichem Einfügen in die lokale Datenbank, Todo an die Webanwendung senden
        apiService.createTodo(todo).enqueue(new Callback<Todo>() {
            @Override
            public void onResponse(Call<Todo> call, Response<Todo> response) {
                if (response.isSuccessful()) {
                    // Erfolgreich in der Webanwendung erstellt
                    Log.d(TAG, "Todo wurde in der Webanwendung erstellt");
                } else {
                    // Fehler bei der Erstellung
                    Log.e(TAG, "Fehler beim Erstellen des Todos in der Webanwendung: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Todo> call, Throwable t) {
                Log.e(TAG, "Netzwerkfehler beim Erstellen des Todos in der Webanwendung", t);
            }
        });
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
        return todoCRUDOperation.getAllTodos();
    }

    // Methode zum Überprüfen der Verfügbarkeit der Webanwendung
    public boolean isWebApplicationAvailable() {
        // Implementiere eine Methode, um die Erreichbarkeit der Webanwendung zu prüfen
        // Zum Beispiel einen Ping oder eine Testanfrage senden
        return true; // Platzhalter
    }
}
