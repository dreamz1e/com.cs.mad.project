package org.dieschnittstelle.mobile.android.skeleton;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.dieschnittstelle.mobile.android.skeleton.model.Todo;
import org.dieschnittstelle.mobile.android.skeleton.model.TodoContact;
import org.dieschnittstelle.mobile.android.skeleton.repository.TodoRepository;
import org.dieschnittstelle.mobile.android.skeleton.adapter.TodoContactAdapter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.List;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import retrofit2.Response;

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

    private static final int PICK_CONTACT_REQUEST = 1;
    private RecyclerView recyclerViewContacts;
    private TodoContactAdapter contactAdapter;

    private static final int PERMISSION_REQUEST_READ_CONTACTS = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo_detail);

        // Repository initialisieren
        todoRepository = new TodoRepository(this);

        // Initialisiere Views
        initializeViews();

        // Todo aus Intent laden
        if (getIntent().hasExtra("todo")) {
            todo = (Todo) getIntent().getSerializableExtra("todo");
        } else {
            todo = new Todo(); // Erstelle neues Todo wenn keins übergeben wurde
        }

        // RecyclerView für Kontakte einrichten
        recyclerViewContacts = findViewById(R.id.recyclerViewContacts);
        recyclerViewContacts.setLayoutManager(new LinearLayoutManager(this));
        contactAdapter = new TodoContactAdapter(new TodoContactAdapter.OnContactActionListener() {
            @Override
            public void onDeleteContact(TodoContact contact) {
                deleteContact(contact);
            }

            @Override
            public void onEmailContact(TodoContact contact) {
                sendEmail(contact);
            }

            @Override
            public void onSmsContact(TodoContact contact) {
                sendSms(contact);
            }
        });
        recyclerViewContacts.setAdapter(contactAdapter);

        // UI mit Todo-Daten füllen
        if (todo != null) {
            editTextName.setText(todo.getName());
            editTextDescription.setText(todo.getDescription());
            checkBoxFavourite.setChecked(todo.isFavourite());
            checkBoxCompleted.setChecked(todo.isDone());
            
            if (todo.getExpiry() > 0) {
                selectedExpiryTimestamp = todo.getExpiry();
                buttonExpiryDate.setText(dateFormat.format(new Date(selectedExpiryTimestamp)));
            }

            // Kontakte nur laden wenn Todo bereits existiert (ID > 0)
            if (todo.getId() > 0) {
                loadContacts();
            }
        }

        // Button zum Hinzufügen von Kontakten
        Button buttonAddContact = findViewById(R.id.buttonAddContact);
        buttonAddContact.setOnClickListener(v -> pickContact());
    }

    private void initializeViews() {
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
        // Hole die Werte aus den Eingabefeldern
        String name = editTextName.getText().toString();
        String description = editTextDescription.getText().toString();
        boolean important = checkBoxFavourite.isChecked();
        boolean completed = checkBoxCompleted.isChecked();

        // Aktualisiere das Todo-Objekt
        todo.setName(name);
        todo.setDescription(description);
        todo.setFavourite(important);
        todo.setDone(completed);
        todo.setExpiry(selectedExpiryTimestamp);

        // Speichere das Todo und die Kontakte
        new Thread(() -> {
            // Temporär die komplexen Kontakte speichern
            List<TodoContact> tempContacts = todo.getContacts();
            
            // Erstelle eine Liste von Kontakt-IDs für den Server
            List<String> contactIds = new ArrayList<>();
            if (tempContacts != null) {
                for (TodoContact contact : tempContacts) {
                    contactIds.add(contact.getContactId());
                }
            }
            
            // Setze die Kontakt-IDs für den Server-Request
            todo.setContacts(null); // Zuerst auf null setzen
            try {
                // Setze die String-Liste der Kontakt-IDs
                java.lang.reflect.Field contactsField = Todo.class.getDeclaredField("contacts");
                contactsField.setAccessible(true);
                contactsField.set(todo, contactIds);

                todoRepository.updateTodo(todo, () -> {
                    // Nach erfolgreichem Update die komplexen Kontakte wiederherstellen
                    todo.setContacts(tempContacts);
                    
                    runOnUiThread(() -> {
                        setResult(RESULT_OK);
                        finish();
                    });
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(TodoDetailActivity.this, 
                        "Fehler beim Speichern der Kontakte", 
                        Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
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

    private void pickContact() {
        requestContactPermission();
    }

    private void sendEmail(TodoContact contact) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{contact.getContactEmail()});
        intent.putExtra(Intent.EXTRA_SUBJECT, todo.getName());
        intent.putExtra(Intent.EXTRA_TEXT, todo.getDescription());
        startActivity(Intent.createChooser(intent, "E-Mail senden..."));
    }

    private void sendSms(TodoContact contact) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("smsto:" + contact.getContactPhone()));
        intent.putExtra("sms_body", todo.getName() + "\n" + todo.getDescription());
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_CONTACT_REQUEST && resultCode == RESULT_OK) {
            Uri contactUri = data.getData();
            addSelectedContact(contactUri);
        }
    }

    private void addSelectedContact(Uri contactUri) {
        if (todo.getId() == 0) {
            Toast.makeText(this, "Bitte speichern Sie das Todo zuerst", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentResolver contentResolver = getContentResolver();
        try {
            Cursor cursor = contentResolver.query(contactUri, null, null, null, null);
            
            if (cursor != null && cursor.moveToFirst()) {
                @SuppressLint("Range") String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                @SuppressLint("Range")String contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                
                String email = getContactEmail(contactId);
                String phone = getContactPhone(contactId);
                
                TodoContact todoContact = new TodoContact();
                todoContact.setTodoId(todo.getId());
                todoContact.setContactId(contactId);
                todoContact.setContactName(contactName);
                todoContact.setContactEmail(email);
                todoContact.setContactPhone(phone);
                
                // Füge Kontakt zur Liste im Todo hinzu
                if (todo.getContacts() == null) {
                    todo.setContacts(new ArrayList<>());
                }
                todo.getContacts().add(todoContact);
                
                new Thread(() -> {
                    todoRepository.insertTodoContact(todoContact);
                    runOnUiThread(() -> {
                        contactAdapter.setContacts(todo.getContacts());
                        Toast.makeText(this, "Kontakt hinzugefügt", Toast.LENGTH_SHORT).show();
                    });
                }).start();
                
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                Toast.makeText(this, "Fehler beim Hinzufügen des Kontakts", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void deleteContact(TodoContact contact) {
        new Thread(() -> {
            todoRepository.deleteTodoContact(contact);
            todo.getContacts().remove(contact);
            runOnUiThread(() -> {
                contactAdapter.setContacts(todo.getContacts());
                Toast.makeText(this, "Kontakt entfernt", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    @SuppressLint("Range")
    private String getContactEmail(String contactId) {
        String email = null;
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                new String[]{contactId},
                null);

        if (cursor != null && cursor.moveToFirst()) {
            email = cursor.getString(cursor.getColumnIndex(
                    ContactsContract.CommonDataKinds.Email.DATA));
            cursor.close();
        }
        return email;
    }

    @SuppressLint("Range")
    private String getContactPhone(String contactId) {
        String phone = null;
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                new String[]{contactId},
                null);

        if (cursor != null && cursor.moveToFirst()) {
            phone = cursor.getString(cursor.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.NUMBER));
            cursor.close();
        }
        return phone;
    }

    private void loadContacts() {
        if (todo == null || todo.getId() == 0) return;

        new Thread(() -> {
            // Lade lokale Kontakte
            List<TodoContact> localContacts = todoRepository.getContactsForTodo(todo.getId());
            
            // Hole Server-Kontakt-IDs
            List<String> serverContactIds = todo.getContactIds();
            
            // Wenn Server-Kontakte vorhanden sind, aber keine lokalen
            if ((serverContactIds != null && !serverContactIds.isEmpty()) && 
                (localContacts == null || localContacts.isEmpty())) {
                
                // Erstelle lokale Kontakte aus Server-IDs
                localContacts = new ArrayList<>();
                for (String contactId : serverContactIds) {
                    ContentResolver contentResolver = getContentResolver();
                    Cursor cursor = contentResolver.query(
                            ContactsContract.Contacts.CONTENT_URI,
                            null,
                            ContactsContract.Contacts._ID + " = ?",
                            new String[]{contactId},
                            null);

                    if (cursor != null && cursor.moveToFirst()) {
                        String contactName = cursor.getString(
                                cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                        
                        TodoContact todoContact = new TodoContact();
                        todoContact.setTodoId(todo.getId());
                        todoContact.setContactId(contactId);
                        todoContact.setContactName(contactName);
                        todoContact.setContactEmail(getContactEmail(contactId));
                        todoContact.setContactPhone(getContactPhone(contactId));
                        
                        // Speichere den Kontakt in der lokalen Datenbank
                        todoRepository.insertTodoContact(todoContact);
                        localContacts.add(todoContact);
                        
                        cursor.close();
                    }
                }
            }

            // Setze die Kontakte im Todo und aktualisiere die UI
            final List<TodoContact> finalContacts = localContacts;
            if (finalContacts != null) {
                todo.setContacts(finalContacts);
                runOnUiThread(() -> {
                    contactAdapter.setContacts(finalContacts);
                });
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Lade Kontakte neu, wenn die Activity wieder sichtbar wird
        if (todo != null && todo.getId() > 0) {
            loadContacts();
        }
    }

    private void requestContactPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    PERMISSION_REQUEST_READ_CONTACTS);
        } else {
            startContactPicker();
        }
    }

    private void startContactPicker() {
        Intent pickContactIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(pickContactIntent, PICK_CONTACT_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startContactPicker();
            } else {
                Toast.makeText(this, "Berechtigung zum Lesen der Kontakte erforderlich",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void saveContacts() {
        // Wird beim Speichern des Todos aufgerufen
        new Thread(() -> {
            // Speichere Todo und Kontakte
            todoRepository.updateTodo(todo, () -> {
                // Callback nach erfolgreichem Update
                for (TodoContact contact : todo.getContacts()) {
                    if (contact.getId() == 0) { // Neue Kontakte einfügen
                        todoRepository.insertTodoContact(contact);
                    }
                }
            });
        }).start();
    }

    @Override
    public void onBackPressed() {
        // Speichere Änderungen bevor die Activity beendet wird
        saveTodo();
    }
}
