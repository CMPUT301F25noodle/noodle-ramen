package com.example.eventlottery.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eventlottery.EditProfileActivity;
import com.example.eventlottery.SplashActivity;
import com.example.eventlottery.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * ProfileFragment
 *
 * Displays the logged-in user's profile inside a Fragment-based UI.
 * Allows navigation to EditProfileActivity for profile updates and
 * deletion of the user's account.
 *
 * Automatically reloads updated user information when the fragment resumes.
 *
 * @author Junseok Song
 * @since 2025-11-06
 */
public class ProfileFragment extends Fragment {

    // UI fields
    private TextView nameTextView, roleTextView;
    private EditText emailEditText, phoneEditText;
    private Button editAccountButton, deleteAccountButton;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String userId;

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Firebase init
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (auth.getCurrentUser() != null) {
            userId = auth.getCurrentUser().getUid();
        }

        // UI init
        nameTextView = view.findViewById(R.id.userNameText);
        roleTextView = view.findViewById(R.id.userRoleText);
        emailEditText = view.findViewById(R.id.emailEditText);
        phoneEditText = view.findViewById(R.id.phoneEditText);
        editAccountButton = view.findViewById(R.id.editAccountButton);
        deleteAccountButton = view.findViewById(R.id.deleteAccountButton);

        // Load profile info
        loadProfileData();

        // Buttons
        editAccountButton.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), EditProfileActivity.class));
        });
        deleteAccountButton.setOnClickListener(v -> deleteAccount());

        return view;
    }
    /**
     * Called when the fragment becomes visible again.
     * Reloads the latest profile data from Firestore.
     */
    @Override
    public void onResume() {
        super.onResume();
        loadProfileData();  // call new data
    }

    /**
     * Load profile data from Firestore
     */
    private void loadProfileData() {
        if (userId == null) return;

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        String phone = doc.getString("phone");
                        String role = doc.getString("role");

                        nameTextView.setText(name);
                        roleTextView.setText(role);
                        emailEditText.setText(email);
                        phoneEditText.setText(phone);

                        // lock editing
                        emailEditText.setEnabled(false);
                        phoneEditText.setEnabled(false);
                    } else {
                        Toast.makeText(getContext(), "Profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Confirms and deletes the user's account from both Firebase Authentication
     * and Firestore. After deletion, the user is signed out and redirected to
     * the login screen.
     *
     * @see FirebaseAuth
     * @see FirebaseFirestore
     */
    private void deleteAccount() {
        if (auth.getCurrentUser() == null || userId == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Yes", (dialog, which) -> {

                    // Delete user document from Firestore
                    db.collection("users").document(userId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {

                                // Delete the Firebase Authentication user
                                auth.getCurrentUser().delete()
                                        .addOnSuccessListener(v -> {
                                            Toast.makeText(getContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show();

                                            // Sign out and redirect to splash screen
                                            auth.signOut();
                                            Intent intent = new Intent(requireContext(), SplashActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                            requireActivity().finish();
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(getContext(), "Failed to delete authentication account: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                        );

                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(), "Failed to delete Firestore data: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );

                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
