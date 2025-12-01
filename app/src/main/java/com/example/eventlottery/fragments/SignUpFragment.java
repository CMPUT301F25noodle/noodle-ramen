package com.example.eventlottery.fragments;
import com.example.eventlottery.R;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.eventlottery.MainActivity;
import com.example.eventlottery.AdminMainActivity;

import android.content.Intent;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * SignUpFragment - Handles user sign up page
 * Created by: Mataab
 * ..... gotta add more info/ javadoc
 *
 */

public class SignUpFragment extends Fragment {
    private EditText nameField, phoneField, emailField;
    public final int brand_green = R.color.brand_green;
    private Button organizerButton, entrantButton, signUpButton;
    private RadioGroup notificationsGroup;
    private ProgressBar progressBar;
    private TextView loadingText;
//sets up firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String selectedRole = "entrant"; //everyone will deafault to entrant
    private boolean notificationsEnabled = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sign_up, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews(view);
        setupListeners();

        return view;

    }

    /**
     * Initilaizes all the view components
     * @param view
     */

    private void initializeViews(View view) {
        organizerButton = view.findViewById(R.id.btn_organizer);
        entrantButton = view.findViewById(R.id.btn_entrant);
        signUpButton = view.findViewById(R.id.btn_sign_up);

        //inputs
        nameField = view.findViewById(R.id.et_name);
        phoneField = view.findViewById(R.id.et_phone);
        emailField = view.findViewById(R.id.et_email);
        //notificaitons prefernce at sign up, will need one for the profile page
        notificationsGroup = view.findViewById(R.id.rg_notifications);

        //progess bar
        progressBar = view.findViewById(R.id.progress_bar);
        loadingText = view.findViewById(R.id.tv_loading);

    }

    /**
     * set up listeners for clicks
     *
     */

    private void setupListeners() {
        organizerButton.setOnClickListener( v-> {
            selectedRole = "organizer";
            updateRoleButtonStyles();
        });

        entrantButton.setOnClickListener(v-> {
            selectedRole = "entrant";
            updateRoleButtonStyles();
        });

        notificationsGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_yes) {
                notificationsEnabled = true;
            } else if (checkedId == R.id.rb_no) {
                notificationsEnabled = false;
            }
        });

        signUpButton.setOnClickListener(v-> handleSignUp());
        }



        private void updateRoleButtonStyles () {
            if (selectedRole.equals("organizer")) {
                organizerButton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.brand_green));
                entrantButton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.darker_gray));
            } else {
                entrantButton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.brand_green));
                organizerButton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.darker_gray));
            }
        }

    /**
     * sign up handler wil validate inputs and create account
     */

    private void handleSignUp() {
        // Get input values
        String name = nameField.getText().toString().trim();
        String phone = phoneField.getText().toString().trim();
        String email = emailField.getText().toString().trim();

        // Validate inputs
        if (!validateInputs(name, email)) {
            return;
        }

        // Disable button and show progress
        signUpButton.setEnabled(false);
        showLoading(true);

        // Check if email already exists, then create account
        checkEmailExists(email, exists -> {
            if (exists) {
                signUpButton.setEnabled(true);
                showLoading(false);
                emailField.setError("An account with this email already exists");
                Toast.makeText(getContext(), "Email already registered.", Toast.LENGTH_LONG).show();
            } else {
                createUserAccount(name, email, phone);
            }
        });
    }

    /**
     * validaitng user inputs
     */
    private boolean validateInputs(String name, String email) {
        boolean isValid = true;

        if (name.isEmpty()) {
            nameField.setError("Please enter your name");
            nameField.requestFocus();
            isValid = false;
        }
        if (email.isEmpty()) {
            emailField.setError("Please enter your email");
            emailField.requestFocus();
            isValid = false;

        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailField.setError("Please enter a valid email address");
            emailField.requestFocus();
            isValid = false;
        }

        return isValid;

    }
    /**
     * check if email already in db
     * @param email we check
     * @param callback Callback if its true we have email already or false doesnst exist
     */
    private void checkEmailExists(String email, EmailCheckCallback callback) {
        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    callback.onResult(!queryDocumentSnapshots.isEmpty());
                })
                .addOnFailureListener(e-> {
                    // Permission denied is expected for unauthenticated users
                    // Proceed with sign-up - duplicate emails will be caught by unique user IDs
                    callback.onResult(false);
                });


    }

    /**
     * create user account in firebase using anonymous authentication
     */

    private void createUserAccount(String name, String email, String phone) {
        mAuth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Anonymous sign in success
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Create user profile in Firestore
                            createUserProfile(user.getUid(), name, email, phone);
                        }
                    } else {
                        // Sign up failed
                        signUpButton.setEnabled(true);
                        showLoading(false);

                        String errorMessage = "Sign up failed";
                        if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();
                        }
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * create user profile in firestore
     */

    private  void createUserProfile(String userId, String name, String email, String phone) {
        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("userId", userId);
        userProfile.put("name", name);
        userProfile.put("email", email);
        userProfile.put("phone", phone.isEmpty() ? null : phone); // Store null if phone not provided
        userProfile.put("role", selectedRole);
        userProfile.put("notificationsEnabled", notificationsEnabled);
        userProfile.put("createdAt", System.currentTimeMillis());
        userProfile.put("deviceId", android.provider.Settings.Secure.getString(
                getContext().getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID)); // gets the device id so we dont need to sign in again

        // Add role-specific fields
        if (selectedRole.equals("entrant")) {
            userProfile.put("eventHistory", new HashMap<>());
            userProfile.put("waitingLists", new HashMap<>());
        } else if (selectedRole.equals("organizer")) {
            userProfile.put("eventsCreated", new HashMap<>());
        }

        db.collection("users").document(userId)
                .set(userProfile)
                .addOnSuccessListener(aVoid -> {
                    signUpButton.setEnabled(true);
                    showLoading(false);

                    navigateToHomePage();
                })
                .addOnFailureListener(e -> {
                    signUpButton.setEnabled(true);
                    showLoading(false);

                    // Profile creation failed
                    FirebaseUser currentUser = mAuth.getCurrentUser();
                    if (currentUser != null) {
                        currentUser.delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(),
                                            "Sign up failed. Please try again.",
                                            Toast.LENGTH_LONG).show();
                                })
                                .addOnFailureListener(deleteError -> {
                                    Toast.makeText(getContext(),
                                            "Error during sign up. Please contact support with error: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    } else {
                        Toast.makeText(getContext(),
                                "Failed to create profile: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }


                });
    }

    private void navigateToHomePage() {
        // Save user role to SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", requireContext().MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("userRole", selectedRole);
        editor.apply();

        // Navigate based on role
        Intent intent;
        if ("admin".equals(selectedRole)) {
            intent = new Intent(getActivity(), AdminMainActivity.class);
        } else {
            intent = new Intent(getActivity(), MainActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    /**
     * loading bar so the app looks cool
     */
    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);

        }
        if (loadingText !=null) {
            loadingText.setVisibility(show ? View.VISIBLE :View.GONE);

        }
    }

    /**
     * call back for checking if email exsists
     */
    private interface EmailCheckCallback {
        void onResult (boolean exists) ;

    }



}
