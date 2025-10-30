package com.example.eventlottery;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eventlottery.fragments.SignUpFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main, new SignUpFragment())
                    .commit();
        }
    }
}