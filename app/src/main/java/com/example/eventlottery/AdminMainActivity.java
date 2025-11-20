package com.example.eventlottery;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AdminMainActivity extends AppCompatActivity {

    private TextView navStats, navEvents, navProfiles, navUsers, navLogs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        navStats = findViewById(R.id.navStats);
        navEvents = findViewById(R.id.navEvents);
        navProfiles = findViewById(R.id.navProfiles);
        navUsers = findViewById(R.id.navUsers);
        navLogs = findViewById(R.id.navLogs);

        // Default page
        loadFragment(new AdminDashboardFragment());

        navStats.setOnClickListener(v -> loadFragment(new AdminDashboardFragment()));
        navEvents.setOnClickListener(v -> loadFragment(new EventsFragment()));
        navProfiles.setOnClickListener(v -> loadFragment(new ProfilesFragment()));
        navUsers.setOnClickListener(v -> loadFragment(new UsersFragment()));
        navLogs.setOnClickListener(v -> loadFragment(new LogsFragment()));
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.mainFragmentContainer, fragment)
                .commit();
    }
}
