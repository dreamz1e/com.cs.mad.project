package org.dieschnittstelle.mobile.android.skeleton;

import android.content.Intent;
import android.os.Bundle;

import android.view.Menu;
import android.view.MenuItem;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.dieschnittstelle.mobile.android.skeleton.adapter.TodoAdapter;
import org.dieschnittstelle.mobile.android.skeleton.model.Todo;
import org.dieschnittstelle.mobile.android.skeleton.repository.TodoRepository;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Collections;
import java.util.List;

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
    
        // Lade Todos nur wenn sie noch nicht geladen wurden
        if (todoAdapter.getItemCount() == 0) {
            updateTodoList();
        }
    
        // FAB Setup
        FloatingActionButton fabAddTodo = findViewById(R.id.fabAddTodo);
        fabAddTodo.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TodoDetailActivity.class);
            startActivityForResult(intent, REQUEST_CODE_ADD_TODO);
        });
    }

    private void loadTodosFromLocalDatabase() {
        new Thread(() -> {
            List<Todo> todoList = todoRepository.getAllTodos();
            // Sortiere die Liste entsprechend dem aktuellen Sortiermodus
            sortTodoList(todoList);

            runOnUiThread(() -> todoAdapter.setTodos(todoList));
        }).start();
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
        if (currentSortMode == SORT_BY_DATE_IMPORTANCE) {
            Collections.sort(todoList, (t1, t2) -> {
                // Zuerst nach Fälligkeit sortieren
                if (t1.getExpiry() != t2.getExpiry()) {
                    return Long.compare(t1.getExpiry(), t2.getExpiry());
                }
                // Bei gleicher Fälligkeit nach Wichtigkeit
                if (t1.isFavourite() != t2.isFavourite()) {
                    return t1.isFavourite() ? -1 : 1;
                }
                // Bei gleicher Wichtigkeit nach Namen
                return t1.getName().compareTo(t2.getName());
            });
        } else {
            Collections.sort(todoList, (t1, t2) -> {
                // Zuerst nach Wichtigkeit sortieren
                if (t1.isFavourite() != t2.isFavourite()) {
                    return t1.isFavourite() ? -1 : 1;
                }
                // Bei gleicher Wichtigkeit nach Fälligkeit
                if (t1.getExpiry() != t2.getExpiry()) {
                    return Long.compare(t1.getExpiry(), t2.getExpiry());
                }
                // Bei gleicher Fälligkeit nach Namen
                return t1.getName().compareTo(t2.getName());
            });
        }
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
        int id = item.getItemId();

        switch (id) {
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
            default:
                return super.onOptionsItemSelected(item);
        }
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
