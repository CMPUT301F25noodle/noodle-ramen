package com.example.eventlottery;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import android.content.Intent;

/**
 * loginactivity - handles user login
 * created by: ibrahim
 */

public class LoginActivity extends AppCompatActivity {

    private EditText emailField, passwordField;
    private TextView signUpRedirect;
    private Button loginButton;
    private CheckBox rememberMe;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        initializeViews();
        setupListeners();
        checkRememberedUser();
    }

    /**
     * connects xml elements with java
     */
    private void initializeViews() {
        emailField = findViewById(R.id.editTextTextEmailAddress);
        passwordField = findViewById(R.id.editTextTextPassword);
        signUpRedirect = findViewById(R.id.textView5);
        rememberMe = findViewById(R.id.checkboxRememberMe);
        loginButton = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
    }

    /**
     * sets up button actions
     */
    private void setupListeners() {
        loginButton.setOnClickListener(v -> handleLogin());

        /**
         * handles sign up redirect message
         */
        signUpRedirect.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }


    /**
     * handles user login with firebase
     */
    private void handleLogin() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (!validateInputs(email, password)) return;

        showLoading(true);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            if (rememberMe.isChecked()) {
                                saveLogin(email, password);
                            }
                            // Fetch user role from Firestore
                            fetchUserRole(user.getUid());
                        }
                    } else {
                        Exception e = task.getException();
                        if (e instanceof FirebaseAuthInvalidUserException) {
                            emailField.setError("no account found");
                        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            passwordField.setError("wrong password");
                        } else {
                            Toast.makeText(this, "login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /**
     * checks for valid inputs
     */
    private boolean validateInputs(String email, String password) {
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailField.setError("enter a valid email");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            passwordField.setError("enter password");
            return false;
        }
        return true;
    }

    /**
     * shows or hides progress bar
     */
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!show);
    }

    /**
     * saves login info if remember me is checked
     */
    private void saveLogin(String email, String password) {
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("email", email);
        editor.putString("password", password);
        editor.putBoolean("remember", true);
        editor.apply();
    }

    /**
     * checks if user info is saved
     */
    private void checkRememberedUser() {
        SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        if (prefs.getBoolean("remember", false)) {
            String email = prefs.getString("email", "");
            String password = prefs.getString("password", "");
            emailField.setText(email);
            passwordField.setText(password);
            rememberMe.setChecked(true);
        }
    }

    /**
     * fetches user role from Firestore and saves to SharedPreferences
     */
    private void fetchUserRole(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        if (role != null) {
                            // Save role to SharedPreferences
                            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("userRole", role);
                            editor.apply();
                        }
                        Toast.makeText(this, "login successful", Toast.LENGTH_SHORT).show();

                        // Navigate to MainActivity when role is user or organizer or
                        // Navigate to AdminMainActivity when role is admin
                        if ("admin".equals(role)) {

                            Intent intent = new Intent(LoginActivity.this, AdminMainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);

                        } else {

                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        }

                        finish();
                    } else {
                        Toast.makeText(this, "user data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "failed to fetch user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
