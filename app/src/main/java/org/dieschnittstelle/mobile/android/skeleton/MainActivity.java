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

import java.util.List;

public class MainActivity extends AppCompatActivity implements TodoAdapter.OnItemClickListener {

    private static final int REQUEST_CODE_ADD_TODO = 1;
    private static final int REQUEST_CODE_EDIT_TODO = 2;

    private RecyclerView recyclerView;
    private TodoAdapter todoAdapter;
    private TodoRepository todoRepository;

    // Variable zum Speichern des aktuellen Sortiermodus
    private int currentSortMode = SORT_BY_DATE_IMPORTANCE;
    private static final int SORT_BY_DATE_IMPORTANCE = 0;
    private static final int SORT_BY_IMPORTANCE_DATE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if (savedInstanceState != null) {
            currentSortMode = savedInstanceState.getInt("currentSortMode", SORT_BY_DATE_IMPORTANCE);
        }

        // Initialisiere das TodoRepository
        todoRepository = new TodoRepository(getApplicationContext());

        // Initialisiere RecyclerView und Adapter
        recyclerView = findViewById(R.id.recyclerViewTodos);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        todoAdapter = new TodoAdapter(this, this.todoRepository ,this);
        recyclerView.setAdapter(todoAdapter);

        // Lade Todos aus der lokalen Datenbank
        loadTodosFromLocalDatabase();

        // Setup des FloatingActionButtons zum Hinzufügen neuer Todos
        FloatingActionButton fabAddTodo = findViewById(R.id.fabAddTodo);
        fabAddTodo.setOnClickListener(v -> {
            // Starte die Aktivität zum Hinzufügen eines neuen Todos
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

    private void sortTodoList(List<Todo> todoList) {
        switch (currentSortMode) {
            case SORT_BY_DATE_IMPORTANCE:
                todoList.sort((t1, t2) -> {
                    // Zuerst nach Done-Status
                    if (t1.isDone() != t2.isDone()) {
                        return Boolean.compare(t1.isDone(), t2.isDone());
                    }
                    // Dann nach Datum
                    if (t1.getExpiry() > 0 && t2.getExpiry() > 0) {
                        int dateCompare = Long.compare(t1.getExpiry(), t2.getExpiry());
                        if (dateCompare != 0) {
                            return dateCompare;
                        }
                    } else if (t1.getExpiry() > 0) {
                        return -1; // t1 hat Datum, t2 nicht
                    } else if (t2.getExpiry() > 0) {
                        return 1; // t2 hat Datum, t1 nicht
                    }
                    // Dann nach Wichtigkeit
                    return Boolean.compare(!t1.isFavourite(), !t2.isFavourite()); // Wichtige zuerst
                });
                break;
            case SORT_BY_IMPORTANCE_DATE:
                todoList.sort((t1, t2) -> {
                    // Zuerst nach Done-Status
                    if (t1.isDone() != t2.isDone()) {
                        return Boolean.compare(t1.isDone(), t2.isDone());
                    }
                    // Dann nach Wichtigkeit
                    if (t1.isFavourite() != t2.isFavourite()) {
                        return Boolean.compare(!t1.isFavourite(), !t2.isFavourite()); // Wichtige zuerst
                    }
                    // Dann nach Datum
                    if (t1.getExpiry() > 0 && t2.getExpiry() > 0) {
                        return Long.compare(t1.getExpiry(), t2.getExpiry());
                    } else if (t1.getExpiry() > 0) {
                        return -1; // t1 hat Datum, t2 nicht
                    } else if (t2.getExpiry() > 0) {
                        return 1; // t2 hat Datum, t1 nicht
                    }
                    return 0;
                });
                break;
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
    public void onFavoriteToggle(Todo todo, boolean isFavorite) {
        // Aktualisiere den Favoritenstatus
        new Thread(() -> {
            todoRepository.updateTodo(todo);
            // Aktualisiere die Liste
            loadTodosFromLocalDatabase();
        }).start();
    }

    @Override
    public void onCompletedToggle(Todo todo, boolean isCompleted) {
        // Aktualisiere den Erledigtstatus
        new Thread(() -> {
            todoRepository.updateTodo(todo);
            // Aktualisiere die Liste
            loadTodosFromLocalDatabase();
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Aktualisiere die Liste nach Rückkehr aus der Detailansicht
        if (requestCode == REQUEST_CODE_ADD_TODO && resultCode == RESULT_OK) {
            loadTodosFromLocalDatabase();
        }
        if (requestCode == REQUEST_CODE_EDIT_TODO && resultCode == RESULT_OK) {
            loadTodosFromLocalDatabase();
        }
    }

    // Erstelle ein Optionsmenü zum Umschalten des Sortiermodus
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_sort, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Aktualisiere die Menüelemente entsprechend dem aktuellen Sortiermodus
        MenuItem sortByDateImportance = menu.findItem(R.id.action_sort_date_importance);
        MenuItem sortByImportanceDate = menu.findItem(R.id.action_sort_importance_date);

        if (currentSortMode == SORT_BY_DATE_IMPORTANCE) {
            sortByDateImportance.setChecked(true);
        } else if (currentSortMode == SORT_BY_IMPORTANCE_DATE) {
            sortByImportanceDate.setChecked(true);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    // Behandle die Auswahl von Menüelementen
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_sort_date_importance:
                currentSortMode = SORT_BY_DATE_IMPORTANCE;
                loadTodosFromLocalDatabase();
                return true;
            case R.id.action_sort_importance_date:
                currentSortMode = SORT_BY_IMPORTANCE_DATE;
                loadTodosFromLocalDatabase();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Speichere den aktuellen Sortiermodus bei Orientierungwechsel
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt("currentSortMode", currentSortMode);
        super.onSaveInstanceState(outState);
    }
}
