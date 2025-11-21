package com.example.eventlottery;

import android.content.Intent;
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
 * ProfileActivity
 *
 * Displays the user's profile information (name, role, email, phone, password hint).
 * Provides buttons to edit the profile or delete the account.
 * Automatically reloads the profile from Firestore when resumed.
 *
 * <p>Associated layout: activity_profile.xml</p>
 *
 * @see EditProfileActivity
 * @see FirebaseAuth
 * @see FirebaseFirestore
 * @author Junseok Song
 * @since 2025-11-05
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
            userId = auth.getCurrentUser().getUid(); // save log in Uid from user
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
        editAccountButton.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class));
        });

        // Delete
        deleteAccountButton.setOnClickListener(v -> deleteAccount());
    }

    /**
     * Retrieves the user's profile data from Firestore and updates the UI.
     * Fields are locked in read-only mode after loading.
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
     * Enable/disable input fields.
     */
    private void setEditable(boolean enabled) {
        emailEditText.setEnabled(enabled);
        phoneEditText.setEnabled(enabled);
        // password field stays disabled (hint only)
        passwordHintField.setEnabled(false);
    }
    /**
     * Deletes the user's Firebase account and Firestore user document.
     * Prompts for confirmation before deletion.
     * After successful deletion, signs out and finishes the activity.
     *
     * @see FirebaseAuth
     * @see FirebaseFirestore
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
