package com.example.eventlottery.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import com.example.eventlottery.R;

/**
 * SignUpFragment - Handles user sign up page
 * Created by: Mataab
 * ..... gotta add more info/ javadoc
 *
 */

public class SignUpFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sign_up, container, false);

        Button organizerButton = view.findViewById(R.id.btn_organizer);
        Button entrantButton = view.findViewById(R.id.btn_entrant);
        Button signUpButton = view.findViewById(R.id.btn_sign_up);

        EditText nameField = view.findViewById(R.id.et_name);
        EditText phoneField = view.findViewById(R.id.et_phone);
        EditText emailField = view.findViewById(R.id.et_email);
        EditText passwordField = view.findViewById(R.id.et_password);

        organizerButton.setOnClickListener(v -> Toast.makeText(getContext(), "Organizer selected", Toast.LENGTH_SHORT).show());

        entrantButton.setOnClickListener(v -> Toast.makeText(getContext(), "Entrant selected", Toast.LENGTH_SHORT).show());

        signUpButton.setOnClickListener(v -> {
            String name = nameField.getText().toString();
            String phone = phoneField.getText().toString();
            String email = emailField.getText().toString();
            String password = passwordField.getText().toString();

            if (name.isEmpty()) {
                nameField.setError("Please enter your name");
                return;
            }

            if (phone.isEmpty()) {
                phoneField.setError("Please enter your phone number");
                return;
            }

            if (email.isEmpty()) {
                emailField.setError("Please enter your email");
                return;
            }

            if (password.isEmpty()) {
                passwordField.setError("Please enter a password");
                return;
            }

            if (password.length() < 6) {
                passwordField.setError("Password must be at least 6 characters");
                return;
            }

            Toast.makeText(getContext(), "Sign up successful!", Toast.LENGTH_SHORT).show();
        });

        return view;
    }
}