package org.dieschnittstelle.mobile.android.skeleton;

import android.app.DatePickerDialog;
import android.os.Bundle;

import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.dieschnittstelle.mobile.android.skeleton.model.Todo;
import org.dieschnittstelle.mobile.android.skeleton.repository.TodoRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class TodoDetailActivity extends AppCompatActivity {

    private EditText editTextName, editTextDescription;
    private Button buttonExpiryDate, buttonSave;
    private CheckBox checkBoxFavourite, checkBoxDone;

    private long selectedExpiryTimestamp = 0L; // UTC-Zeitstempel

    private TodoRepository todoRepository;

    private Todo todo; // Wird null sein, wenn ein neues Todo erstellt wird

    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo_detail);

        editTextName = findViewById(R.id.editTextName);
        editTextDescription = findViewById(R.id.editTextDescription);
        buttonExpiryDate = findViewById(R.id.buttonExpiryDate); // Beibehaltung des IDs
        checkBoxFavourite = findViewById(R.id.checkBoxFavourite);
        checkBoxDone = findViewById(R.id.checkBoxCompleted); // Umbenannt von 'Completed' zu 'Done'
        buttonSave = findViewById(R.id.buttonSave);

        todoRepository = new TodoRepository(getApplicationContext());

        int todoId = getIntent().getIntExtra("todoId", -1);

        if (todoId != -1) {
            // Bearbeiten eines bestehenden Todos
            new Thread(() -> {
                todo = todoRepository.getTodoById(todoId);
                runOnUiThread(() -> populateFields(todo));
            }).start();
        } else {
            // Erstellen eines neuen Todos
            todo = new Todo();
        }

        buttonExpiryDate.setOnClickListener(v -> showDatePicker());

        buttonSave.setOnClickListener(v -> saveTodo());

    }

    private void populateFields(Todo todo) {
        editTextName.setText(todo.getName());
        editTextDescription.setText(todo.getDescription());
        checkBoxFavourite.setChecked(todo.isFavourite());
        checkBoxDone.setChecked(todo.isDone());

        if (todo.getExpiry() > 0) {
            selectedExpiryTimestamp = todo.getExpiry();
            String dateString = dateFormat.format(selectedExpiryTimestamp);
            buttonExpiryDate.setText(dateString);
        }
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();

        if (selectedExpiryTimestamp > 0) {
            c.setTimeInMillis(selectedExpiryTimestamp);
        }

        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(TodoDetailActivity.this,
                (view, year1, month1, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year1, month1, dayOfMonth, 0, 0, 0);
                    selectedDate.set(Calendar.MILLISECOND, 0);
                    selectedExpiryTimestamp = selectedDate.getTimeInMillis();
                    String dateString = dateFormat.format(selectedExpiryTimestamp);
                    buttonExpiryDate.setText(dateString);
                }, year, month, day);

        datePickerDialog.show();
    }

    private void saveTodo() {
        // Werte aus den Feldern abrufen
        String name = editTextName.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();
        boolean isFavourite = checkBoxFavourite.isChecked();
        boolean isDone = checkBoxDone.isChecked();

        if (name.isEmpty()) {
            Toast.makeText(this, "Bitte einen Namen eingeben.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Aktualisiere das Todo-Objekt
        todo.setName(name);
        todo.setDescription(description);
        todo.setFavourite(isFavourite);
        todo.setDone(isDone);
        todo.setExpiry(selectedExpiryTimestamp);

        // In der Datenbank speichern
        new Thread(() -> {
            if (todo.getId() == 0) {
                // Neues Todo
                todoRepository.insertTodo(todo);
            } else {
                // Bestehendes Todo
                todoRepository.updateTodo(todo);
            }

            // Rückkehr zur MainActivity
            runOnUiThread(() -> {
                setResult(RESULT_OK);
                finish();
            });
        }).start();
    }
}
