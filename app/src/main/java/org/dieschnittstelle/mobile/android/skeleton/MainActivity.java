package org.dieschnittstelle.mobile.android.skeleton;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.dieschnittstelle.mobile.android.skeleton.adapter.TodoAdapter;
import org.dieschnittstelle.mobile.android.skeleton.model.Todo;
import org.dieschnittstelle.mobile.android.skeleton.repository.TodoRepository;
import org.dieschnittstelle.mobile.android.skeleton.util.MADAsyncTask;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private TodoRepository todoRepository;
    private RecyclerView recyclerView;
    private TodoAdapter todoAdapter;
    private List<Todo> todoList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        todoRepository = new TodoRepository(getApplicationContext());

        // RecyclerView initialisieren
        recyclerView = findViewById(R.id.recyclerViewTodos);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Todos aus der lokalen Datenbank laden
        loadTodosFromLocalDatabase();

        // Verfügbarkeit der Webanwendung prüfen und synchronisieren
        if (todoRepository.isWebApplicationAvailable()) {
            todoRepository.synchronizeData(true);
            // Nach der Synchronisation lokale Daten aktualisieren
            loadTodosFromLocalDatabase();
        } else {
            Toast.makeText(this, "Webanwendung nicht verfügbar. Nur lokale Daten werden verwendet.", Toast.LENGTH_LONG).show();
        }
    }

    private void loadTodosFromLocalDatabase() {
        new MADAsyncTask<Void, Void, List<Todo>>() {
            @Override
            protected List<Todo> doInBackground(Void... voids) {
                // Room will execute this on a background thread due to @WorkerThread
                return todoRepository.getAllTodos();
            }

            @Override
            protected void onPostExecute(List<Todo> todos) {
                // Update UI on the main thread
                todoAdapter = new TodoAdapter(todos, todoRepository);
                recyclerView.setAdapter(todoAdapter);
            }
        }.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Menüinflater
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Aktionen behandeln
        int id = item.getItemId();

        if (id == R.id.action_delete_local) {
            todoRepository.deleteLocalTodos();
            loadTodosFromLocalDatabase();
            Toast.makeText(this, "Lokale Todos wurden gelöscht.", Toast.LENGTH_SHORT).show();
            return true;
        }
        if (id == R.id.action_delete_remote) {
            todoRepository.deleteRemoteTodos();
            Toast.makeText(this, "Löschanfrage für entfernte Todos gesendet.", Toast.LENGTH_SHORT).show();
            return true;
        }
        if (id == R.id.action_synchronize) {
            if (todoRepository.isWebApplicationAvailable()) {
                todoRepository.synchronizeData(true);
                loadTodosFromLocalDatabase();
                Toast.makeText(this, "Synchronisation abgeschlossen.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Webanwendung nicht verfügbar. Synchronisation nicht möglich.", Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
