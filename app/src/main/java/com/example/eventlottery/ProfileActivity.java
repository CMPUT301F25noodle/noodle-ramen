package com.example.eventlottery;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * ProfileActivity (Activity version)
 * - Loads user profile from Firestore
 * - Allows edit (toggle enable -> update -> lock)
 * - Allows delete account
 * NOTE: Password is never displayed; show a hint only.
 */
public class ProfileActivity extends AppCompatActivity {

    // Header
    private ImageView notificationIcon;

    // Profile summary
    private TextView userNameText, userRoleText;

    // Info fields
    private EditText phoneEditText, emailEditText, passwordHintField;

    // Actions
    private Button editAccountButton, deleteAccountButton;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String userId;

    // Simple flag to toggle edit mode
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Init Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (auth.getCurrentUser() != null) {
            userId = auth.getCurrentUser().getUid();
        }

        // Bind UI
        notificationIcon   = findViewById(R.id.notificationIcon);
        userNameText       = findViewById(R.id.userNameText);
        userRoleText       = findViewById(R.id.userRoleText);
        phoneEditText      = findViewById(R.id.phoneEditText);
        emailEditText      = findViewById(R.id.emailEditText);
        passwordHintField  = findViewById(R.id.passwordHintField);
        editAccountButton  = findViewById(R.id.editAccountButton);
        deleteAccountButton= findViewById(R.id.deleteAccountButton);

        // Load profile on start
        loadProfileData();

        // Notification (stub)
        notificationIcon.setOnClickListener(v ->
                Toast.makeText(this, "Notifications screen TBD", Toast.LENGTH_SHORT).show()
        );

        // Edit / Save
        editAccountButton.setOnClickListener(v -> onEditOrSave());

        // Delete
        deleteAccountButton.setOnClickListener(v -> deleteAccount());
    }

    /**
     * Reads the user document from Firestore and fills UI.
     * Fields are locked initially.
     */
    private void loadProfileData() {
        if (userId == null) return;

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Profile not found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String name = doc.getString("name");
                    String email = doc.getString("email");
                    String phone = doc.getString("phone");
                    String role  = doc.getString("role");

                    // Fill UI
                    userNameText.setText(name == null ? "User" : name);
                    userRoleText.setText(role == null ? "Role" : role);
                    emailEditText.setText(email);
                    phoneEditText.setText(phone);

                    // lock fields
                    setEditable(false);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Toggles between Edit mode and Save action.
     */
    private void onEditOrSave() {
        if (!isEditMode) {
            // Enter edit mode
            setEditable(true);
            isEditMode = true;
            editAccountButton.setText("Save");
            Toast.makeText(this, "Edit mode enabled", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save changes to Firestore
        String newEmail = emailEditText.getText().toString().trim();
        String newPhone = phoneEditText.getText().toString().trim();

        Map<String, Object> updates = new HashMap<>();
        updates.put("email", newEmail);
        updates.put("phone", newPhone);

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                    setEditable(false);
                    isEditMode = false;
                    editAccountButton.setText("Edit Account");
                    loadProfileData(); // refresh UI
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Enable/disable input fields.
     */
    private void setEditable(boolean enabled) {
        emailEditText.setEnabled(enabled);
        phoneEditText.setEnabled(enabled);
        // password field stays disabled (hint only)
        passwordHintField.setEnabled(false);
    }

    /**
     * Deletes the Firebase user account.
     * (Consider adding a confirmation dialog in production.)
     */
    private void deleteAccount() {
        if (auth.getCurrentUser() == null) return;

        auth.getCurrentUser().delete()
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show();
                    auth.signOut();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
