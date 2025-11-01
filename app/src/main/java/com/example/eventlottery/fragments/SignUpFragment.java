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

import androidx.fragment.app.Fragment;




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
    private EditText nameField, phoneField, emailField, passwordField;
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
        passwordField = view.findViewById(R.id.et_password);
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
            Toast.makeText(getContext(), "Organizer role selected", Toast.LENGTH_SHORT).show();
        });

        entrantButton.setOnClickListener(v-> {
            selectedRole = "entrant";
            updateRoleButtonStyles();
            Toast.makeText(getContext(), "Entrant role selected", Toast.LENGTH_SHORT).show();
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
                organizerButton.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_purple));
                entrantButton.setBackgroundTintList(getResources().getColorStateList(android.R.color.darker_gray));
            } else {
                entrantButton.setBackgroundTintList(getResources().getColorStateList(android.R.color.holo_purple));
                organizerButton.setBackgroundTintList(getResources().getColorStateList(android.R.color.darker_gray));
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
        String password = passwordField.getText().toString().trim();

        // Validate inputs
        if (!validateInputs(name, email, password)) {
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
                Toast.makeText(getContext(), "Email already registered. Please login instead.", Toast.LENGTH_LONG).show();
            } else {
                createUserAccount(name, email, password, phone);
            }
        });
    }

    /**
     * validaitng user inputs
     */
    private boolean validateInputs(String name, String email, String password) {
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
        if (password.isEmpty()) {
            passwordField.setError("Please enter a password");
            passwordField.requestFocus();
            isValid = false;
        } else if (password.length() < 6) {
            passwordField.setError("Password must be at least 6 characters");
            passwordField.requestFocus();
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
                    Toast.makeText(getContext(), "Error checking email" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    callback.onResult(false);
                });


    }

    /**
     * create user account in firebase
     */

    private void createUserAccount(String name, String email, String password, String phone) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Sign up success
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

                    Toast.makeText(getContext(), "Account created successfully!", Toast.LENGTH_SHORT).show();


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
        Toast.makeText(getContext(), "Welcome! Home page not yet implemented.", Toast.LENGTH_SHORT).show();
        // TODO: Navigate to home fragment when ready
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
