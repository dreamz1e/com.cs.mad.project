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
import java.util.Date;
import java.util.Locale;

public class TodoDetailActivity extends AppCompatActivity {

    private EditText editTextName, editTextDescription;
    private Button buttonExpiryDate, buttonSave;
    private CheckBox checkBoxCompleted;
    private CheckBox checkBoxFavourite;

    private long selectedExpiryTimestamp = 0L;
    private boolean isEditMode = false;

    private TodoRepository todoRepository;
    private Todo todo;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo_detail);

        // Initialisiere Views
        editTextName = findViewById(R.id.editTextName);
        editTextDescription = findViewById(R.id.editTextDescription);
        buttonExpiryDate = findViewById(R.id.buttonExpiryDate);
        buttonSave = findViewById(R.id.buttonSave);
        checkBoxCompleted = findViewById(R.id.checkBoxCompleted);
        checkBoxFavourite = findViewById(R.id.checkBoxFavourite);

        todoRepository = new TodoRepository(getApplicationContext());

        // Prüfe ob wir im Edit-Mode sind
        int todoId = getIntent().getIntExtra("todoId", -1);
        isEditMode = todoId != -1;

        if (isEditMode) {
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
        checkBoxCompleted.setChecked(todo.isDone());

        if (todo.getExpiry() > 0) {
            selectedExpiryTimestamp = todo.getExpiry();
            buttonExpiryDate.setText(dateFormat.format(new Date(selectedExpiryTimestamp)));
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

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, month1, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year1, month1, dayOfMonth, 0, 0, 0);
                    selectedDate.set(Calendar.MILLISECOND, 0);
                    selectedExpiryTimestamp = selectedDate.getTimeInMillis();
                    buttonExpiryDate.setText(dateFormat.format(new Date(selectedExpiryTimestamp)));
                }, year, month, day);

        datePickerDialog.show();
    }

    private void saveTodo() {
        String name = editTextName.getText().toString().trim();
        if (name.isEmpty()) {
            editTextName.setError("Name ist erforderlich");
            return;
        }

        if (todo == null) {
            todo = new Todo();
        }

        todo.setName(name);
        todo.setDescription(editTextDescription.getText().toString());
        todo.setExpiry(selectedExpiryTimestamp);
        todo.setFavourite(checkBoxFavourite.isChecked());
        todo.setDone(checkBoxCompleted.isChecked());

        if (isEditMode) {
            todoRepository.updateTodo(todo, () -> {
                // Nach erfolgreicher Aktualisierung
                setResult(RESULT_OK);
                finish();
            });
        } else {
            todoRepository.insertTodo(todo);
            setResult(RESULT_OK);
            finish();
        }
    }
}
