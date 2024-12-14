package org.dieschnittstelle.mobile.android.skeleton;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;

import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.dieschnittstelle.mobile.android.skeleton.model.Todo;
import org.dieschnittstelle.mobile.android.skeleton.repository.TodoRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TodoDetailActivity extends AppCompatActivity {

    private EditText editTextName, editTextDescription;
    private Button buttonExpiryDate, buttonSave, buttonDelete;
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

        // Initialisiere Repository
        todoRepository = new TodoRepository(this);

        // Initialisiere Views
        editTextName = findViewById(R.id.editTextName);
        editTextDescription = findViewById(R.id.editTextDescription);
        buttonExpiryDate = findViewById(R.id.buttonExpiryDate);
        buttonSave = findViewById(R.id.buttonSave);
        buttonDelete = findViewById(R.id.buttonDelete);
        checkBoxCompleted = findViewById(R.id.checkBoxCompleted);
        checkBoxFavourite = findViewById(R.id.checkBoxFavourite);

        // Prüfe, ob ein existierendes Todo bearbeitet wird
        int todoId = getIntent().getIntExtra("todoId", -1);
        isEditMode = todoId != -1;

        if (isEditMode) {
            // Lade existierendes Todo
            new Thread(() -> {
                todo = todoRepository.getTodoById(todoId);
                runOnUiThread(() -> {
                    populateFields(todo);
                    buttonDelete.setVisibility(View.VISIBLE);
                });
            }).start();
        } else {
            buttonDelete.setVisibility(View.GONE);
        }

        // Setze OnClickListener
        buttonSave.setOnClickListener(v -> saveTodo());
        buttonDelete.setOnClickListener(v -> showDeleteConfirmationDialog());
        buttonExpiryDate.setOnClickListener(v -> showDatePicker());
        
        // Lange klicken für direkte Zeitauswahl
        buttonExpiryDate.setOnLongClickListener(v -> {
            showTimePicker();
            return true;
        });

        // Setze initialen Text für den Datum/Zeit-Button
        updateDateTimeButton();
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

    private void showTimePicker() {
        final Calendar c = Calendar.getInstance();
        if (selectedExpiryTimestamp > 0) {
            c.setTimeInMillis(selectedExpiryTimestamp);
        }
    
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
    
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute1) -> {
                    Calendar selectedTime = Calendar.getInstance();
                    selectedTime.setTimeInMillis(selectedExpiryTimestamp);
                    selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    selectedTime.set(Calendar.MINUTE, minute1);
                    selectedExpiryTimestamp = selectedTime.getTimeInMillis();
                    updateDateTimeButton();
                }, hour, minute, true);
    
        timePickerDialog.show();
    }
    
    private void updateDateTimeButton() {
        if (selectedExpiryTimestamp > 0) {
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            buttonExpiryDate.setText(dateTimeFormat.format(new Date(selectedExpiryTimestamp)));
        } else {
            buttonExpiryDate.setText("Datum/Zeit wählen");
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
            new Thread(() -> {
                todoRepository.insertTodo(todo);
                runOnUiThread(() -> {
                    setResult(RESULT_OK);
                    finish();
                });
            }).start();
        }
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Todo löschen")
                .setMessage("Möchten Sie dieses Todo wirklich löschen?")
                .setPositiveButton("Ja", (dialog, which) -> deleteTodo())
                .setNegativeButton("Nein", null)
                .show();
    }
    
    private void deleteTodo() {
        if (todo != null && todo.getId() != 0) {
            new Thread(() -> {
                todoRepository.deleteTodo(todo);
                runOnUiThread(() -> {
                    setResult(RESULT_OK);
                    finish();
                });
            }).start();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("name", editTextName.getText().toString());
        outState.putString("description", editTextDescription.getText().toString());
        outState.putLong("expiryTimestamp", selectedExpiryTimestamp);
        outState.putBoolean("isFavorite", checkBoxFavourite.isChecked());
        outState.putBoolean("isCompleted", checkBoxCompleted.isChecked());
        if (todo != null) {
            outState.putInt("todoId", todo.getId());
        }
    }
    
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        editTextName.setText(savedInstanceState.getString("name"));
        editTextDescription.setText(savedInstanceState.getString("description"));
        selectedExpiryTimestamp = savedInstanceState.getLong("expiryTimestamp");
        checkBoxFavourite.setChecked(savedInstanceState.getBoolean("isFavorite"));
        checkBoxCompleted.setChecked(savedInstanceState.getBoolean("isCompleted"));
        updateDateTimeButton();
    }
}
