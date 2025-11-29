package com.example.eventlottery;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * LandingActivity - acts as the app's home screen.
 * Provides navigation to Login and Sign Up screens.
 * Created by Ibrahim.
 */

public class LandingActivity extends AppCompatActivity {

    private Button signupButton;
    private TextView titleEvent, titleLottery, subtitle, readyText;
    /**
     * Initializes the activity, sets up the UI layout, and configures the navigation button.
     *
     * @param savedInstanceState If non-null, this activity is being re-constructed from a previous saved state.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.landing_page);

        // Initialize layout components
        signupButton = findViewById(R.id.signup_button);
        titleEvent = findViewById(R.id.textEvent);
        titleLottery = findViewById(R.id.textLottery);
        subtitle = findViewById(R.id.textSubtitle);
        readyText = findViewById(R.id.textReady);

        // Set up navigation to SignUpActivity
        signupButton.setOnClickListener(v -> {
            Intent intent = new Intent(LandingActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }
}