package com.cs.mad.project;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cs.mad.project.model.User;
import com.cs.mad.project.remote.RetrofitClient;
import com.cs.mad.project.remote.ITodoAPIService;
import com.cs.mad.project.util.MADAsyncTask;

import retrofit2.Call;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private TextView textViewEmailError, textViewPasswordError, textViewLoginError;
    private Button buttonLogin;
    private ProgressDialog progressDialog;

    private boolean isEmailValid = false;
    private boolean isPasswordValid = false;
    private boolean isWebAvailable = true; // Setze dies entsprechend deiner Logik

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialisiere die Views
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);

        textViewEmailError = findViewById(R.id.textViewEmailError);
        textViewPasswordError = findViewById(R.id.textViewPasswordError);
        textViewLoginError = findViewById(R.id.textViewLoginError);

        buttonLogin = findViewById(R.id.buttonLogin);

        // Prüfe die Verfügbarkeit der Webanwendung
        checkWebApplicationAvailability();

        // Setze TextWatcher für die Eingabefelder
        editTextEmail.addTextChangedListener(emailTextWatcher);
        editTextPassword.addTextChangedListener(passwordTextWatcher);

        // Setze OnFocusChangeListener für die Validierung nach Eingabe
        editTextEmail.setOnFocusChangeListener(emailFocusChangeListener);
        editTextPassword.setOnFocusChangeListener(passwordFocusChangeListener);

        // Setze OnClickListener für den Login-Button
        buttonLogin.setOnClickListener(v -> {
            // Asynchrone Anmeldung durchführen
            new LoginTask().execute(editTextEmail.getText().toString(), editTextPassword.getText().toString());
        });
    }

    // TextWatcher für das Email-Feld
    private TextWatcher emailTextWatcher = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            isEmailValid = false;
            // Fehlermeldung entfernen
            textViewEmailError.setVisibility(View.GONE);
            textViewLoginError.setVisibility(View.GONE);
            updateLoginButtonState();
        }

        // Vorherige Methoden sind hier leer, werden aber benötigt
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
    };

    // TextWatcher für das Passwort-Feld
    private TextWatcher passwordTextWatcher = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            isPasswordValid = false;
            // Fehlermeldung entfernen
            textViewPasswordError.setVisibility(View.GONE);
            textViewLoginError.setVisibility(View.GONE);
            updateLoginButtonState();
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
    };

    // FocusChangeListener für das Email-Feld
    private View.OnFocusChangeListener emailFocusChangeListener = (v, hasFocus) -> {
        if (!hasFocus) {
            validateEmail();
        }
    };

    // FocusChangeListener für das Passwort-Feld
    private View.OnFocusChangeListener passwordFocusChangeListener = (v, hasFocus) -> {
        if (!hasFocus) {
            validatePassword();
        }
    };

    // Methoden zur Validierung
    private void validateEmail() {
        String emailInput = editTextEmail.getText().toString().trim();
        if (emailInput.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            textViewEmailError.setVisibility(View.VISIBLE);
            isEmailValid = false;
        } else {
            textViewEmailError.setVisibility(View.GONE);
            isEmailValid = true;
        }
        updateLoginButtonState();
    }

    private void validatePassword() {
        String passwordInput = editTextPassword.getText().toString().trim();
        if (passwordInput.length() != 6 || !passwordInput.matches("\\d{6}")) {
            textViewPasswordError.setVisibility(View.VISIBLE);
            isPasswordValid = false;
        } else {
            textViewPasswordError.setVisibility(View.GONE);
            isPasswordValid = true;
        }
        updateLoginButtonState();
    }

    // Methode zur Aktualisierung des Login-Buttons
    private void updateLoginButtonState() {
        buttonLogin.setEnabled(isEmailValid && isPasswordValid);
    }

    // Asynchrone Login-Task
    private class LoginTask extends MADAsyncTask<String, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            // ProgressDialog anzeigen
            progressDialog = new ProgressDialog(LoginActivity.this);
            progressDialog.setMessage("Anmeldung wird überprüft...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            // Verzögerung einfügen
            try {
                Thread.sleep(2000); // 2 Sekunden Verzögerung
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Überprüfung der Anmeldedaten
            String email = params[0];
            String password = params[1];

            if (isWebAvailable) {
                // Anmeldedaten an die Webanwendung senden
                ITodoAPIService apiService = RetrofitClient.getInstance().getApiService();

                User user = new User();
                user.setEmail(email);
                user.setPwd(password);

                Call<Boolean> call = apiService.authenticateUser(user);
                try {
                    Response<Boolean> response = call.execute();
                    if (response.isSuccessful() && Boolean.TRUE.equals(response.body())) {
                        return true; // Anmeldung erfolgreich
                    } else {
                        return false; // Anmeldung fehlgeschlagen
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return false; // Fehler bei der Anfrage
                }
            } else {
                // Webanwendung nicht verfügbar
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            // ProgressDialog ausblenden
            progressDialog.dismiss();

            if (success) {
                // Weiter zur MainActivity
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                // Fehlermeldung anzeigen
                textViewLoginError.setVisibility(View.VISIBLE);
            }
        }
    }

    // Methode zur Überprüfung der Verfügbarkeit der Webanwendung
    private void checkWebApplicationAvailability() {
        // Implementiere hier deine Logik zur Überprüfung
        // Setze 'isWebAvailable' entsprechend
        // Wenn die Webanwendung nicht verfügbar ist, direkt zur MainActivity wechseln

        // Beispielhaft (sollte durch eine echte Überprüfung ersetzt werden)
        isWebAvailable = true; // oder false

        if (!isWebAvailable) {
            Toast.makeText(this, "Webanwendung nicht verfügbar. Anzeige der Todos.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("email", editTextEmail.getText().toString());
        outState.putString("password", editTextPassword.getText().toString());
        outState.putBoolean("isEmailValid", isEmailValid);
        outState.putBoolean("isPasswordValid", isPasswordValid);
        outState.putBoolean("isWebAvailable", isWebAvailable);

        // Zustand der Fehlermeldungen speichern
        outState.putInt("emailErrorVisibility", textViewEmailError.getVisibility());
        outState.putInt("passwordErrorVisibility", textViewPasswordError.getVisibility());
        outState.putInt("loginErrorVisibility", textViewLoginError.getVisibility());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        editTextEmail.setText(savedInstanceState.getString("email"));
        editTextPassword.setText(savedInstanceState.getString("password"));
        isEmailValid = savedInstanceState.getBoolean("isEmailValid");
        isPasswordValid = savedInstanceState.getBoolean("isPasswordValid");
        isWebAvailable = savedInstanceState.getBoolean("isWebAvailable");

        // Zustand der Fehlermeldungen wiederherstellen
        textViewEmailError.setVisibility(savedInstanceState.getInt("emailErrorVisibility"));
        textViewPasswordError.setVisibility(savedInstanceState.getInt("passwordErrorVisibility"));
        textViewLoginError.setVisibility(savedInstanceState.getInt("loginErrorVisibility"));

        // Login-Button Zustand aktualisieren
        updateLoginButtonState();
    }
}
