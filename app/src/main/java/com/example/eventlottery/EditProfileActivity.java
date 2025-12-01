package com.example.eventlottery;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * EditProfileActivity
 *
 * Displays editable user profile fields (email, phone, and password hint).
 * Each field can be unlocked using the pencil icon.
 * When the "Done Edit" button is pressed, updated data is saved to Firestore
 * and the activity closes with RESULT_OK.
 *
 * <p>Firestore structure:
 * users/{userId} → { name, email, phone, role, passwordHint }
 *
 * <p>Flow:
 * 1. Load user data from Firestore
 * 2. Enable individual fields on edit
 * 3. Save updates and return to previous screen
 *
 * @author junseok Song
 * @since 2025-11-06
 */
public class EditProfileActivity extends AppCompatActivity {

    // Header bits (optional action)

    // Top summary
    private TextView userRoleText;
    //back button
    private ImageView backButton;

    // Info fields
    private EditText userNameEditText, phoneEditText, emailEditText, passwordHintField;

    // Pencil buttons
    private ImageButton userNameBtn, phoneEditBtn, emailEditBtn,passwordEditBtn;

    // Done
    private Button doneButton;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Firebase init
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        // Bind UI
        backButton = findViewById(R.id.backButton3);
        userNameEditText      = findViewById(R.id.userNameEditText);
        userRoleText      = findViewById(R.id.userRoleText);

        phoneEditText     = findViewById(R.id.phoneEditText);
        emailEditText     = findViewById(R.id.emailEditText);
        passwordHintField = findViewById(R.id.passwordHintField);

        userNameBtn       = findViewById(R.id.userNameBtn);
        phoneEditBtn      = findViewById(R.id.phoneEditBtn);
        emailEditBtn      = findViewById(R.id.emailEditBtn);
        passwordEditBtn   = findViewById(R.id.passwordEditBtn);

        doneButton        = findViewById(R.id.doneButton);

        backButton.setOnClickListener(v-> finish());

        // Load current profile values
        loadProfile();

        // Pencil toggles
        userNameBtn.setOnClickListener(v -> {
            userNameEditText.setEnabled(true);
            userNameEditText.requestFocus();
            userNameEditText.setSelection(userNameEditText.getText().length());
        });

        phoneEditBtn.setOnClickListener(v -> {
            phoneEditText.setEnabled(true);
            phoneEditText.requestFocus();
            phoneEditText.setSelection(phoneEditText.getText().length());
        });

        emailEditBtn.setOnClickListener(v -> {
            emailEditText.setEnabled(true);
            emailEditText.requestFocus();
            emailEditText.setSelection(emailEditText.getText().length());
        });

        passwordEditBtn.setOnClickListener(v -> {
            passwordHintField.setEnabled(true);
            passwordHintField.requestFocus();
            passwordHintField.setSelection(passwordHintField.getText().length());
        });

        // Save + close
        doneButton.setOnClickListener(v -> saveAndClose());
    }
    /**
     * Loads the current user's profile data from Firestore and populates the UI fields.
     * If the user is not signed in or no document exists, the activity finishes.
     */
    private void loadProfile() {
        if (userId == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    String name = doc.getString("name");
                    String email = doc.getString("email");
                    String phone = doc.getString("phone");
                    String role  = doc.getString("role");

                    userNameEditText.setText(name == null ? "User" : name);
                    userRoleText.setText(role == null ? "" : role);

                    emailEditText.setText(email);
                    phoneEditText.setText(phone);

                    // Start in read-only; pencil enables each
                    userNameEditText.setEnabled(false);
                    emailEditText.setEnabled(false);
                    phoneEditText.setEnabled(false);
                    passwordHintField.setEnabled(false);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Load failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
    /**
     * Saves the updated email, phone, and password hint values to Firestore.
     * Closes the activity upon successful update.
     */
    private void saveAndClose() {
        if (userId == null) return;

        String newName = userNameEditText.getText().toString().trim();
        String newEmail = emailEditText.getText().toString().trim();
        String newPhone = phoneEditText.getText().toString().trim();
        String newPasswordHint = passwordHintField.getText().toString().trim();

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("email", newEmail);
        updates.put("phone", newPhone);
        updates.put("passwordHint", newPasswordHint);

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
