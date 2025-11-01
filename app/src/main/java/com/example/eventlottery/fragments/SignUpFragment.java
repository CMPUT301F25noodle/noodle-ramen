package com.example.eventlottery.fragments;

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
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.eventlottery.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
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

        intializeViews(view);
        setupListeners();

        return view;

    }

    /**
     * Initilaizes all the view components
     * @param view
     */

    private void InitializeViews(View view) {
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

    private void handleSignUp () {
        String name = nameField.getText().toString().trim();
        String
    }

}
