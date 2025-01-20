package com.cs.mad.project;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cs.mad.project.adapter.TodoAdapter;
import com.cs.mad.project.model.Todo;
import com.cs.mad.project.model.TodoContact;
import com.cs.mad.project.repository.TodoRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements TodoAdapter.OnItemClickListener {

    private static final int REQUEST_CODE_ADD_TODO = 1;
    private static final int REQUEST_CODE_EDIT_TODO = 2;

    private static final String KEY_SORT_MODE = "currentSortMode";
    private static final String KEY_SCROLL_POSITION = "scrollPosition";

    private RecyclerView recyclerView;
    private TodoAdapter todoAdapter;
    private TodoRepository todoRepository;


    // Variable zum Speichern des aktuellen Sortiermodus
    private int currentSortMode = SORT_BY_DATE_IMPORTANCE;
    private int lastScrollPosition = 0;
    private static final int SORT_BY_DATE_IMPORTANCE = 0;
    private static final int SORT_BY_IMPORTANCE_DATE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    
        // Setze Standardsortiermodus, falls nicht gespeichert
        if (savedInstanceState != null) {
            currentSortMode = savedInstanceState.getInt(KEY_SORT_MODE, SORT_BY_DATE_IMPORTANCE);
            lastScrollPosition = savedInstanceState.getInt(KEY_SCROLL_POSITION, 0);
        }
    
        // Initialisiere Repository und RecyclerView
        todoRepository = new TodoRepository(getApplicationContext());
        recyclerView = findViewById(R.id.recyclerViewTodos);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        todoAdapter = new TodoAdapter(this, todoRepository, this);
        recyclerView.setAdapter(todoAdapter);
    
        // Check web availability and sync
        checkWebAvailabilityAndSync();
    
        // FAB Setup
        FloatingActionButton fabAddTodo = findViewById(R.id.fabAddTodo);
        fabAddTodo.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TodoDetailActivity.class);
            startActivityForResult(intent, REQUEST_CODE_ADD_TODO);
        });
    }

    private void checkWebAvailabilityAndSync() {
        new Thread(() -> {
            boolean isWebAvailable = todoRepository.checkWebAvailability();
            
            runOnUiThread(() -> {
                if (!isWebAvailable) {
                    showWebUnavailableWarning();
                } else {
                    synchronizeWithBackend();
                }
            });
        }).start();
    }

    private void showWebUnavailableWarning() {
        new AlertDialog.Builder(this)
            .setTitle("Offline Mode")
            .setMessage("The server (10.0.2.2:8080) is not reachable. The app will operate in offline mode using local storage only.")
            .setPositiveButton("OK", (dialog, which) -> {
                // Load local todos only
                updateTodoList();
            })
            .setCancelable(false)
            .show();
    }

    private void synchronizeWithBackend() {
        new Thread(() -> {
            try {
                List<Todo> localTodos = todoRepository.getAllTodos();
                boolean syncSuccess = false;
                
                if (localTodos.isEmpty()) {
                    syncSuccess = syncFromServer();
                } else {
                    syncSuccess = syncToServer(localTodos);
                }
                
                if (syncSuccess) {
                    runOnUiThread(this::updateTodoList);
                }
            } catch (IOException e) {
                Log.e("MainActivity", "Synchronization error: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, 
                        "Error during synchronization: " + e.getMessage(), 
                        Toast.LENGTH_LONG).show();
                    updateTodoList();
                });
            }
        }).start();
    }

    private boolean syncFromServer() throws IOException {
        Response<List<Todo>> response = todoRepository.getApiService().readAllTodos().execute();
        if (response.isSuccessful() && response.body() != null) {
            todoRepository.insertTodosLocally(response.body());
            return true;
        }
        return false;
    }

    private boolean syncToServer(List<Todo> localTodos) throws IOException {
        Response<Boolean> deleteResponse = todoRepository.getApiService().deleteAllTodos().execute();
        if (!deleteResponse.isSuccessful()) return false;
        
        for (Todo localTodo : localTodos) {
            Todo serverTodo = todoRepository.createServerTodo(localTodo);
            Response<Todo> createResponse = todoRepository.getApiService()
                .createTodo(serverTodo).execute();
            if (!createResponse.isSuccessful()) {
                throw new IOException("Failed to create todo on server: " + 
                    createResponse.code());
            }
        }
        return true;
    }

    private void updateTodoList() {
        new Thread(() -> {
            List<Todo> todoList = todoRepository.getAllTodos();
            sortTodoList(todoList);
            runOnUiThread(() -> {
                todoAdapter.setTodos(todoList);
                // Stelle Scrollposition wieder her
                recyclerView.post(() -> {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager != null) {
                        layoutManager.scrollToPosition(lastScrollPosition);
                    }
                });
            });
        }).start();
    }

    private void sortTodoList(List<Todo> todoList) {
        Collections.sort(todoList, (t1, t2) -> {
            // First, sort by completion status
            if (t1.isDone() != t2.isDone()) {
                return Boolean.compare(t1.isDone(), t2.isDone());
            }
            
            // If both are completed or both are not completed, use the current sort mode
            int result;
            if (currentSortMode == SORT_BY_DATE_IMPORTANCE) {
                result = Long.compare(t1.getExpiry(), t2.getExpiry());
                if (result == 0) {
                    result = Boolean.compare(t2.isFavourite(), t1.isFavourite());
                }
            } else {
                result = Boolean.compare(t2.isFavourite(), t1.isFavourite());
                if (result == 0) {
                    result = Long.compare(t1.getExpiry(), t2.getExpiry());
                }
            }
            return result != 0 ? result : t1.getName().compareTo(t2.getName());
        });
    }


    @Override
    public void onItemClick(Todo todo) {
        // Öffne die Detailansicht des ausgewählten Todos
        Intent intent = new Intent(MainActivity.this, TodoDetailActivity.class);
        intent.putExtra("todoId", todo.getId());
        startActivityForResult(intent, REQUEST_CODE_EDIT_TODO);
    }

    @Override
    public void onCompletedToggle(Todo todo, boolean isCompleted) {
        todo.setDone(isCompleted);
        new Thread(() -> {
            todoRepository.updateTodo(todo, () -> {
                // Nach dem Update die gesamte Liste neu laden und sortieren
                updateTodoList();
            });
        }).start();
    }

    @Override
    public void onFavoriteToggle(Todo todo, boolean isFavorite) {
        todo.setFavourite(isFavorite);
        new Thread(() -> {
            todoRepository.updateTodo(todo, () -> {
                // Nach dem Update die gesamte Liste neu laden und sortieren
                updateTodoList();
            });
        }).start();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQUEST_CODE_ADD_TODO || requestCode == REQUEST_CODE_EDIT_TODO) 
                && resultCode == RESULT_OK) {
            updateTodoList();
        }
    }

    // Erstelle ein Optionsmenü zum Umschalten des Sortiermodus
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        getMenuInflater().inflate(R.menu.menu_main_sort, menu);
        // Setze initial den ersten Sortiermodus als ausgewählt
        if (currentSortMode == SORT_BY_DATE_IMPORTANCE) {
            menu.findItem(R.id.action_sort_date_importance).setChecked(true);
        } else {
            menu.findItem(R.id.action_sort_importance_date).setChecked(true);
        }
        return true;
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Stelle sicher, dass nur eine Option ausgewählt ist
        MenuItem sortByDateImportance = menu.findItem(R.id.action_sort_date_importance);
        MenuItem sortByImportanceDate = menu.findItem(R.id.action_sort_importance_date);

        sortByDateImportance.setChecked(currentSortMode == SORT_BY_DATE_IMPORTANCE);
        sortByImportanceDate.setChecked(currentSortMode == SORT_BY_IMPORTANCE_DATE);

        return super.onPrepareOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sort_date_importance:
                if (!item.isChecked()) {
                    item.setChecked(true);
                    currentSortMode = SORT_BY_DATE_IMPORTANCE;
                    updateTodoList();
                }
                return true;
            
            case R.id.action_sort_importance_date:
                if (!item.isChecked()) {
                    item.setChecked(true);
                    currentSortMode = SORT_BY_IMPORTANCE_DATE;
                    updateTodoList();
                }
                return true;
            
            case R.id.action_sync:
                new Thread(() -> {
                    boolean isAvailable = todoRepository.checkWebAvailability();
                    runOnUiThread(() -> {
                        if (isAvailable) {
                            synchronizeWithBackend();
                        } else {
                            showWebUnavailableWarning();
                        }
                    });
                }).start();
                return true;
            
            case R.id.action_delete_local:
                showDeleteConfirmationDialog("Delete Local Todos?", 
                    "This will delete all todos from your device.", 
                    () -> {
                        new Thread(() -> {
                            todoRepository.deleteLocalTodos();
                            runOnUiThread(() -> {
                                updateTodoList();
                                Toast.makeText(this, "Local todos deleted", Toast.LENGTH_SHORT).show();
                            });
                        }).start();
                    });
                return true;
            
            case R.id.action_delete_server:
                new Thread(() -> {
                    boolean isAvailable = todoRepository.checkWebAvailability();
                    runOnUiThread(() -> {
                        if (isAvailable) {
                            showDeleteConfirmationDialog("Delete Server Todos?", 
                                "This will delete all todos from the server.", 
                                () -> {
                                    new Thread(() -> {
                                        todoRepository.deleteRemoteTodos();
                                        runOnUiThread(() -> Toast.makeText(this, 
                                            "Server todos deleted", Toast.LENGTH_SHORT).show());
                                    }).start();
                                });
                        } else {
                            showWebUnavailableWarning();
                        }
                    });
                }).start();
                return true;
            
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showDeleteConfirmationDialog(String title, String message, Runnable onConfirm) {
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Delete", (dialog, which) -> onConfirm.run())
            .setNegativeButton("Cancel", null)
            .show();
    }

    // Speichere den aktuellen Sortiermodus
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Speichere Sortiermodus
        outState.putInt(KEY_SORT_MODE, currentSortMode);
        
        // Speichere Scrollposition
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager != null) {
            lastScrollPosition = layoutManager.findFirstVisibleItemPosition();
            outState.putInt(KEY_SCROLL_POSITION, lastScrollPosition);
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        currentSortMode = savedInstanceState.getInt(KEY_SORT_MODE, SORT_BY_DATE_IMPORTANCE);
        lastScrollPosition = savedInstanceState.getInt(KEY_SCROLL_POSITION, 0);
        
        // Stelle Scrollposition wieder her
        recyclerView.post(() -> {
            LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            if (layoutManager != null) {
                layoutManager.scrollToPosition(lastScrollPosition);
            }
        });
    }
}
