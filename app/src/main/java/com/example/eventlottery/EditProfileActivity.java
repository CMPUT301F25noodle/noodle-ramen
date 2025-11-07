package com.example.eventlottery;

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
 * - Shows the "edit mode" UI (second screenshot)
 * - Pencil icons enable each field
 * - "Done Edit" saves to Firestore and closes
 */
public class EditProfileActivity extends AppCompatActivity {

    // Header bits (optional action)
    private ImageView notificationIcon;

    // Top summary
    private TextView userNameText, userRoleText;

    // Info fields
    private EditText phoneEditText, emailEditText, passwordHintField;

    // Pencil buttons
    private ImageButton phoneEditBtn, emailEditBtn,passwordEditBtn;

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
        notificationIcon  = findViewById(R.id.notificationIcon);
        userNameText      = findViewById(R.id.userNameText);
        userRoleText      = findViewById(R.id.userRoleText);

        phoneEditText     = findViewById(R.id.phoneEditText);
        emailEditText     = findViewById(R.id.emailEditText);
        passwordHintField = findViewById(R.id.passwordHintField);

        phoneEditBtn      = findViewById(R.id.phoneEditBtn);
        emailEditBtn      = findViewById(R.id.emailEditBtn);
        passwordEditBtn   = findViewById(R.id.passwordEditBtn);

        doneButton        = findViewById(R.id.doneButton);

        // Load current profile values
        loadProfile();

        // Pencil toggles
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

                    userNameText.setText(name == null ? "User" : name);
                    userRoleText.setText(role == null ? "" : role);

                    emailEditText.setText(email);
                    phoneEditText.setText(phone);

                    // Start in read-only; pencil enables each
                    emailEditText.setEnabled(false);
                    phoneEditText.setEnabled(false);
                    passwordHintField.setEnabled(false);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Load failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void saveAndClose() {
        if (userId == null) return;

        String newEmail = emailEditText.getText().toString().trim();
        String newPhone = phoneEditText.getText().toString().trim();
        String newPasswordHint = passwordHintField.getText().toString().trim();

        Map<String, Object> updates = new HashMap<>();
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
